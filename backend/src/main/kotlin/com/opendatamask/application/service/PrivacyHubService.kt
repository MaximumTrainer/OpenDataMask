package com.opendatamask.application.service

import com.opendatamask.domain.port.input.PrivacyHubUseCase

import com.opendatamask.domain.port.input.dto.PrivacyHubSummary
import com.opendatamask.domain.port.input.dto.PrivacyRecommendation
import com.opendatamask.domain.port.input.dto.TableProtectionSummary
import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.port.output.ColumnGeneratorPort
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import com.opendatamask.domain.port.output.GeneratorPresetPort
import com.opendatamask.domain.port.output.TableConfigurationPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrivacyHubService(
    private val columnSensitivityRepository: ColumnSensitivityPort,
    private val columnGeneratorRepository: ColumnGeneratorPort,
    private val tableConfigurationRepository: TableConfigurationPort,
    private val generatorPresetRepository: GeneratorPresetPort
) : PrivacyHubUseCase {
    private val logger = LoggerFactory.getLogger(PrivacyHubService::class.java)

    override fun getSummary(workspaceId: Long): PrivacyHubSummary {
        val sensitivities = columnSensitivityRepository.findByWorkspaceId(workspaceId)
        val generatorMap = buildGeneratorMap(workspaceId)

        var atRisk = 0
        var protected = 0
        var notSensitive = 0
        val tableStats = mutableMapOf<String, Triple<Int, Int, Int>>()

        for (col in sensitivities) {
            val generator = generatorMap[Pair(col.tableName, col.columnName)]
            val classification = classify(col.isSensitive, generator)

            val (ar, p, ns) = tableStats.getOrDefault(col.tableName, Triple(0, 0, 0))
            tableStats[col.tableName] = when (classification) {
                "AT_RISK" -> { atRisk++; Triple(ar + 1, p, ns) }
                "PROTECTED" -> { protected++; Triple(ar, p + 1, ns) }
                else -> { notSensitive++; Triple(ar, p, ns + 1) }
            }
        }

        val tables = tableStats.map { (name, counts) ->
            TableProtectionSummary(name = name, atRisk = counts.first, protected = counts.second, notSensitive = counts.third)
        }
        val recommendations = getRecommendations(workspaceId)

        return PrivacyHubSummary(
            atRiskCount = atRisk,
            protectedCount = protected,
            notSensitiveCount = notSensitive,
            tables = tables,
            recommendationsCount = recommendations.size
        )
    }

    override fun getRecommendations(workspaceId: Long): List<PrivacyRecommendation> {
        val sensitivities = columnSensitivityRepository.findByWorkspaceId(workspaceId)
        val generatorMap = buildGeneratorMap(workspaceId)

        return sensitivities
            .filter { col ->
                col.isSensitive && generatorMap[Pair(col.tableName, col.columnName)] == null
            }
            .map { col ->
                PrivacyRecommendation(
                    tableName = col.tableName,
                    columnName = col.columnName,
                    sensitivityType = col.customSensitivityLabel ?: col.sensitivityType.name,
                    confidenceLevel = col.confidenceLevel.name,
                    recommendedGenerator = col.recommendedGeneratorType?.name ?: "",
                    recommendedPresetId = col.recommendedPresetId
                )
            }
    }

    override fun applyRecommendations(workspaceId: Long): Int {
        val recommendations = getRecommendations(workspaceId)
        val tableConfigs = tableConfigurationRepository.findByWorkspaceId(workspaceId)
        val tableConfigMap = tableConfigs.associateBy { it.tableName }

        // Preload all referenced presets into a map to avoid N+1 queries
        val presetIds = recommendations.mapNotNull { it.recommendedPresetId }.toSet()
        val presetMap = presetIds.associateWith { id ->
            generatorPresetRepository.findById(id).orElse(null)
        }

        // Preload existing column generators per table config to avoid N+1 queries
        val existingGeneratorMap: Map<Long, Map<String, ColumnGenerator>> = tableConfigs.associate { tc ->
            tc.id to columnGeneratorRepository.findByTableConfigurationId(tc.id).associateBy { it.columnName }
        }

        var count = 0
        for (rec in recommendations) {
            val tableConfig = tableConfigMap[rec.tableName] ?: continue

            if (rec.recommendedPresetId != null) {
                // Apply linked preset from a custom sensitivity rule
                val preset = presetMap[rec.recommendedPresetId]
                if (preset == null) {
                    logger.warn(
                        "Cannot apply recommendation for ${rec.tableName}.${rec.columnName}: " +
                            "linked preset id=${rec.recommendedPresetId} not found"
                    )
                    continue
                }
                val existingGenerator = existingGeneratorMap[tableConfig.id]?.get(rec.columnName)
                if (existingGenerator != null) {
                    existingGenerator.presetId = preset.id
                    existingGenerator.generatorType = preset.generatorType
                    existingGenerator.generatorParams = preset.generatorParams
                    columnGeneratorRepository.save(existingGenerator)
                } else {
                    columnGeneratorRepository.save(
                        ColumnGenerator(
                            tableConfigurationId = tableConfig.id,
                            columnName = rec.columnName,
                            generatorType = preset.generatorType,
                            generatorParams = preset.generatorParams,
                            presetId = preset.id
                        )
                    )
                }
                count++
            } else {
                if (rec.recommendedGenerator.isBlank()) continue
                val generatorType = try {
                    GeneratorType.valueOf(rec.recommendedGenerator)
                } catch (e: IllegalArgumentException) {
                    continue
                }
                columnGeneratorRepository.save(
                    ColumnGenerator(
                        tableConfigurationId = tableConfig.id,
                        columnName = rec.columnName,
                        generatorType = generatorType
                    )
                )
                count++
            }
        }
        return count
    }

    private fun buildGeneratorMap(workspaceId: Long): Map<Pair<String, String>, ColumnGenerator> {
        val tableConfigs = tableConfigurationRepository.findByWorkspaceId(workspaceId)
        val result = mutableMapOf<Pair<String, String>, ColumnGenerator>()
        for (tc in tableConfigs) {
            val generators = columnGeneratorRepository.findByTableConfigurationId(tc.id)
            for (gen in generators) {
                result[Pair(tc.tableName, gen.columnName)] = gen
            }
        }
        return result
    }

    private fun classify(isSensitive: Boolean, generator: ColumnGenerator?): String =
        when {
            generator != null -> "PROTECTED"
            isSensitive -> "AT_RISK"
            else -> "NOT_SENSITIVE"
        }
}

