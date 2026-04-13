package com.opendatamask.application.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opendatamask.domain.model.CustomRuleMatcher
import com.opendatamask.domain.model.CustomSensitivityRule
import com.opendatamask.domain.model.GenericDataType
import com.opendatamask.domain.model.MatcherType
import com.opendatamask.domain.model.SchemaSnapshot
import com.opendatamask.domain.port.input.dto.CustomRuleMatcherDto
import com.opendatamask.domain.port.input.dto.CustomRulePreviewRequest
import com.opendatamask.domain.port.input.dto.CustomSensitivityRuleRequest
import com.opendatamask.domain.port.output.CustomSensitivityRulePort
import com.opendatamask.domain.port.output.SchemaSnapshotPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CustomSensitivityRuleServiceTest {

    @Mock private lateinit var customRuleRepository: CustomSensitivityRulePort
    @Mock private lateinit var schemaSnapshotRepository: SchemaSnapshotPort

    @InjectMocks
    private lateinit var service: CustomSensitivityRuleService

    private val mapper = jacksonObjectMapper()

    // ── matchesColumnName tests ────────────────────────────────────────────

    @Test
    fun `matchesColumnName CONTAINS matches when value is in column name`() {
        val matchers = listOf(CustomRuleMatcher(MatcherType.CONTAINS, "uid", false))
        assertTrue(service.matchesColumnName("user_uid_col", matchers))
    }

    @Test
    fun `matchesColumnName CONTAINS does not match when value absent`() {
        val matchers = listOf(CustomRuleMatcher(MatcherType.CONTAINS, "uid", false))
        assertFalse(service.matchesColumnName("email_address", matchers))
    }

    @Test
    fun `matchesColumnName STARTS_WITH matches prefix`() {
        val matchers = listOf(CustomRuleMatcher(MatcherType.STARTS_WITH, "user", false))
        assertTrue(service.matchesColumnName("user_id", matchers))
    }

    @Test
    fun `matchesColumnName ENDS_WITH matches suffix`() {
        val matchers = listOf(CustomRuleMatcher(MatcherType.ENDS_WITH, "id", false))
        assertTrue(service.matchesColumnName("user_id", matchers))
        assertTrue(service.matchesColumnName("tx_id", matchers))
        assertFalse(service.matchesColumnName("email_address", matchers))
    }

    @Test
    fun `matchesColumnName REGEX matches pattern`() {
        val matchers = listOf(CustomRuleMatcher(MatcherType.REGEX, ".*_id$", false))
        assertTrue(service.matchesColumnName("user_id", matchers))
        assertFalse(service.matchesColumnName("user_email", matchers))
    }

    @Test
    fun `matchesColumnName is case-insensitive by default`() {
        val matchers = listOf(CustomRuleMatcher(MatcherType.CONTAINS, "UID", false))
        assertTrue(service.matchesColumnName("user_uid", matchers))
    }

    @Test
    fun `matchesColumnName respects case-sensitive flag`() {
        val matchers = listOf(CustomRuleMatcher(MatcherType.CONTAINS, "UID", caseSensitive = true))
        assertFalse(service.matchesColumnName("user_uid", matchers))
        assertTrue(service.matchesColumnName("user_UID", matchers))
    }

    @Test
    fun `matchesColumnName returns false when matchers list is empty`() {
        assertFalse(service.matchesColumnName("user_id", emptyList()))
    }

    // ── toGenericDataType tests ────────────────────────────────────────────

    @Test
    fun `toGenericDataType maps TEXT types correctly`() {
        assertEquals(GenericDataType.TEXT, service.toGenericDataType("varchar"))
        assertEquals(GenericDataType.TEXT, service.toGenericDataType("VARCHAR(255)"))
        assertEquals(GenericDataType.TEXT, service.toGenericDataType("text"))
        assertEquals(GenericDataType.TEXT, service.toGenericDataType("string"))
    }

    @Test
    fun `toGenericDataType maps NUMERIC types correctly`() {
        assertEquals(GenericDataType.NUMERIC, service.toGenericDataType("integer"))
        assertEquals(GenericDataType.NUMERIC, service.toGenericDataType("bigint"))
        assertEquals(GenericDataType.NUMERIC, service.toGenericDataType("decimal(10,2)"))
        assertEquals(GenericDataType.NUMERIC, service.toGenericDataType("int"))
    }

    @Test
    fun `toGenericDataType maps DATE types correctly`() {
        assertEquals(GenericDataType.DATE, service.toGenericDataType("date"))
        assertEquals(GenericDataType.DATE, service.toGenericDataType("timestamp"))
        assertEquals(GenericDataType.DATE, service.toGenericDataType("datetime"))
    }

    @Test
    fun `toGenericDataType maps BOOLEAN types correctly`() {
        assertEquals(GenericDataType.BOOLEAN, service.toGenericDataType("boolean"))
        assertEquals(GenericDataType.BOOLEAN, service.toGenericDataType("bool"))
    }

    @Test
    fun `toGenericDataType returns ANY for unknown types`() {
        assertEquals(GenericDataType.ANY, service.toGenericDataType("bytea"))
        assertEquals(GenericDataType.ANY, service.toGenericDataType("json"))
    }

    // ── matchesDataType tests ──────────────────────────────────────────────

    @Test
    fun `matchesDataType ANY always returns true`() {
        assertTrue(service.matchesDataType("integer", GenericDataType.ANY))
        assertTrue(service.matchesDataType("varchar", GenericDataType.ANY))
    }

    @Test
    fun `matchesDataType returns false for mismatched type`() {
        assertFalse(service.matchesDataType("varchar", GenericDataType.NUMERIC))
        assertFalse(service.matchesDataType("integer", GenericDataType.TEXT))
    }

    // ── previewRule tests ─────────────────────────────────────────────────

    @Test
    fun `previewRule returns matching columns from schema snapshot`() {
        val schemaJson = """
            {"tables":[
                {"tableName":"users","columns":[
                    {"name":"user_id","type":"integer","nullable":false},
                    {"name":"email","type":"varchar","nullable":true}
                ]},
                {"tableName":"transactions","columns":[
                    {"name":"tx_id","type":"integer","nullable":false},
                    {"name":"amount","type":"decimal","nullable":true}
                ]}
            ]}
        """.trimIndent()
        val snapshot = SchemaSnapshot(workspaceId = 1L, schemaJson = schemaJson)
        whenever(schemaSnapshotRepository.findTopByWorkspaceIdOrderBySnapshotAtDesc(1L))
            .thenReturn(snapshot)

        val request = CustomRulePreviewRequest(
            workspaceId = 1L,
            dataTypeFilter = GenericDataType.NUMERIC,
            matchers = listOf(CustomRuleMatcherDto(MatcherType.ENDS_WITH, "id", false))
        )
        val results = service.previewRule(request)

        assertEquals(2, results.size)
        assertTrue(results.any { it.tableName == "users" && it.columnName == "user_id" })
        assertTrue(results.any { it.tableName == "transactions" && it.columnName == "tx_id" })
    }

    @Test
    fun `previewRule returns empty list when no snapshot exists`() {
        whenever(schemaSnapshotRepository.findTopByWorkspaceIdOrderBySnapshotAtDesc(1L))
            .thenReturn(null)

        val request = CustomRulePreviewRequest(
            workspaceId = 1L,
            matchers = listOf(CustomRuleMatcherDto(MatcherType.CONTAINS, "id", false))
        )
        val results = service.previewRule(request)
        assertTrue(results.isEmpty())
    }

    // ── createRule tests ──────────────────────────────────────────────────

    @Test
    fun `createRule saves and returns rule`() {
        whenever(customRuleRepository.existsByName("Internal_ID")).thenReturn(false)
        val saved = CustomSensitivityRule(
            id = 1L,
            name = "Internal_ID",
            dataTypeFilter = GenericDataType.NUMERIC,
            matchersJson = """[{"matcherType":"CONTAINS","value":"uid","caseSensitive":false}]"""
        )
        whenever(customRuleRepository.save(any())).thenReturn(saved)

        val request = CustomSensitivityRuleRequest(
            name = "Internal_ID",
            dataTypeFilter = GenericDataType.NUMERIC,
            matchers = listOf(CustomRuleMatcherDto(MatcherType.CONTAINS, "uid", false))
        )
        val response = service.createRule(request)
        assertEquals("Internal_ID", response.name)
        assertEquals(GenericDataType.NUMERIC, response.dataTypeFilter)
    }

    @Test
    fun `createRule throws when name already exists`() {
        whenever(customRuleRepository.existsByName("Internal_ID")).thenReturn(true)

        val request = CustomSensitivityRuleRequest(name = "Internal_ID")
        assertThrows(IllegalArgumentException::class.java) { service.createRule(request) }
    }

    @Test
    fun `deleteRule throws when rule not found`() {
        whenever(customRuleRepository.findById(99L)).thenReturn(Optional.empty())
        assertThrows(NoSuchElementException::class.java) { service.deleteRule(99L) }
    }
}
