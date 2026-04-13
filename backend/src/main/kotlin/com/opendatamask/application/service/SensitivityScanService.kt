package com.opendatamask.application.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.opendatamask.domain.port.input.SensitivityScanUseCase

import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.domain.port.output.ConnectorFactoryPort
import com.opendatamask.domain.model.*
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import com.opendatamask.domain.port.output.CustomSensitivityRulePort
import com.opendatamask.domain.port.output.SensitivityScanLogPort
import com.opendatamask.domain.port.output.SensitivityScanLogEntryPort
import com.opendatamask.domain.port.output.WorkspacePort
import com.opendatamask.domain.port.output.DataConnectionPort
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime

@Service
class SensitivityScanService(
    private val columnSensitivityRepository: ColumnSensitivityPort,
    private val sensitivityScanLogRepository: SensitivityScanLogPort,
    private val sensitivityScanLogEntryRepository: SensitivityScanLogEntryPort,
    private val workspaceRepository: WorkspacePort,
    private val dataConnectionRepository: DataConnectionPort,
    private val connectorFactory: ConnectorFactoryPort,
    private val encryptionPort: EncryptionPort,
    private val customSensitivityRuleRepository: CustomSensitivityRulePort,
    private val customSensitivityRuleService: CustomSensitivityRuleService
) : SensitivityScanUseCase {
    private val logger = LoggerFactory.getLogger(SensitivityScanService::class.java)
    private val builtInRules: List<SensitivityRule> = buildRules()
    private val mapper = jacksonObjectMapper()

    override fun scanWorkspace(workspaceId: Long): SensitivityScanLog {
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
                connectionString = encryptionPort.decrypt(sourceConnection.connectionString),
                username = sourceConnection.username,
                password = sourceConnection.password?.let { encryptionPort.decrypt(it) },
                database = sourceConnection.database
            )

            val activeCustomRules = customSensitivityRuleRepository.findByIsActiveTrue()
                .map { rule ->
                    val matchers: List<CustomRuleMatcher> = try {
                        mapper.readValue(rule.matchersJson)
                    } catch (e: Exception) {
                        logger.warn("Failed to parse matchers JSON for custom rule '${rule.name}' (id=${rule.id}): ${e.message}")
                        emptyList()
                    }
                    rule to matchers
                }

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
                    val builtInResult = detectSensitivity(column, samples)
                    // Evaluate custom rules for every column so a linked preset can be attached
                    // even when a built-in rule already detected sensitivity.
                    val customMatch = detectCustomRuleSensitivity(column, columnInfo.type, activeCustomRules)

                    if (builtInResult != null) {
                        log.sensitiveColumnsFound++
                        val existing = columnSensitivityRepository
                            .findByWorkspaceIdAndTableNameAndColumnName(workspaceId, table, column)
                        val entity = existing ?: ColumnSensitivity(
                            workspaceId = workspaceId,
                            tableName = table,
                            columnName = column
                        )
                        entity.isSensitive = true
                        entity.sensitivityType = builtInResult.sensitivityType
                        entity.confidenceLevel = builtInResult.confidence
                        entity.recommendedGeneratorType = builtInResult.recommendedGenerator
                        // Custom rule match may supply a linked preset while keeping the built-in type label
                        if (customMatch != null) {
                            val (matchedRule, _) = customMatch
                            entity.customSensitivityLabel = matchedRule.name
                            entity.recommendedPresetId = matchedRule.linkedPresetId
                        } else {
                            entity.customSensitivityLabel = null
                            entity.recommendedPresetId = null
                        }
                        columnSensitivityRepository.save(entity)
                        sensitivityScanLogEntryRepository.save(
                            SensitivityScanLogEntry(
                                scanLogId = log.id!!,
                                tableName = table,
                                columnName = column,
                                detectedType = builtInResult.sensitivityType.name,
                                confidenceLevel = builtInResult.confidence.name,
                                recommendedGenerator = builtInResult.recommendedGenerator.name,
                                scannedAt = LocalDateTime.now()
                            )
                        )
                    } else if (customMatch != null) {
                        val (matchedRule, _) = customMatch
                        log.sensitiveColumnsFound++
                        val existing = columnSensitivityRepository
                            .findByWorkspaceIdAndTableNameAndColumnName(workspaceId, table, column)
                        val entity = existing ?: ColumnSensitivity(
                            workspaceId = workspaceId,
                            tableName = table,
                            columnName = column
                        )
                        entity.isSensitive = true
                        entity.sensitivityType = SensitivityType.UNKNOWN
                        entity.confidenceLevel = ConfidenceLevel.HIGH
                        entity.recommendedGeneratorType = null
                        entity.customSensitivityLabel = matchedRule.name
                        entity.recommendedPresetId = matchedRule.linkedPresetId
                        columnSensitivityRepository.save(entity)
                        sensitivityScanLogEntryRepository.save(
                            SensitivityScanLogEntry(
                                scanLogId = log.id!!,
                                tableName = table,
                                columnName = column,
                                detectedType = matchedRule.name,
                                confidenceLevel = ConfidenceLevel.HIGH.name,
                                recommendedGenerator = null,
                                scannedAt = LocalDateTime.now()
                            )
                        )
                    } else {
                        sensitivityScanLogEntryRepository.save(
                            SensitivityScanLogEntry(
                                scanLogId = log.id!!,
                                tableName = table,
                                columnName = column,
                                detectedType = null,
                                confidenceLevel = null,
                                recommendedGenerator = null,
                                scannedAt = LocalDateTime.now()
                            )
                        )
                    }
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
        for (rule in builtInRules) {
            val colMatch = rule.columnNamePatterns.any { it.containsMatchIn(lowerCol) }
            if (!colMatch) continue
            val valMatch = rule.valuePatterns.isNotEmpty() &&
                sampleValues.any { sample -> rule.valuePatterns.any { it.containsMatchIn(sample) } }
            val effectiveConfidence = if (valMatch) ConfidenceLevel.FULL else rule.confidence
            return rule.copy(confidence = effectiveConfidence)
        }

        // Second pass: value-only matches (lower confidence, no column name signal)
        for (rule in builtInRules) {
            if (rule.valuePatterns.isEmpty()) continue
            val valMatch = sampleValues.any { sample ->
                rule.valuePatterns.any { it.containsMatchIn(sample) }
            }
            if (valMatch) return rule.copy(confidence = ConfidenceLevel.MEDIUM)
        }

        return null
    }

    /** Returns the matching custom rule and its matchers, or null if no match. */
    private fun detectCustomRuleSensitivity(
        columnName: String,
        columnType: String,
        activeCustomRules: List<Pair<CustomSensitivityRule, List<CustomRuleMatcher>>>
    ): Pair<CustomSensitivityRule, List<CustomRuleMatcher>>? {
        for ((rule, matchers) in activeCustomRules) {
            if (!customSensitivityRuleService.matchesDataType(columnType, rule.dataTypeFilter)) continue
            if (customSensitivityRuleService.matchesColumnName(columnName, matchers)) {
                return rule to matchers
            }
        }
        return null
    }

    override fun getLatestLog(workspaceId: Long): SensitivityScanLog? =
        sensitivityScanLogRepository.findTopByWorkspaceIdOrderByStartedAtDesc(workspaceId)

    override fun getScanLogs(workspaceId: Long): List<SensitivityScanLog> =
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

