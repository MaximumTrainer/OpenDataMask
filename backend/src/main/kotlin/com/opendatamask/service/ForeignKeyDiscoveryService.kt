package com.opendatamask.service

import com.opendatamask.connector.ConnectorFactory
import com.opendatamask.domain.model.ForeignKeyRelationship
import com.opendatamask.repository.DataConnectionRepository
import com.opendatamask.repository.ForeignKeyRelationshipRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ForeignKeyDiscoveryService(
    private val foreignKeyRelationshipRepository: ForeignKeyRelationshipRepository,
    private val dataConnectionRepository: DataConnectionRepository,
    private val connectorFactory: ConnectorFactory
) {
    private val logger = LoggerFactory.getLogger(ForeignKeyDiscoveryService::class.java)

    fun discoverForeignKeys(workspaceId: Long): List<ForeignKeyRelationship> {
        val sourceConn = dataConnectionRepository.findByWorkspaceIdAndIsSource(workspaceId, true)
            .firstOrNull() ?: return foreignKeyRelationshipRepository.findByWorkspaceId(workspaceId)

        val connector = try {
            connectorFactory.createConnector(
                type = sourceConn.type,
                connectionString = sourceConn.connectionString,
                username = sourceConn.username,
                password = sourceConn.password,
                database = sourceConn.database
            )
        } catch (e: Exception) {
            logger.error("Cannot create connector for workspace $workspaceId: ${e.message}")
            return foreignKeyRelationshipRepository.findByWorkspaceId(workspaceId)
        }

        val tables = try {
            connector.listTables()
        } catch (e: Exception) {
            logger.error("Cannot list tables for workspace $workspaceId: ${e.message}")
            return foreignKeyRelationshipRepository.findByWorkspaceId(workspaceId)
        }

        val discovered = mutableListOf<ForeignKeyRelationship>()
        for (table in tables) {
            try {
                val fkInfoList = connector.listForeignKeys(table)
                for (fkInfo in fkInfoList) {
                    val existing = foreignKeyRelationshipRepository.findByWorkspaceId(workspaceId).any { fk ->
                        fk.fromTable == fkInfo.fromTable &&
                            fk.fromColumn == fkInfo.fromColumn &&
                            fk.toTable == fkInfo.toTable &&
                            fk.toColumn == fkInfo.toColumn &&
                            !fk.isVirtual
                    }
                    if (!existing) {
                        discovered.add(
                            ForeignKeyRelationship(
                                workspaceId = workspaceId,
                                fromTable = fkInfo.fromTable,
                                fromColumn = fkInfo.fromColumn,
                                toTable = fkInfo.toTable,
                                toColumn = fkInfo.toColumn,
                                isVirtual = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                logger.warn("Cannot discover FKs for table $table in workspace $workspaceId: ${e.message}")
            }
        }

        if (discovered.isNotEmpty()) {
            foreignKeyRelationshipRepository.saveAll(discovered)
        }

        return foreignKeyRelationshipRepository.findByWorkspaceId(workspaceId)
    }

    fun getVirtualForeignKeys(workspaceId: Long): List<ForeignKeyRelationship> =
        foreignKeyRelationshipRepository.findByWorkspaceId(workspaceId)

    fun createVirtualForeignKey(
        workspaceId: Long,
        fromTable: String,
        fromColumn: String,
        toTable: String,
        toColumn: String
    ): ForeignKeyRelationship {
        val fk = ForeignKeyRelationship(
            workspaceId = workspaceId,
            fromTable = fromTable,
            fromColumn = fromColumn,
            toTable = toTable,
            toColumn = toColumn,
            isVirtual = true
        )
        return foreignKeyRelationshipRepository.save(fk)
    }

    fun deleteForeignKey(workspaceId: Long, fkId: Long) {
        val fk = foreignKeyRelationshipRepository.findById(fkId)
            .orElseThrow { NoSuchElementException("ForeignKeyRelationship not found: $fkId") }
        if (fk.workspaceId != workspaceId) {
            throw NoSuchElementException("ForeignKeyRelationship $fkId does not belong to workspace $workspaceId")
        }
        foreignKeyRelationshipRepository.deleteById(fkId)
    }
}
