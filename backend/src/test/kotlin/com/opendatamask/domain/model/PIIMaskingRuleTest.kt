package com.opendatamask.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PIIMaskingRuleTest {

    // ── PassThroughRule ──────────────────────────────────────────────────────

    @Test
    fun `PassThroughRule returns input unchanged`() {
        val rule = PassThroughRule()
        assertEquals("hello", rule.mask("hello"))
        assertEquals(42, rule.mask(42))
    }

    @Test
    fun `PassThroughRule returns null for null input`() {
        assertNull(PassThroughRule().mask(null))
    }

    // ── RedactRule ───────────────────────────────────────────────────────────

    @Test
    fun `RedactRule replaces non-null value with REDACTED token`() {
        val rule = RedactRule()
        assertEquals("[REDACTED]", rule.mask("john.doe@example.com"))
        assertEquals("[REDACTED]", rule.mask(12345))
        assertEquals("[REDACTED]", rule.mask(""))
    }

    @Test
    fun `RedactRule returns null for null input`() {
        assertNull(RedactRule().mask(null))
    }

    // ── PartialMaskRule ──────────────────────────────────────────────────────

    @Test
    fun `PartialMaskRule masks middle characters keeping last 4`() {
        val rule = PartialMaskRule(keepFirst = 0, keepLast = 4)
        assertEquals("****1234", rule.mask("12341234"))
    }

    @Test
    fun `PartialMaskRule keeps first and last characters`() {
        val rule = PartialMaskRule(keepFirst = 1, keepLast = 1)
        assertEquals("j***e", rule.mask("johne"))
    }

    @Test
    fun `PartialMaskRule with short input returns input unchanged`() {
        val rule = PartialMaskRule(keepFirst = 2, keepLast = 3)
        assertEquals("hello", PartialMaskRule(keepFirst = 0, keepLast = 10).mask("hello"))
    }

    @Test
    fun `PartialMaskRule returns null for null input`() {
        assertNull(PartialMaskRule().mask(null))
    }

    @Test
    fun `PartialMaskRule uses custom mask character`() {
        val rule = PartialMaskRule(keepFirst = 0, keepLast = 4, maskChar = '#')
        assertEquals("####5678", rule.mask("12345678"))
    }

    // ── HashRule ─────────────────────────────────────────────────────────────

    @Test
    fun `HashRule produces consistent SHA-256 hex digest`() {
        val rule = HashRule()
        val result = rule.mask("hello")
        assertNotNull(result)
        // SHA-256 of "hello" is well-known
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result)
    }

    @Test
    fun `HashRule with salt produces different digest`() {
        val noSalt = HashRule().mask("hello")
        val withSalt = HashRule(salt = "pepper").mask("hello")
        assertNotEquals(noSalt, withSalt)
    }

    @Test
    fun `HashRule returns null for null input`() {
        assertNull(HashRule().mask(null))
    }

    @Test
    fun `HashRule output is always 64 hex characters`() {
        val result = HashRule().mask("test value") as String
        assertEquals(64, result.length)
        assertTrue(result.all { it in '0'..'9' || it in 'a'..'f' })
    }

    // ── RegexRule ────────────────────────────────────────────────────────────

    @Test
    fun `RegexRule replaces all pattern matches`() {
        val rule = RegexRule(pattern = "\\d", replacement = "#")
        assertEquals("###-##-####", rule.mask("123-45-6789"))
    }

    @Test
    fun `RegexRule with capture group replacement`() {
        val rule = RegexRule(pattern = "(\\w+)@(\\w+\\.\\w+)", replacement = "***@$2")
        assertEquals("***@example.com", rule.mask("john@example.com"))
    }

    @Test
    fun `RegexRule returns null for null input`() {
        assertNull(RegexRule(".*", "").mask(null))
    }

    @Test
    fun `RegexRule with no match returns original string`() {
        val rule = RegexRule(pattern = "\\d+", replacement = "")
        assertEquals("hello", rule.mask("hello"))
    }

    // ── ruleId contract ──────────────────────────────────────────────────────

    @Test
    fun `built-in rules have expected stable rule IDs`() {
        assertEquals("pass_through", PassThroughRule().ruleId)
        assertEquals("redact", RedactRule().ruleId)
        assertEquals("partial_mask", PartialMaskRule().ruleId)
        assertEquals("hash", HashRule().ruleId)
        assertEquals("regex", RegexRule("", "").ruleId)
    }
}
