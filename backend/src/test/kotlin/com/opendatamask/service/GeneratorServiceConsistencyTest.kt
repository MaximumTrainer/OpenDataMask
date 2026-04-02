package com.opendatamask.service

import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.ConsistencyMode
import com.opendatamask.domain.model.GeneratorType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GeneratorServiceConsistencyTest {

    private lateinit var service: GeneratorService

    @BeforeEach
    fun setup() {
        service = GeneratorService("0123456789abcdef")
    }

    @Test
    fun `same input always produces same output with consistent mode`() {
        val secret = service.computeWorkspaceSecret(1L)
        val result1 = service.generateValueConsistent(GeneratorType.EMAIL, null, "user@example.com", secret)
        val result2 = service.generateValueConsistent(GeneratorType.EMAIL, null, "user@example.com", secret)
        assertEquals(result1, result2, "Consistent mode must produce the same output for the same input")
    }

    @Test
    fun `different inputs produce different outputs`() {
        val secret = service.computeWorkspaceSecret(1L)
        val result1 = service.generateValueConsistent(GeneratorType.EMAIL, null, "alice@example.com", secret)
        val result2 = service.generateValueConsistent(GeneratorType.EMAIL, null, "bob@example.com", secret)
        assertNotEquals(result1, result2, "Different inputs should (almost certainly) produce different outputs")
    }

    @Test
    fun `different workspace secrets produce different outputs for same input`() {
        val secret1 = service.computeWorkspaceSecret(1L)
        val secret2 = service.computeWorkspaceSecret(2L)
        assertNotEquals(secret1, secret2, "Different workspace IDs should yield different secrets")
        val result1 = service.generateValueConsistent(GeneratorType.NAME, null, "Alice", secret1)
        val result2 = service.generateValueConsistent(GeneratorType.NAME, null, "Alice", secret2)
        assertNotEquals(result1, result2)
    }

    @Test
    fun `hmacSeed produces same seed for same inputs`() {
        val secret = service.computeWorkspaceSecret(1L)
        val seed1 = service.hmacSeed(secret, "test-value")
        val seed2 = service.hmacSeed(secret, "test-value")
        assertEquals(seed1, seed2)
    }

    @Test
    fun `linked columns use same faker instance producing deterministic results`() {
        val gen1 = ColumnGenerator(
            tableConfigurationId = 1L,
            columnName = "first_name",
            generatorType = GeneratorType.FIRST_NAME,
            consistencyMode = ConsistencyMode.CONSISTENT,
            linkKey = "name_group"
        )
        val gen2 = ColumnGenerator(
            tableConfigurationId = 1L,
            columnName = "last_name",
            generatorType = GeneratorType.LAST_NAME,
            consistencyMode = ConsistencyMode.CONSISTENT,
            linkKey = "name_group"
        )
        val row = mapOf<String, Any?>("first_name" to "Alice", "last_name" to "Smith")
        val secret = service.computeWorkspaceSecret(1L)

        val result1 = service.applyGenerators(row, listOf(gen1, gen2), secret)
        val result2 = service.applyGenerators(row, listOf(gen1, gen2), secret)

        assertEquals(result1["first_name"], result2["first_name"], "Linked first_name must be deterministic")
        assertEquals(result1["last_name"], result2["last_name"], "Linked last_name must be deterministic")
    }

    @Test
    fun `applyGenerators without workspaceSecret uses random mode`() {
        val gen = ColumnGenerator(
            tableConfigurationId = 1L,
            columnName = "email",
            generatorType = GeneratorType.EMAIL,
            consistencyMode = ConsistencyMode.CONSISTENT
        )
        val row = mapOf<String, Any?>("email" to "original@test.com")
        // Without workspaceSecret, CONSISTENT flag is ignored and random mode is used
        val result1 = service.applyGenerators(row, listOf(gen), workspaceSecret = null)
        assertNotNull(result1["email"])
    }

    @Test
    fun `consistent mode across two generator types same original value same seed`() {
        val secret = service.computeWorkspaceSecret(5L)
        // Call generateValueConsistent twice with the same original value and type → same result
        val r1 = service.generateValueConsistent(GeneratorType.FIRST_NAME, null, "John", secret)
        val r2 = service.generateValueConsistent(GeneratorType.FIRST_NAME, null, "John", secret)
        assertEquals(r1, r2)
    }
}
