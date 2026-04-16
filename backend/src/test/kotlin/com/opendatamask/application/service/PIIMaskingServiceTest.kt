package com.opendatamask.application.service

import com.opendatamask.domain.model.CustomDataMapping
import com.opendatamask.domain.model.MappingAction
import com.opendatamask.domain.model.MaskingStrategy
import com.opendatamask.domain.model.RedactRule
import com.opendatamask.domain.port.output.CustomDataMappingPort
import com.opendatamask.domain.port.output.RuleRegistryPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PIIMaskingServiceTest {

    @Mock private lateinit var ruleRegistry: RuleRegistryPort
    @Mock private lateinit var customDataMappingPort: CustomDataMappingPort

    private lateinit var service: PIIMaskingService

    @BeforeEach
    fun setUp() {
        service = PIIMaskingService(ruleRegistry, customDataMappingPort)
        // Wire the real built-in registry so REDACT tests use the actual rule
        whenever(ruleRegistry.getRule("redact")).thenReturn(RedactRule())
    }

    private fun mapping(
        column: String,
        action: MappingAction,
        strategy: MaskingStrategy? = null,
        params: String? = null
    ) = CustomDataMapping(
        id = 1L, workspaceId = 1L, connectionId = 2L,
        tableName = "users", columnName = column,
        action = action, maskingStrategy = strategy,
        fakeGeneratorType = null, piiRuleParams = params
    )

    // ── no mappings ──────────────────────────────────────────────────────────

    @Test
    fun `applyMappings returns row unchanged when no mappings exist`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(emptyList())

        val row = mapOf("email" to "john@example.com", "id" to 1L)
        val result = service.applyMappings(1L, 2L, "users", row)

        assertEquals(row, result)
    }

    // ── MIGRATE_AS_IS ────────────────────────────────────────────────────────

    @Test
    fun `applyMappings passes through columns with MIGRATE_AS_IS action`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("email", MappingAction.MIGRATE_AS_IS)))

        val row = mapOf("email" to "john@example.com")
        val result = service.applyMappings(1L, 2L, "users", row)

        assertEquals("john@example.com", result["email"])
    }

    // ── NULL strategy ────────────────────────────────────────────────────────

    @Test
    fun `applyMappings nullifies column with NULL strategy`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("ssn", MappingAction.MASK, MaskingStrategy.NULL)))

        val row = mapOf("ssn" to "123-45-6789")
        val result = service.applyMappings(1L, 2L, "users", row)

        assertNull(result["ssn"])
    }

    // ── REDACT strategy ──────────────────────────────────────────────────────

    @Test
    fun `applyMappings redacts column with REDACT strategy`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("email", MappingAction.MASK, MaskingStrategy.REDACT)))

        val row = mapOf("email" to "john@example.com")
        val result = service.applyMappings(1L, 2L, "users", row)

        assertEquals("[REDACTED]", result["email"])
    }

    // ── HASH strategy ────────────────────────────────────────────────────────

    @Test
    fun `applyMappings hashes column with HASH strategy`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("email", MappingAction.MASK, MaskingStrategy.HASH)))

        val row = mapOf("email" to "hello")
        val result = service.applyMappings(1L, 2L, "users", row)

        // SHA-256 of "hello" with no salt
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result["email"])
    }

    @Test
    fun `applyMappings hashes with salt when piiRuleParams contains salt`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("email", MappingAction.MASK, MaskingStrategy.HASH, """{"salt":"pepper"}""")))

        val row = mapOf("email" to "hello")
        val result = service.applyMappings(1L, 2L, "users", row)

        // Must differ from the unsalted hash
        assertNotEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result["email"])
        assertNotNull(result["email"])
    }

    // ── PARTIAL_MASK strategy ────────────────────────────────────────────────

    @Test
    fun `applyMappings applies partial mask with default params`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("credit_card", MappingAction.MASK, MaskingStrategy.PARTIAL_MASK)))

        val row = mapOf("credit_card" to "4111111111111234")
        val result = service.applyMappings(1L, 2L, "users", row)

        // Default: keepFirst=0, keepLast=4 → ************1234
        assertEquals("************1234", result["credit_card"])
    }

    @Test
    fun `applyMappings applies partial mask with custom params`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(
                mapping("name", MappingAction.MASK, MaskingStrategy.PARTIAL_MASK, """{"keepFirst":"1","keepLast":"0"}""")
            ))

        val row = mapOf("name" to "Alice")
        val result = service.applyMappings(1L, 2L, "users", row)

        assertEquals("A****", result["name"])
    }

    // ── REGEX strategy ───────────────────────────────────────────────────────

    @Test
    fun `applyMappings applies regex replacement`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(
                mapping("phone", MappingAction.MASK, MaskingStrategy.REGEX, """{"pattern":"\\d","replacement":"#"}""")
            ))

        val row = mapOf("phone" to "123-456-7890")
        val result = service.applyMappings(1L, 2L, "users", row)

        assertEquals("###-###-####", result["phone"])
    }

    @Test
    fun `applyMappings passes value through unchanged when REGEX pattern is missing`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("phone", MappingAction.MASK, MaskingStrategy.REGEX)))

        val row = mapOf("phone" to "123-456-7890")
        val result = service.applyMappings(1L, 2L, "users", row)

        // Missing params → no-op, not data loss
        assertEquals("123-456-7890", result["phone"])
    }

    @Test
    fun `applyMappings passes value through unchanged when piiRuleParams JSON is invalid`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("phone", MappingAction.MASK, MaskingStrategy.HASH, "not-json")))

        val row = mapOf("phone" to "123-456-7890")
        val result = service.applyMappings(1L, 2L, "users", row)

        // Invalid JSON → parseParams returns null → value passes through unchanged
        assertEquals("123-456-7890", result["phone"])
    }

    // ── custom ruleId dispatch via piiRuleParams ──────────────────────────────

    @Test
    fun `applyMappings dispatches to registry when ruleId is set in piiRuleParams`() {
        val customRule = com.opendatamask.domain.model.RedactRule()
        whenever(ruleRegistry.getRule("my_custom_rule")).thenReturn(customRule)
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(
                mapping("email", MappingAction.MASK, MaskingStrategy.REDACT, """{"ruleId":"my_custom_rule"}""")
            ))

        val row = mapOf("email" to "john@example.com")
        val result = service.applyMappings(1L, 2L, "users", row)

        assertEquals("[REDACTED]", result["email"])
    }

    @Test
    fun `applyMappings passes value through unchanged when custom ruleId is not found in registry`() {
        whenever(ruleRegistry.getRule("unknown_rule")).thenReturn(null)
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(
                mapping("email", MappingAction.MASK, MaskingStrategy.REDACT, """{"ruleId":"unknown_rule"}""")
            ))

        val row = mapOf("email" to "john@example.com")
        val result = service.applyMappings(1L, 2L, "users", row)

        // Unknown rule → pass through with warning log
        assertEquals("john@example.com", result["email"])
    }

    // ── FAKE strategy (handled by GeneratorService, not PIIMaskingService) ───

    @Test
    fun `applyMappings leaves FAKE strategy columns to GeneratorService`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("email", MappingAction.MASK, MaskingStrategy.FAKE)))

        val row = mapOf("email" to "original@example.com")
        val result = service.applyMappings(1L, 2L, "users", row)

        // FAKE strategy is a no-op in PIIMaskingService; value is left for GeneratorService
        assertEquals("original@example.com", result["email"])
    }

    // ── column name case-insensitivity ───────────────────────────────────────

    @Test
    fun `applyMappings matches columns case-insensitively`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("EMAIL", MappingAction.MASK, MaskingStrategy.REDACT)))

        val row = mapOf("email" to "john@example.com")
        val result = service.applyMappings(1L, 2L, "users", row)

        assertEquals("[REDACTED]", result["email"])
    }

    // ── mixed row ────────────────────────────────────────────────────────────

    @Test
    fun `applyMappings handles mixed actions in same row`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(
                mapping("id", MappingAction.MIGRATE_AS_IS),
                mapping("email", MappingAction.MASK, MaskingStrategy.REDACT),
                mapping("ssn", MappingAction.MASK, MaskingStrategy.NULL)
            ))

        val row = mapOf("id" to 42L, "email" to "jane@example.com", "ssn" to "000-00-0000", "name" to "Jane")
        val result = service.applyMappings(1L, 2L, "users", row)

        assertEquals(42L, result["id"])
        assertEquals("[REDACTED]", result["email"])
        assertNull(result["ssn"])
        assertEquals("Jane", result["name"]) // unmapped column passes through
    }

    // ── batch API ─────────────────────────────────────────────────────────────

    @Test
    fun `applyMappingsToRows fetches mappings once and applies to all rows`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(listOf(mapping("email", MappingAction.MASK, MaskingStrategy.REDACT)))

        val rows = listOf(
            mapOf("email" to "alice@example.com"),
            mapOf("email" to "bob@example.com")
        )
        val results = service.applyMappingsToRows(1L, 2L, "users", rows)

        assertEquals(2, results.size)
        assertEquals("[REDACTED]", results[0]["email"])
        assertEquals("[REDACTED]", results[1]["email"])
    }

    @Test
    fun `applyMappingsToRows returns original list unchanged when no mappings exist`() {
        whenever(customDataMappingPort.findByWorkspaceIdAndConnectionIdAndTableName(1L, 2L, "users"))
            .thenReturn(emptyList())

        val rows = listOf(mapOf("email" to "alice@example.com"))
        val results = service.applyMappingsToRows(1L, 2L, "users", rows)

        assertEquals(rows, results)
    }
}
