package com.opendatamask.service

import com.opendatamask.model.GeneratorType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GeneratorServiceCompositeTest {

    private lateinit var service: GeneratorService

    @BeforeEach
    fun setup() {
        service = GeneratorService("0123456789abcdef")
    }

    // ── PARTIAL_MASK ──────────────────────────────────────────────────────────

    @Test
    fun `PARTIAL_MASK with maskEnd=-4 masks alphanumeric preserving non-alphanumeric`() {
        val result = service.generateValue(
            GeneratorType.PARTIAL_MASK,
            "4111-1111-1111-1234",
            mapOf("maskEnd" to "-4", "maskChar" to "*")
        ) as String
        assertEquals("****-****-****-1234", result)
    }

    @Test
    fun `PARTIAL_MASK with keepLast backward compat masks suffix`() {
        val result = service.generateValue(
            GeneratorType.PARTIAL_MASK,
            "4111111111111111",
            mapOf("keepLast" to "4")
        ) as String
        assertTrue(result.endsWith("1111"))
        assertTrue(result.startsWith("*"))
        assertEquals(16, result.length)
    }

    @Test
    fun `PARTIAL_MASK default params mask all but last 4`() {
        val result = service.generateValue(
            GeneratorType.PARTIAL_MASK,
            "ABCDEFGH",
            null
        ) as String
        assertEquals("****EFGH", result)
    }

    @Test
    fun `PARTIAL_MASK returns null for null input`() {
        val result = service.generateValue(GeneratorType.PARTIAL_MASK, null, null)
        assertNull(result)
    }

    // ── FORMAT_PRESERVING ─────────────────────────────────────────────────────

    @Test
    fun `FORMAT_PRESERVING preserves hyphens and length`() {
        val input = "555-123-4567"
        val result = service.generateValue(GeneratorType.FORMAT_PRESERVING, input, null) as String
        assertEquals(input.length, result.length)
        assertEquals('-', result[3])
        assertEquals('-', result[7])
    }

    @Test
    fun `FORMAT_PRESERVING replaces digits with digits`() {
        val input = "12345"
        val result = service.generateValue(GeneratorType.FORMAT_PRESERVING, input, null) as String
        assertTrue(result.all { it.isDigit() })
        assertEquals(5, result.length)
    }

    @Test
    fun `FORMAT_PRESERVING preserves letter case`() {
        val input = "Hello-World"
        val result = service.generateValue(GeneratorType.FORMAT_PRESERVING, input, null) as String
        assertEquals(input.length, result.length)
        assertTrue(result[0].isUpperCase(), "First letter should remain uppercase")
        assertTrue(result[6].isUpperCase(), "W position should remain uppercase")
        assertEquals('-', result[5])
    }

    @Test
    fun `FORMAT_PRESERVING returns null for null input`() {
        val result = service.generateValue(GeneratorType.FORMAT_PRESERVING, null, null)
        assertNull(result)
    }

    // ── CONDITIONAL ──────────────────────────────────────────────────────────

    @Test
    fun `CONDITIONAL applies matching condition`() {
        val rawParams = """{"conditions":[{"when":"value == 'US'","then":"CONSTANT","thenValue":"USA"}],"default":"NULL"}"""
        val result = service.generateValue(
            GeneratorType.CONDITIONAL,
            "US",
            null,
            rawParams = rawParams
        )
        assertEquals("USA", result)
    }

    @Test
    fun `CONDITIONAL applies default when no match`() {
        val rawParams = """{"conditions":[{"when":"value == 'US'","then":"CONSTANT","thenValue":"USA"}],"default":"NULL"}"""
        val result = service.generateValue(
            GeneratorType.CONDITIONAL,
            "CA",
            null,
            rawParams = rawParams
        )
        assertNull(result)
    }

    @Test
    fun `CONDITIONAL without rawParams returns original value`() {
        val result = service.generateValue(GeneratorType.CONDITIONAL, "original", null)
        assertEquals("original", result)
    }

    @Test
    fun `CONDITIONAL matches second condition`() {
        val rawParams = """{"conditions":[{"when":"value == 'US'","then":"CONSTANT","thenValue":"USA"},{"when":"value == 'GB'","then":"CONSTANT","thenValue":"GBR"}],"default":"NULL"}"""
        val result = service.generateValue(
            GeneratorType.CONDITIONAL,
            "GB",
            null,
            rawParams = rawParams
        )
        assertEquals("GBR", result)
    }

    // ── SEQUENTIAL ────────────────────────────────────────────────────────────

    @Test
    fun `SEQUENTIAL starts at configured start value`() {
        val result = service.generateValue(
            GeneratorType.SEQUENTIAL,
            null,
            mapOf("start" to "10", "step" to "1"),
            columnKey = "test:seq_col_start"
        ) as String
        assertEquals("10", result)
    }

    @Test
    fun `SEQUENTIAL increments by step`() {
        val key = "test:seq_col_step"
        val r1 = service.generateValue(GeneratorType.SEQUENTIAL, null, mapOf("start" to "1", "step" to "5"), columnKey = key) as String
        val r2 = service.generateValue(GeneratorType.SEQUENTIAL, null, mapOf("start" to "1", "step" to "5"), columnKey = key) as String
        val r3 = service.generateValue(GeneratorType.SEQUENTIAL, null, mapOf("start" to "1", "step" to "5"), columnKey = key) as String
        assertEquals("1", r1)
        assertEquals("6", r2)
        assertEquals("11", r3)
    }

    @Test
    fun `SEQUENTIAL uses separate counters per column key`() {
        val r1 = service.generateValue(GeneratorType.SEQUENTIAL, null, mapOf("start" to "1", "step" to "1"), columnKey = "table:col_a") as String
        val r2 = service.generateValue(GeneratorType.SEQUENTIAL, null, mapOf("start" to "1", "step" to "1"), columnKey = "table:col_b") as String
        assertEquals("1", r1)
        assertEquals("1", r2, "Different column keys should have independent counters")
    }

    // ── RANDOM_INT ────────────────────────────────────────────────────────────

    @Test
    fun `RANDOM_INT returns string within range`() {
        val result = service.generateValue(
            GeneratorType.RANDOM_INT,
            null,
            mapOf("min" to "1000", "max" to "9999999")
        ) as String
        val num = result.toLong()
        assertTrue(num >= 1000, "Result $num should be >= 1000")
        assertTrue(num <= 9999999, "Result $num should be <= 9999999")
    }

    @Test
    fun `RANDOM_INT returns a string not an integer`() {
        val result = service.generateValue(GeneratorType.RANDOM_INT, null, mapOf("min" to "1", "max" to "100"))
        assertNotNull(result)
        assertTrue(result is String, "RANDOM_INT should return a String, got ${result?.javaClass}")
    }

    @Test
    fun `RANDOM_INT uses defaults when no params`() {
        val result = service.generateValue(GeneratorType.RANDOM_INT, null, null) as String
        val num = result.toLong()
        assertTrue(num >= 1)
        assertTrue(num <= 999999)
    }
}
