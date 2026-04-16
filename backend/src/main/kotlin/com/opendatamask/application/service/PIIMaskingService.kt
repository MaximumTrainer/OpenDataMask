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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

// Transforms source data rows by applying the PIIMaskingRule selected by each
// CustomDataMapping entry. Columns without a mapping pass through unchanged.
@Service
class PIIMaskingService(
    private val ruleRegistry: RuleRegistryPort,
    private val customDataMappingPort: CustomDataMappingPort
) {
    private val logger = LoggerFactory.getLogger(PIIMaskingService::class.java)
    private val mapper = jacksonObjectMapper()

    // Load mappings once and apply to every row in a batch. Prefer this over
    // the convenience overload (applyMappings with workspaceId/connectionId/tableName/row)
    // to avoid N+1 persistence queries when processing multiple rows for the same table.
    fun applyMappingsToRows(
        workspaceId: Long,
        connectionId: Long,
        tableName: String,
        rows: List<Map<String, Any?>>
    ): List<Map<String, Any?>> {
        val mappings = loadMappings(workspaceId, connectionId, tableName)
        if (mappings.isEmpty()) return rows
        return rows.map { row -> applyMappings(mappings, row) }
    }

    // Load the per-column mapping index for a table; result can be reused across rows.
    fun loadMappings(
        workspaceId: Long,
        connectionId: Long,
        tableName: String
    ): Map<String, CustomDataMapping> =
        customDataMappingPort
            .findByWorkspaceIdAndConnectionIdAndTableName(workspaceId, connectionId, tableName)
            .associateBy { it.columnName.lowercase() }

    // Apply pre-fetched mappings to a single row. Column name matching is case-insensitive.
    fun applyMappings(
        mappings: Map<String, CustomDataMapping>,
        row: Map<String, Any?>
    ): Map<String, Any?> {
        if (mappings.isEmpty()) return row
        return row.mapValues { (column, value) ->
            val mapping = mappings[column.lowercase()] ?: return@mapValues value
            when (mapping.action) {
                MappingAction.MIGRATE_AS_IS -> value
                MappingAction.MASK -> applyStrategy(mapping, value)
            }
        }
    }

    // Convenience overload that fetches mappings from persistence on each call.
    // Use applyMappingsToRows() instead when processing multiple rows for the same table.
    fun applyMappings(
        workspaceId: Long,
        connectionId: Long,
        tableName: String,
        row: Map<String, Any?>
    ): Map<String, Any?> =
        applyMappings(loadMappings(workspaceId, connectionId, tableName), row)

    private fun applyStrategy(mapping: CustomDataMapping, value: Any?): Any? {
        val params = parseParams(mapping.piiRuleParams) ?: return value

        // Allow a custom rule to be invoked by specifying its ruleId in params.
        // This enables runtime-registered rules to be used from any mapping strategy slot.
        val customRuleId = params["ruleId"]?.takeIf { it.isNotBlank() }
        if (customRuleId != null) {
            val customRule = ruleRegistry.getRule(customRuleId)
            if (customRule != null) return customRule.mask(value)
            logger.warn("PII rule '{}' not found in registry — passing value through unchanged", customRuleId)
            return value
        }

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
                val pattern = params["pattern"]
                val replacement = params["replacement"]
                if (pattern.isNullOrBlank() || replacement == null) {
                    logger.warn(
                        "REGEX strategy for column '{}' is missing 'pattern' or 'replacement' in piiRuleParams — passing value through unchanged",
                        mapping.columnName
                    )
                    return value
                }
                runCatching { RegexRule(pattern, replacement).mask(value) }
                    .getOrElse { cause ->
                        logger.warn(
                            "REGEX strategy for column '{}' has invalid pattern '{}' — passing value through unchanged: {}",
                            mapping.columnName, pattern, cause.message
                        )
                        value
                    }
            }
            // FAKE strategy is handled upstream by the GeneratorService; return value unchanged.
            MaskingStrategy.FAKE -> value
            null -> value
        }
    }

    // Returns null (and logs a warning) when piiRuleParams is present but cannot be parsed as JSON.
    // Returns an empty map when piiRuleParams is absent/blank (treated as "no extra config").
    private fun parseParams(json: String?): Map<String, String>? {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching { mapper.readValue<Map<String, String>>(json) }
            .getOrElse { cause ->
                logger.warn("Failed to parse piiRuleParams JSON '{}': {} — passing value through unchanged", json, cause.message)
                null
            }
    }
}
