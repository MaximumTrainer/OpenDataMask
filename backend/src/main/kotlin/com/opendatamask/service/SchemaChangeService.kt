package com.opendatamask.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.opendatamask.connector.ColumnInfo
import com.opendatamask.connector.ConnectorFactory
import com.opendatamask.model.*
import com.opendatamask.repository.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime

data class TableSchema(val tableName: String, val columns: List<ColumnInfo>)
data class WorkspaceSchema(val tables: List<TableSchema>)

@Service
class SchemaChangeService(
    private val schemaSnapshotRepository: SchemaSnapshotRepository,
    private val schemaChangeRepository: SchemaChangeRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val dataConnectionRepository: DataConnectionRepository,
    private val connectorFactory: ConnectorFactory,
    private val webhookService: WebhookService
) {
    private val logger = LoggerFactory.getLogger(SchemaChangeService::class.java)
    private val mapper = jacksonObjectMapper()

    fun detectChanges(workspaceId: Long): List<SchemaChange> {
        val workspace = workspaceRepository.findById(workspaceId).orElse(null) ?: return emptyList()
        val sourceConn = dataConnectionRepository.findByWorkspaceIdAndIsSource(workspaceId, true)
            .firstOrNull() ?: return emptyList()

        val connector = try {
            connectorFactory.createConnector(
                sourceConn.type, sourceConn.connectionString,
                sourceConn.username, sourceConn.password, sourceConn.database
            )
        } catch (e: Exception) {
            logger.error("Cannot create connector for workspace $workspaceId: ${e.message}")
            return emptyList()
        }

        val currentTables = try {
            connector.listTables().map { tableName ->
                TableSchema(tableName, connector.listColumns(tableName))
            }
        } catch (e: Exception) {
            logger.error("Cannot fetch schema for workspace $workspaceId: ${e.message}")
            return emptyList()
        }

        val currentSchema = WorkspaceSchema(currentTables)
        val snapshot = schemaSnapshotRepository.findTopByWorkspaceIdOrderBySnapshotAtDesc(workspaceId)

        val newChanges = if (snapshot == null) {
            emptyList() // First snapshot — no changes to detect yet
        } else {
            val previousSchema: WorkspaceSchema = try {
                mapper.readValue(snapshot.schemaJson)
            } catch (e: Exception) {
                logger.warn("Cannot parse previous snapshot for workspace $workspaceId")
                return emptyList()
            }
            diffSchemas(workspaceId, previousSchema, currentSchema)
        }

        val newSnapshot = SchemaSnapshot(
            workspaceId = workspaceId,
            snapshotAt = LocalDateTime.now(),
            schemaJson = mapper.writeValueAsString(currentSchema)
        )
        schemaSnapshotRepository.save(newSnapshot)

        val existing = schemaChangeRepository.findByWorkspaceIdAndStatus(workspaceId, SchemaChangeStatus.UNRESOLVED)
        val existingKeys = existing.map { "${it.changeType}|${it.tableName}|${it.columnName}" }.toSet()
        val toSave = newChanges.filter { c ->
            "${c.changeType}|${c.tableName}|${c.columnName}" !in existingKeys
        }
        val savedChanges = schemaChangeRepository.saveAll(toSave)
        if (savedChanges.isNotEmpty()) {
            webhookService.triggerForSchemaChange(workspaceId, savedChanges)
        }
        return savedChanges
    }

    private fun diffSchemas(
        workspaceId: Long,
        previous: WorkspaceSchema,
        current: WorkspaceSchema
    ): List<SchemaChange> {
        val changes = mutableListOf<SchemaChange>()
        val prevTables = previous.tables.associateBy { it.tableName }
        val currTables = current.tables.associateBy { it.tableName }

        (currTables.keys - prevTables.keys).forEach { tableName ->
            changes.add(SchemaChange(workspaceId = workspaceId, changeType = SchemaChangeType.NEW_TABLE, tableName = tableName))
        }
        (prevTables.keys - currTables.keys).forEach { tableName ->
            changes.add(SchemaChange(workspaceId = workspaceId, changeType = SchemaChangeType.DROPPED_TABLE, tableName = tableName))
        }
        (prevTables.keys intersect currTables.keys).forEach { tableName ->
            val prevCols = prevTables[tableName]!!.columns.associateBy { it.name }
            val currCols = currTables[tableName]!!.columns.associateBy { it.name }

            (currCols.keys - prevCols.keys).forEach { colName ->
                changes.add(SchemaChange(workspaceId = workspaceId, changeType = SchemaChangeType.NEW_COLUMN, tableName = tableName, columnName = colName))
            }
            (prevCols.keys - currCols.keys).forEach { colName ->
                changes.add(SchemaChange(workspaceId = workspaceId, changeType = SchemaChangeType.DROPPED_COLUMN, tableName = tableName, columnName = colName))
            }
            (prevCols.keys intersect currCols.keys).forEach { colName ->
                val prev = prevCols[colName]!!
                val curr = currCols[colName]!!
                if (prev.type != curr.type) {
                    changes.add(SchemaChange(workspaceId = workspaceId, changeType = SchemaChangeType.TYPE_CHANGED, tableName = tableName, columnName = colName, oldValue = prev.type, newValue = curr.type))
                }
                if (prev.nullable != curr.nullable) {
                    changes.add(SchemaChange(workspaceId = workspaceId, changeType = SchemaChangeType.NULLABILITY_CHANGED, tableName = tableName, columnName = colName, oldValue = prev.nullable.toString(), newValue = curr.nullable.toString()))
                }
            }
        }
        return changes
    }

    fun getUnresolvedChanges(workspaceId: Long): List<SchemaChange> =
        schemaChangeRepository.findByWorkspaceIdAndStatus(workspaceId, SchemaChangeStatus.UNRESOLVED)

    fun resolveChange(changeId: Long) {
        schemaChangeRepository.findById(changeId).ifPresent { change ->
            change.status = SchemaChangeStatus.RESOLVED
            schemaChangeRepository.save(change)
        }
    }

    fun dismissChange(changeId: Long) {
        schemaChangeRepository.findById(changeId).ifPresent { change ->
            change.status = SchemaChangeStatus.DISMISSED
            schemaChangeRepository.save(change)
        }
    }

    fun resolveAll(workspaceId: Long) {
        val exposingTypes = setOf(SchemaChangeType.NEW_COLUMN, SchemaChangeType.TYPE_CHANGED, SchemaChangeType.NULLABILITY_CHANGED)
        val unresolved = schemaChangeRepository.findByWorkspaceIdAndStatus(workspaceId, SchemaChangeStatus.UNRESOLVED)
            .filter { it.changeType in exposingTypes }
        unresolved.forEach { it.status = SchemaChangeStatus.RESOLVED }
        schemaChangeRepository.saveAll(unresolved)
    }

    fun dismissAll(workspaceId: Long) {
        val notificationTypes = setOf(SchemaChangeType.DROPPED_COLUMN, SchemaChangeType.DROPPED_TABLE, SchemaChangeType.NEW_TABLE)
        val unresolved = schemaChangeRepository.findByWorkspaceIdAndStatus(workspaceId, SchemaChangeStatus.UNRESOLVED)
            .filter { it.changeType in notificationTypes }
        unresolved.forEach { it.status = SchemaChangeStatus.DISMISSED }
        schemaChangeRepository.saveAll(unresolved)
    }

    fun isBlockingJobRun(workspaceId: Long): Boolean {
        val workspace = workspaceRepository.findById(workspaceId).orElse(null) ?: return false
        val unresolvedChanges = getUnresolvedChanges(workspaceId)
        return when (workspace.schemaChangeHandling) {
            SchemaChangeHandling.NEVER_BLOCK -> false
            SchemaChangeHandling.BLOCK_ALL -> unresolvedChanges.isNotEmpty()
            SchemaChangeHandling.BLOCK_EXPOSING -> unresolvedChanges.any { change ->
                change.changeType in setOf(
                    SchemaChangeType.NEW_COLUMN,
                    SchemaChangeType.TYPE_CHANGED,
                    SchemaChangeType.NULLABILITY_CHANGED
                )
            }
        }
    }

    @Scheduled(fixedRateString = "\${tonic.schema.scan.interval.ms:7200000}")
    fun scheduledScan() {
        workspaceRepository.findAll().forEach { workspace ->
            try {
                detectChanges(workspace.id)
            } catch (e: Exception) {
                logger.error("Scheduled schema scan failed for workspace ${workspace.id}: ${e.message}")
            }
        }
    }
}


