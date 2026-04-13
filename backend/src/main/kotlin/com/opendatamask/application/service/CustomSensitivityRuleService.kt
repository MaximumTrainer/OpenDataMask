package com.opendatamask.application.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.opendatamask.domain.model.CustomRuleMatcher
import com.opendatamask.domain.model.CustomSensitivityRule
import com.opendatamask.domain.model.GenericDataType
import com.opendatamask.domain.model.MatcherType
import com.opendatamask.domain.port.input.CustomSensitivityRuleUseCase
import com.opendatamask.domain.port.input.dto.CustomRuleMatcherDto
import com.opendatamask.domain.port.input.dto.CustomRulePreviewRequest
import com.opendatamask.domain.port.input.dto.CustomRulePreviewResult
import com.opendatamask.domain.port.input.dto.CustomSensitivityRuleRequest
import com.opendatamask.domain.port.input.dto.CustomSensitivityRuleResponse
import com.opendatamask.domain.port.output.CustomSensitivityRulePort
import com.opendatamask.domain.port.output.SchemaSnapshotPort
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class CustomSensitivityRuleService(
    private val customRuleRepository: CustomSensitivityRulePort,
    private val schemaSnapshotRepository: SchemaSnapshotPort
) : CustomSensitivityRuleUseCase {

    private val mapper = jacksonObjectMapper()

    override fun listRules(): List<CustomSensitivityRuleResponse> =
        customRuleRepository.findAll().map { it.toResponse() }

    override fun getRule(id: Long): CustomSensitivityRuleResponse =
        customRuleRepository.findById(id)
            .orElseThrow { NoSuchElementException("Custom sensitivity rule not found: $id") }
            .toResponse()

    override fun createRule(request: CustomSensitivityRuleRequest): CustomSensitivityRuleResponse {
        if (customRuleRepository.existsByName(request.name)) {
            throw IllegalArgumentException("A rule with name '${request.name}' already exists")
        }
        val rule = CustomSensitivityRule(
            name = request.name,
            description = request.description,
            dataTypeFilter = request.dataTypeFilter,
            matchersJson = mapper.writeValueAsString(request.matchers.map { it.toModel() }),
            linkedPresetId = request.linkedPresetId,
            isActive = request.isActive
        )
        return customRuleRepository.save(rule).toResponse()
    }

    override fun updateRule(id: Long, request: CustomSensitivityRuleRequest): CustomSensitivityRuleResponse {
        val existing = customRuleRepository.findById(id)
            .orElseThrow { NoSuchElementException("Custom sensitivity rule not found: $id") }
        if (existing.name != request.name && customRuleRepository.existsByName(request.name)) {
            throw IllegalArgumentException("A rule with name '${request.name}' already exists")
        }
        existing.name = request.name
        existing.description = request.description
        existing.dataTypeFilter = request.dataTypeFilter
        existing.matchersJson = mapper.writeValueAsString(request.matchers.map { it.toModel() })
        existing.linkedPresetId = request.linkedPresetId
        existing.isActive = request.isActive
        existing.updatedAt = Instant.now()
        return customRuleRepository.save(existing).toResponse()
    }

    override fun deleteRule(id: Long) {
        if (!customRuleRepository.findById(id).isPresent) {
            throw NoSuchElementException("Custom sensitivity rule not found: $id")
        }
        customRuleRepository.deleteById(id)
    }

    override fun previewRule(request: CustomRulePreviewRequest): List<CustomRulePreviewResult> {
        val snapshot = schemaSnapshotRepository
            .findTopByWorkspaceIdOrderBySnapshotAtDesc(request.workspaceId)
            ?: return emptyList()

        val workspaceSchema = try {
            mapper.readValue<SchemaSnapshotSchema>(snapshot.schemaJson)
        } catch (e: Exception) {
            return emptyList()
        }

        val matchers = request.matchers.map { it.toModel() }
        return workspaceSchema.tables.flatMap { table ->
            table.columns
                .filter { col ->
                    matchesDataType(col.type, request.dataTypeFilter) &&
                        matchesColumnName(col.name, matchers)
                }
                .map { col ->
                    CustomRulePreviewResult(
                        tableName = table.tableName,
                        columnName = col.name,
                        columnType = col.type
                    )
                }
        }
    }

    // ── Internal matching helpers ──────────────────────────────────────────

    fun matchesColumnName(columnName: String, matchers: List<CustomRuleMatcher>): Boolean {
        if (matchers.isEmpty()) return false
        return matchers.any { matcher ->
            val col = if (matcher.caseSensitive) columnName else columnName.lowercase()
            val value = if (matcher.caseSensitive) matcher.value else matcher.value.lowercase()
            when (matcher.matcherType) {
                MatcherType.CONTAINS -> col.contains(value)
                MatcherType.STARTS_WITH -> col.startsWith(value)
                MatcherType.ENDS_WITH -> col.endsWith(value)
                MatcherType.REGEX -> Regex(
                    matcher.value,
                    if (matcher.caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                ).containsMatchIn(columnName)
            }
        }
    }

    fun matchesDataType(columnType: String, filter: GenericDataType): Boolean {
        if (filter == GenericDataType.ANY) return true
        return toGenericDataType(columnType) == filter
    }

    fun toGenericDataType(dbType: String): GenericDataType {
        val t = dbType.lowercase().replace(Regex("\\s*\\(.*\\)"), "").trim()
        return when {
            t.startsWith("varchar") || t.startsWith("nvarchar") || t.startsWith("char") ||
                t == "text" || t == "tinytext" || t == "mediumtext" || t == "longtext" ||
                t == "clob" || t == "bpchar" || t == "string" || t == "str" ||
                t == "uuid" || t == "enum" -> GenericDataType.TEXT

            t == "int" || t == "integer" || t == "bigint" || t == "smallint" || t == "tinyint" ||
                t.startsWith("decimal") || t.startsWith("numeric") || t.startsWith("float") ||
                t.startsWith("double") || t == "real" || t == "money" || t == "smallmoney" ||
                t == "int4" || t == "int8" || t == "int2" || t == "number" || t == "serial" ||
                t == "bigserial" || t == "smallserial" -> GenericDataType.NUMERIC

            t == "date" || t == "timestamp" || t == "timestamptz" || t == "datetime" ||
                t == "time" || t == "timetz" || t == "datetime2" || t == "datetimeoffset" -> GenericDataType.DATE

            t == "boolean" || t == "bool" || t == "bit" -> GenericDataType.BOOLEAN

            else -> GenericDataType.ANY
        }
    }

    // ── Mapping helpers ────────────────────────────────────────────────────

    private fun CustomRuleMatcherDto.toModel() = CustomRuleMatcher(
        matcherType = matcherType,
        value = value,
        caseSensitive = caseSensitive
    )

    private fun CustomSensitivityRule.toResponse(): CustomSensitivityRuleResponse {
        val matchers: List<CustomRuleMatcher> = try {
            mapper.readValue(matchersJson)
        } catch (e: Exception) {
            emptyList()
        }
        return CustomSensitivityRuleResponse(
            id = id ?: 0L,
            name = name,
            description = description,
            dataTypeFilter = dataTypeFilter,
            matchers = matchers.map { m ->
                CustomRuleMatcherDto(
                    matcherType = m.matcherType,
                    value = m.value,
                    caseSensitive = m.caseSensitive
                )
            },
            linkedPresetId = linkedPresetId,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // Internal data classes for schema JSON parsing
    private data class SchemaSnapshotSchema(val tables: List<SchemaSnapshotTable>)
    private data class SchemaSnapshotTable(val tableName: String, val columns: List<SchemaSnapshotColumn>)
    private data class SchemaSnapshotColumn(val name: String, val type: String, val nullable: Boolean = true)
}
