package com.opendatamask.service

import com.opendatamask.config.EncryptionService
import com.opendatamask.connector.ConnectorFactory
import com.opendatamask.domain.model.*
import com.opendatamask.repository.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime

@Service
class SensitivityScanService(
    private val columnSensitivityRepository: ColumnSensitivityRepository,
    private val sensitivityScanLogRepository: SensitivityScanLogRepository,
    private val sensitivityScanLogEntryRepository: SensitivityScanLogEntryRepository,
    private val workspaceRepository: WorkspaceRepository,
    private val dataConnectionRepository: DataConnectionRepository,
    private val connectorFactory: ConnectorFactory,
    private val encryptionService: EncryptionService
) {
    private val rules: List<SensitivityRule> = buildRules()

    fun scanWorkspace(workspaceId: Long): SensitivityScanLog {
        val log = sensitivityScanLogRepository.save(SensitivityScanLog(workspaceId = workspaceId))
        try {
            workspaceRepository.findById(workspaceId).orElseThrow {
                NoSuchElementException("Workspace not found: $workspaceId")
            }
            val sourceConnection = dataConnectionRepository
                .findByWorkspaceIdAndIsSource(workspaceId, true)
                .firstOrNull()
                ?: run {
                    log.status = "FAILED"
                    log.errorMessage = "No source connection"
                    return sensitivityScanLogRepository.save(log)
                }

            val connector = connectorFactory.createConnector(
                type = sourceConnection.type,
                connectionString = encryptionService.decrypt(sourceConnection.connectionString),
                username = sourceConnection.username,
                password = sourceConnection.password?.let { encryptionService.decrypt(it) },
                database = sourceConnection.database
            )

            val tables = connector.listTables()
            log.tablesScanned = tables.size

            for (table in tables) {
                val columns = connector.listColumns(table)
                for (columnInfo in columns) {
                    val column = columnInfo.name
                    log.columnsScanned++
                    val samples = try {
                        connector.fetchData(table, 100).mapNotNull { it[column]?.toString() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val result = detectSensitivity(column, samples)
                    if (result != null) {
                        log.sensitiveColumnsFound++
                        val existing = columnSensitivityRepository
                            .findByWorkspaceIdAndTableNameAndColumnName(workspaceId, table, column)
                        val entity = existing ?: ColumnSensitivity(
                            workspaceId = workspaceId,
                            tableName = table,
                            columnName = column
                        )
                        entity.isSensitive = true
                        entity.sensitivityType = result.sensitivityType
                        entity.confidenceLevel = result.confidence
                        entity.recommendedGeneratorType = result.recommendedGenerator
                        columnSensitivityRepository.save(entity)
                    }
                    sensitivityScanLogEntryRepository.save(
                        SensitivityScanLogEntry(
                            scanLogId = log.id!!,
                            tableName = table,
                            columnName = column,
                            detectedType = result?.sensitivityType?.name,
                            confidenceLevel = result?.confidence?.name,
                            recommendedGenerator = result?.recommendedGenerator?.name,
                            scannedAt = LocalDateTime.now()
                        )
                    )
                }
            }
            log.status = "COMPLETED"
            log.completedAt = Instant.now()
        } catch (e: Exception) {
            log.status = "FAILED"
            log.errorMessage = e.message
        }
        return sensitivityScanLogRepository.save(log)
    }

    fun detectSensitivity(columnName: String, sampleValues: List<String>): SensitivityRule? {
        val lowerCol = columnName.lowercase()

        // First pass: column name matches (takes priority over value-only matches)
        for (rule in rules) {
            val colMatch = rule.columnNamePatterns.any { it.containsMatchIn(lowerCol) }
            if (!colMatch) continue
            val valMatch = rule.valuePatterns.isNotEmpty() &&
                sampleValues.any { sample -> rule.valuePatterns.any { it.containsMatchIn(sample) } }
            val effectiveConfidence = if (valMatch) ConfidenceLevel.FULL else rule.confidence
            return rule.copy(confidence = effectiveConfidence)
        }

        // Second pass: value-only matches (lower confidence, no column name signal)
        for (rule in rules) {
            if (rule.valuePatterns.isEmpty()) continue
            val valMatch = sampleValues.any { sample ->
                rule.valuePatterns.any { it.containsMatchIn(sample) }
            }
            if (valMatch) return rule.copy(confidence = ConfidenceLevel.MEDIUM)
        }

        return null
    }

    fun getLatestLog(workspaceId: Long): SensitivityScanLog? =
        sensitivityScanLogRepository.findTopByWorkspaceIdOrderByStartedAtDesc(workspaceId)

    fun getScanLogs(workspaceId: Long): List<SensitivityScanLog> =
        sensitivityScanLogRepository.findByWorkspaceIdOrderByStartedAtDesc(workspaceId)

    @Scheduled(cron = "\${tonic.sensitivity.scan.cron:0 0 0 * * *}")
    fun scheduledScan() {
        workspaceRepository.findAll()
            .sortedByDescending { it.updatedAt }
            .take(10)
            .forEach { workspace ->
                try {
                    scanWorkspace(workspace.id)
                } catch (e: Exception) {
                    // log and continue to next workspace
                }
            }
    }
}
