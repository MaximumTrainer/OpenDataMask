package com.opendatamask.application.service

import com.opendatamask.domain.model.PIIMaskingRule
import com.opendatamask.domain.model.RedactRule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DefaultRuleRegistryTest {

    private lateinit var registry: DefaultRuleRegistry

    @BeforeEach
    fun setUp() {
        registry = DefaultRuleRegistry()
    }

    @Test
    fun `registry contains all built-in rules after initialisation`() {
        val ids = registry.getAllRuleIds()
        assertTrue(ids.contains("pass_through"), "missing pass_through")
        assertTrue(ids.contains("redact"), "missing redact")
        assertTrue(ids.contains("partial_mask"), "missing partial_mask")
        assertTrue(ids.contains("hash"), "missing hash")
    }

    @Test
    fun `getRule returns rule for known ID`() {
        val rule = registry.getRule("redact")
        assertNotNull(rule)
        assertEquals("[REDACTED]", rule!!.mask("sensitive"))
    }

    @Test
    fun `getRule returns null for unknown ID`() {
        assertNull(registry.getRule("unknown_rule"))
    }

    @Test
    fun `registerCustomRule makes rule available via getRule`() {
        val customRule = object : PIIMaskingRule {
            override val ruleId = "eu_gdpr_mask"
            override fun mask(input: Any?): Any? = if (input == null) null else "EU_MASKED"
        }

        registry.registerCustomRule(customRule)

        val found = registry.getRule("eu_gdpr_mask")
        assertNotNull(found)
        assertEquals("EU_MASKED", found!!.mask("personal_data"))
    }

    @Test
    fun `registerCustomRule overrides built-in rule when same ID is used`() {
        val override = object : PIIMaskingRule {
            override val ruleId = "redact"
            override fun mask(input: Any?): Any? = "[CUSTOM_REDACTED]"
        }

        registry.registerCustomRule(override)

        assertEquals("[CUSTOM_REDACTED]", registry.getRule("redact")!!.mask("x"))
    }

    @Test
    fun `getAllRuleIds includes custom rules after registration`() {
        val customRule = object : PIIMaskingRule {
            override val ruleId = "custom_biz_rule"
            override fun mask(input: Any?): Any? = null
        }
        registry.registerCustomRule(customRule)

        assertTrue(registry.getAllRuleIds().contains("custom_biz_rule"))
    }
}
