package com.opendatamask.application.service

import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.GeneratorType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class GeneratorServiceTest {

    private lateinit var service: GeneratorService

    @BeforeEach
    fun setup() {
        service = GeneratorService("0123456789abcdef")
    }

    @Test
    fun `NAME generates a non-blank name`() {
        val result = service.generateValue(GeneratorType.NAME, "original", null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `EMAIL generates a valid-looking email`() {
        val result = service.generateValue(GeneratorType.EMAIL, "original@example.com", null) as String
        assertTrue(result.contains("@"))
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `PHONE generates a non-blank phone`() {
        val result = service.generateValue(GeneratorType.PHONE, "555-1234", null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `ADDRESS generates a non-blank address`() {
        val result = service.generateValue(GeneratorType.ADDRESS, "123 Main St", null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `SSN generates a non-blank SSN`() {
        val result = service.generateValue(GeneratorType.SSN, "123-45-6789", null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `CREDIT_CARD generates a non-blank credit card number`() {
        val result = service.generateValue(GeneratorType.CREDIT_CARD, "4111111111111111", null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `DATE generates a java sql Date`() {
        val result = service.generateValue(GeneratorType.DATE, "2024-01-01", null)
        assertNotNull(result)
        assertInstanceOf(java.sql.Date::class.java, result)
    }

    @Test
    fun `BIRTH_DATE generates a java sql Date`() {
        val result = service.generateValue(GeneratorType.BIRTH_DATE, "1990-01-15", null)
        assertNotNull(result)
        assertInstanceOf(java.sql.Date::class.java, result)
    }

    @Test
    fun `UUID generates a valid UUID string`() {
        val result = service.generateValue(GeneratorType.UUID, "old-uuid", null) as String
        assertEquals(36, result.length)
        assertTrue(result.contains("-"))
    }

    @Test
    fun `CONSTANT returns the configured constant value`() {
        val result = service.generateValue(GeneratorType.CONSTANT, "original", mapOf("value" to "REDACTED"))
        assertEquals("REDACTED", result)
    }

    @Test
    fun `CONSTANT returns empty string when no value configured`() {
        val result = service.generateValue(GeneratorType.CONSTANT, "original", null)
        assertEquals("", result)
    }

    @Test
    fun `NULL returns null`() {
        val result = service.generateValue(GeneratorType.NULL, "original", null)
        assertNull(result)
    }

    @Test
    fun `CUSTOM with expression returns non-null`() {
        val result = service.generateValue(GeneratorType.CUSTOM, "original", mapOf("value" to "custom-value"))
        assertEquals("custom-value", result)
    }

    @Test
    fun `FIRST_NAME generates a non-blank first name`() {
        val result = service.generateValue(GeneratorType.FIRST_NAME, "original", null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `IP_ADDRESS generates a valid IPv4 address`() {
        val result = service.generateValue(GeneratorType.IP_ADDRESS, "192.168.1.1", null) as String
        assertTrue(result.contains("."))
        assertEquals(4, result.split(".").size)
    }

    @Test
    fun `PARTIAL_MASK masks all but last 4 characters`() {
        val result = service.generateValue(GeneratorType.PARTIAL_MASK, "4111111111111111", mapOf("keepLast" to "4")) as String
        assertTrue(result.endsWith("1111"))
        assertTrue(result.startsWith("*"))
    }

    @Test
    fun `FORMAT_PRESERVING preserves non-alphanumeric format`() {
        val result = service.generateValue(GeneratorType.FORMAT_PRESERVING, "555-123-4567", null) as String
        assertEquals('-', result[3])
        assertEquals('-', result[7])
    }

    @Test
    fun `ORGANIZATION generates a non-blank company name`() {
        val result = service.generateValue(GeneratorType.ORGANIZATION, "Acme Corp", null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `applyGenerators transforms specified columns`() {
        val row = mapOf("name" to "Alice", "email" to "alice@example.com", "id" to 1)
        val generators = listOf(
            ColumnGenerator(tableConfigurationId = 1L, columnName = "name", generatorType = GeneratorType.NULL),
            ColumnGenerator(tableConfigurationId = 1L, columnName = "email", generatorType = GeneratorType.CONSTANT, generatorParams = """{"value":"masked@example.com"}""")
        )
        val result = service.applyGenerators(row, generators)
        assertNull(result["name"])
        assertEquals("masked@example.com", result["email"])
        assertEquals(1, result["id"])
    }

    @Test
    fun `generateRows produces the requested number of rows`() {
        val generators = listOf(
            ColumnGenerator(tableConfigurationId = 1L, columnName = "name", generatorType = GeneratorType.NAME),
            ColumnGenerator(tableConfigurationId = 1L, columnName = "email", generatorType = GeneratorType.EMAIL)
        )
        val rows = service.generateRows(generators, 3)
        assertEquals(3, rows.size)
        rows.forEach { row ->
            assertTrue((row["name"] as String).isNotBlank())
            assertTrue((row["email"] as String).contains("@"))
        }
    }

    @Test
    fun `generateRows returns empty list when count is zero`() {
        val generators = listOf(
            ColumnGenerator(tableConfigurationId = 1L, columnName = "name", generatorType = GeneratorType.NAME)
        )
        val rows = service.generateRows(generators, 0)
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `CUSTOM falls back to originalValue when no params provided`() {
        val result = service.generateValue(GeneratorType.CUSTOM, "fallback", null)
        assertEquals("fallback", result)
    }

    @Test
    fun `RANDOM_INT generates a Long`() {
        val result = service.generateValue(GeneratorType.RANDOM_INT, null, mapOf("min" to "30000", "max" to "200000"))
        assertNotNull(result)
        assertInstanceOf(java.lang.Long::class.java, result)
        val value = result as Long
        assertTrue(value in 30000..199999)
    }

    @Test
    fun `SEQUENTIAL generates a Long`() {
        val result = service.generateValue(GeneratorType.SEQUENTIAL, null, mapOf("start" to "1", "step" to "1"), columnKey = "test:seq")
        assertNotNull(result)
        assertInstanceOf(java.lang.Long::class.java, result)
        assertEquals(1L, result)
    }

    @Test
    fun `MONEY_AMOUNT generates a BigDecimal`() {
        val result = service.generateValue(GeneratorType.MONEY_AMOUNT, null, null)
        assertNotNull(result)
        assertInstanceOf(java.math.BigDecimal::class.java, result)
    }
}
