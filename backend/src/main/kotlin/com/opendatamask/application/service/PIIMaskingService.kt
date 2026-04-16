package com.opendatamask.application.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.opendatamask.domain.model.CustomDataMapping
import com.opendatamask.domain.model.HashRule
import com.opendatamask.domain.model.MappingAction
import com.opendatamask.domain.model.MaskingStrategy
import com.opendatamask.domain.model.PartialMaskRule
import com.opendatamask.domain.model.RegexRule
import com.opendatamask.domain.port.output.CustomDataMappingPort
import com.opendatamask.domain.port.output.RuleRegistryPort
import org.springframework.stereotype.Service

// Transforms a source data row by applying the PIIMaskingRule selected by each
// CustomDataMapping entry. Columns without a mapping pass through unchanged.
@Service
class PIIMaskingService(
    private val ruleRegistry: RuleRegistryPort,
    private val customDataMappingPort: CustomDataMappingPort
) {
    private val mapper = jacksonObjectMapper()

    // Apply all active CustomDataMappings for the given workspace/connection/table to a single row.
    fun applyMappings(
        workspaceId: Long,
        connectionId: Long,
        tableName: String,
        row: Map<String, Any?>
    ): Map<String, Any?> {
        val mappings = customDataMappingPort
            .findByWorkspaceIdAndConnectionIdAndTableName(workspaceId, connectionId, tableName)
            .associateBy { it.columnName.lowercase() }

        if (mappings.isEmpty()) return row

        return row.mapValues { (column, value) ->
            val mapping = mappings[column.lowercase()] ?: return@mapValues value
            when (mapping.action) {
                MappingAction.MIGRATE_AS_IS -> value
                MappingAction.MASK -> applyStrategy(mapping, value)
            }
        }
    }

    private fun applyStrategy(mapping: CustomDataMapping, value: Any?): Any? {
        val params = parseParams(mapping.piiRuleParams)
        return when (mapping.maskingStrategy) {
            MaskingStrategy.NULL -> null
            MaskingStrategy.REDACT -> ruleRegistry.getRule("redact")?.mask(value) ?: "[REDACTED]"
            MaskingStrategy.HASH -> {
                val salt = params["salt"] ?: ""
                HashRule(salt).mask(value)
            }
            MaskingStrategy.PARTIAL_MASK -> {
                val keepFirst = params["keepFirst"]?.toIntOrNull() ?: 0
                val keepLast = params["keepLast"]?.toIntOrNull() ?: 4
                val maskChar = params["maskChar"]?.firstOrNull() ?: '*'
                PartialMaskRule(keepFirst, keepLast, maskChar).mask(value)
            }
            MaskingStrategy.REGEX -> {
                val pattern = params["pattern"] ?: ".*"
                val replacement = params["replacement"] ?: ""
                RegexRule(pattern, replacement).mask(value)
            }
            // FAKE strategy is handled upstream by the GeneratorService; return value unchanged.
            MaskingStrategy.FAKE -> value
            null -> value
        }
    }

    private fun parseParams(json: String?): Map<String, String> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching { mapper.readValue<Map<String, String>>(json) }.getOrDefault(emptyMap())
    }
}
