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

    @Test
    fun `TITLE generates a non-blank name prefix`() {
        val result = service.generateValue(GeneratorType.TITLE, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `JOB_TITLE generates a non-blank job title`() {
        val result = service.generateValue(GeneratorType.JOB_TITLE, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `NATIONALITY generates a non-blank nationality`() {
        val result = service.generateValue(GeneratorType.NATIONALITY, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `COMPANY_NAME generates a non-blank company name`() {
        val result = service.generateValue(GeneratorType.COMPANY_NAME, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `DEPARTMENT generates a non-blank department`() {
        val result = service.generateValue(GeneratorType.DEPARTMENT, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `CURRENCY_CODE generates a non-blank currency code`() {
        val result = service.generateValue(GeneratorType.CURRENCY_CODE, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `DOMAIN_NAME generates a non-blank domain`() {
        val result = service.generateValue(GeneratorType.DOMAIN_NAME, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `USER_AGENT generates a non-blank user agent`() {
        val result = service.generateValue(GeneratorType.USER_AGENT, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `LATITUDE generates a non-blank latitude`() {
        val result = service.generateValue(GeneratorType.LATITUDE, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `LONGITUDE generates a non-blank longitude`() {
        val result = service.generateValue(GeneratorType.LONGITUDE, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `TIME_ZONE generates a non-blank timezone`() {
        val result = service.generateValue(GeneratorType.TIME_ZONE, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `BOOLEAN generates a Boolean value`() {
        val result = service.generateValue(GeneratorType.BOOLEAN, null, null)
        assertNotNull(result)
        assertInstanceOf(java.lang.Boolean::class.java, result)
    }

    @Test
    fun `LOREM generates a non-blank paragraph`() {
        val result = service.generateValue(GeneratorType.LOREM, null, null)
        assertNotNull(result)
        assertTrue((result as String).isNotBlank())
    }

    @Test
    fun `TIMESTAMP generates a java sql Timestamp`() {
        val result = service.generateValue(GeneratorType.TIMESTAMP, null, null)
        assertNotNull(result)
        assertInstanceOf(java.sql.Timestamp::class.java, result)
    }

    @Test
    fun `HASH generator produces 64-char hex digest`() {
        val result = service.generateValue(GeneratorType.HASH, "hello", null)
        assertNotNull(result)
        val hex = result.toString()
        assertEquals(64, hex.length, "SHA-256 produces 64 hex chars")
        assertTrue(hex.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `HASH generator is deterministic for same input`() {
        val a = service.generateValue(GeneratorType.HASH, "test-value", null)
        val b = service.generateValue(GeneratorType.HASH, "test-value", null)
        assertEquals(a, b)
    }

    @Test
    fun `HASH generator returns null for null input`() {
        assertNull(service.generateValue(GeneratorType.HASH, null, null))
    }

    @Test
    fun `SCRAMBLE generator contains same characters`() {
        val input = "hello123"
        val result = service.generateValue(GeneratorType.SCRAMBLE, input, null)?.toString() ?: ""
        assertEquals(input.toList().sorted(), result.toList().sorted())
    }

    @Test
    fun `TOKENIZE generator preserves character classes`() {
        val input = "AB-12-cd"
        val result = service.generateValue(GeneratorType.TOKENIZE, input, null)?.toString() ?: ""
        assertEquals(input.length, result.length)
        input.zip(result).forEach { (orig, masked) ->
            when {
                orig.isUpperCase() -> assertTrue(masked.isUpperCase())
                orig.isLowerCase() -> assertTrue(masked.isLowerCase())
                orig.isDigit() -> assertTrue(masked.isDigit())
                else -> assertEquals(orig, masked)
            }
        }
    }

    @Test
    fun `DATE_SHIFT generator shifts LocalDate within range`() {
        val original = java.time.LocalDate.of(2024, 6, 15)
        val result = service.generateValue(GeneratorType.DATE_SHIFT, original, mapOf("maxDays" to "30"))
        assertNotNull(result)
        val shifted = result as java.time.LocalDate
        assertTrue(shifted >= original.minusDays(30))
        assertTrue(shifted <= original.plusDays(30))
    }

    @Test
    fun `DATE_BUCKET generator rounds to month start`() {
        val original = java.time.LocalDate.of(2024, 6, 17)
        val result = service.generateValue(GeneratorType.DATE_BUCKET, original, mapOf("bucket" to "month"))
        assertEquals(java.time.LocalDate.of(2024, 6, 1), result)
    }

    @Test
    fun `DATE_BUCKET generator rounds to year start`() {
        val original = java.time.LocalDate.of(2024, 9, 30)
        val result = service.generateValue(GeneratorType.DATE_BUCKET, original, mapOf("bucket" to "year"))
        assertEquals(java.time.LocalDate.of(2024, 1, 1), result)
    }

    @Test
    fun `DATE_BUCKET generator rounds to quarter start`() {
        val original = java.time.LocalDate.of(2024, 8, 15)
        val result = service.generateValue(GeneratorType.DATE_BUCKET, original, mapOf("bucket" to "quarter"))
        assertEquals(java.time.LocalDate.of(2024, 7, 1), result)
    }

    @Test
    fun `NUMERIC_NOISE generator adds noise within percentage`() {
        val result = service.generateValue(GeneratorType.NUMERIC_NOISE, 100.0, mapOf("percentage" to "10"))
        assertNotNull(result)
        val value = (result as Number).toDouble()
        assertTrue(value >= 90.0 && value <= 110.0)
    }

    @Test
    fun `NUMERIC_NOISE generator returns null for null input`() {
        assertNull(service.generateValue(GeneratorType.NUMERIC_NOISE, null, null))
    }

    @Test
    fun `GENERALISE generator buckets numeric value`() {
        val rawParams = """{"buckets":[{"min":0,"max":18,"label":"0-17"},{"min":18,"max":65,"label":"18-64"}]}"""
        val result = service.generateValue(GeneratorType.GENERALISE, "25", null, rawParams = rawParams)
        assertEquals("18-64", result)
    }

    @Test
    fun `GENERALISE generator truncates string when no buckets`() {
        val result = service.generateValue(GeneratorType.GENERALISE, "London", mapOf("keepChars" to "3"))
        assertEquals("Lon...", result)
    }

    @Test
    fun `TEXT_REDACT redacts email addresses in free text`() {
        val text = "Contact john.doe@example.com for info"
        val result = service.generateValue(GeneratorType.TEXT_REDACT, text, null)
        assertFalse(result.toString().contains("john.doe@example.com"))
        assertTrue(result.toString().contains("[REDACTED]"))
    }

    @Test
    fun `TEXT_REDACT redacts phone numbers in free text`() {
        val text = "Call me at 555-123-4567 anytime"
        val result = service.generateValue(GeneratorType.TEXT_REDACT, text, null)
        assertFalse(result.toString().contains("555-123-4567"))
        assertTrue(result.toString().contains("[REDACTED]"))
    }

    @Test
    fun `TEXT_REDACT returns null when original value is null`() {
        val result = service.generateValue(GeneratorType.TEXT_REDACT, null, null)
        assertNull(result)
    }

    @Test
    fun `TEXT_REDACT uses custom redact token from params`() {
        val text = "Email: test@corp.org"
        val result = service.generateValue(GeneratorType.TEXT_REDACT, text, mapOf("token" to "***"))
        assertTrue(result.toString().contains("***"))
        assertFalse(result.toString().contains("test@corp.org"))
    }
}

