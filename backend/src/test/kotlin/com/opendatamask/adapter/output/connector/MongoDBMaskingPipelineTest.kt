package com.opendatamask.adapter.output.connector

import com.mongodb.client.MongoClients
import com.opendatamask.application.service.GeneratorService
import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.ConsistencyMode
import com.opendatamask.domain.model.GeneratorType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

// ---------------------------------------------------------------------------
// MongoDBMaskingPipelineTest
//
// End-to-end pipeline using a real MongoDB instance (via Testcontainers):
//   source collection -> read -> apply generators -> write to target -> verify
// ---------------------------------------------------------------------------
@Testcontainers
class MongoDBMaskingPipelineTest {

    companion object {
        @Container
        @JvmStatic
        val mongoContainer: MongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:7.0"))
    }

    private lateinit var source: MongoDBConnector
    private lateinit var target: MongoDBConnector
    private lateinit var generatorService: GeneratorService

    // Sample source data - a collection of customer documents
    private val sourceCustomers = listOf(
        mapOf("_id" to "c001", "name" to "Alice Smith", "email" to "alice@example.com",
              "phone" to "555-111-2222", "ssn" to "123-45-6789", "account_status" to "active"),
        mapOf("_id" to "c002", "name" to "Bob Jones",  "email" to "bob@example.com",
              "phone" to "555-333-4444", "ssn" to "987-65-4321", "account_status" to "inactive"),
        mapOf("_id" to "c003", "name" to "Carol White", "email" to "carol@example.com",
              "phone" to "555-555-6666", "ssn" to "111-22-3333", "account_status" to "active")
    )

    @BeforeEach
    fun setUp() {
        val uri = mongoContainer.connectionString
        // Drop entire databases to guarantee a clean state for every test
        MongoClients.create(uri).use { client ->
            client.getDatabase("source_db").drop()
            client.getDatabase("target_db").drop()
        }
        source = MongoDBConnector(uri, "source_db")
        target = MongoDBConnector(uri, "target_db")
        generatorService = GeneratorService("0123456789abcdef")

        // Seed source collection
        source.writeData("customers", sourceCustomers)
    }

    // ── Helper to build a ColumnGenerator without JPA machinery ─────────────

    private fun buildColumnGenerator(
        tableConfigId: Long = 1L,
        columnName: String,
        type: GeneratorType,
        params: String? = null,
        consistencyMode: ConsistencyMode = ConsistencyMode.RANDOM
    ) = ColumnGenerator(
        id = 0,
        tableConfigurationId = tableConfigId,
        columnName = columnName,
        generatorType = type,
        generatorParams = params,
        consistencyMode = consistencyMode
    )

    // ── 1. Basic masking pipeline ─────────────────────────────────────────────

    @Test
    fun `mask pipeline replaces sensitive fields and preserves non-sensitive fields`() {
        val generators = listOf(
            buildColumnGenerator(columnName = "name",  type = GeneratorType.FULL_NAME),
            buildColumnGenerator(columnName = "email", type = GeneratorType.EMAIL),
            buildColumnGenerator(columnName = "phone", type = GeneratorType.PHONE),
            buildColumnGenerator(columnName = "ssn",   type = GeneratorType.SSN)
        )

        val sourceRows = source.fetchData("customers")
        assertEquals(3, sourceRows.size)

        val maskedRows = sourceRows.map { row -> generatorService.applyGenerators(row, generators) }
        target.writeData("customers", maskedRows)

        val result = target.fetchData("customers")
        assertEquals(3, result.size)

        // Build a lookup by _id so order-independence is maintained
        val originalById = sourceCustomers.associateBy { it["_id"] }
        result.forEach { row ->
            val id = row["_id"]
            val original = originalById[id] ?: fail("Unexpected _id in result: $id")

            // _id is preserved (not in generator list, passed through)
            assertEquals(original["_id"], row["_id"], "_id should be preserved for $id")

            // account_status is not in generators, passed through unchanged
            assertEquals(original["account_status"], row["account_status"],
                "account_status should be passed through for $id")

            // Sensitive columns must be replaced with non-blank values
            assertNotNull(row["name"],  "masked name must not be null for $id")
            assertNotNull(row["email"], "masked email must not be null for $id")
            assertNotNull(row["phone"], "masked phone must not be null for $id")
            assertNotNull(row["ssn"],   "masked ssn must not be null for $id")

            assertTrue((row["name"]  as String).isNotBlank(), "masked name must not be blank for $id")
            assertTrue((row["email"] as String).isNotBlank(), "masked email must not be blank for $id")
            assertTrue((row["phone"] as String).isNotBlank(), "masked phone must not be blank for $id")

            // Masked values must differ from originals (false-positive probability negligible)
            assertNotEquals(original["name"],  row["name"],  "name must be masked for $id")
            assertNotEquals(original["email"], row["email"], "email must be masked for $id")
        }
    }

    // ── 2. Passthrough mode - write source data as-is ─────────────────────────

    @Test
    fun `passthrough mode copies all rows unchanged`() {
        val sourceRows = source.fetchData("customers")
        target.writeData("customers_passthrough", sourceRows)

        val result = target.fetchData("customers_passthrough")
        assertEquals(sourceCustomers.size, result.size)

        val originalById = sourceCustomers.associateBy { it["_id"] }
        result.forEach { row ->
            val original = originalById[row["_id"]] ?: fail("Unexpected _id: ${row["_id"]}")
            assertEquals(original["name"],  row["name"])
            assertEquals(original["email"], row["email"])
            assertEquals(original["_id"],   row["_id"])
        }
    }

    // ── 3. Row limit is respected when fetching from source ───────────────────

    @Test
    fun `fetchData respects row limit`() {
        val rows = source.fetchData("customers", limit = 2)
        assertEquals(2, rows.size)
    }

    // ── 4. Upsert path: masked rows with _id replace existing documents ────────

    @Test
    fun `upsert updates existing documents in target preserving _id`() {
        // First pass - write original data to target
        val sourceRows = source.fetchData("customers")
        target.writeData("customers", sourceRows)
        assertEquals(3, target.fetchData("customers").size)

        // Second pass - mask and upsert (should replace, not add)
        val generators = listOf(
            buildColumnGenerator(columnName = "name",  type = GeneratorType.FULL_NAME),
            buildColumnGenerator(columnName = "email", type = GeneratorType.EMAIL)
        )
        val maskedRows = sourceRows.map { row -> generatorService.applyGenerators(row, generators) }
        target.writeData("customers", maskedRows)

        // Still 3 documents (upsert replaced, did not duplicate)
        val result = target.fetchData("customers")
        assertEquals(3, result.size)

        // _id values are unchanged
        val resultIds = result.map { it["_id"] }.toSet()
        val expectedIds = setOf("c001", "c002", "c003")
        assertEquals(expectedIds, resultIds)

        // Names were replaced by masked values
        val resultNames = result.map { it["name"] }.toSet()
        val originalNames = sourceCustomers.map { it["name"] }.toSet()
        assertTrue(
            resultNames.none { it in originalNames },
            "All names should have been replaced by masked values"
        )
    }

    // ── 5. Mixed batch: rows with and without _id ──────────────────────────────

    @Test
    fun `writeData handles mixed batch of rows with and without _id`() {
        val mixedRows = listOf(
            mapOf("_id" to "x001", "field" to "upsert-me"),
            mapOf(                  "field" to "insert-me")
        )
        val count = target.writeData("mixed", mixedRows)
        assertEquals(2, count)

        val stored = target.fetchData("mixed")
        assertEquals(2, stored.size)
        assertEquals(1, stored.count { it.containsKey("_id") && it["_id"] == "x001" })
        assertEquals(1, stored.count { it["field"] == "insert-me" })
    }

    // ── 6. Truncate clears target before re-run ─────────────────────────────────

    @Test
    fun `truncateTable then writeData yields clean result`() {
        // Write all 3 rows once
        target.writeData("customers", source.fetchData("customers"))
        assertEquals(3, target.fetchData("customers").size)

        // Truncate and re-write a single row
        target.truncateTable("customers")
        assertEquals(0, target.fetchData("customers").size)

        target.writeData("customers", listOf(sourceCustomers.first()))
        assertEquals(1, target.fetchData("customers").size)
    }

    // ── 7. Consistent masking produces identical output for same input ──────────

    @Test
    fun `CONSISTENT mode produces deterministic output for same original value`() {
        val generator = buildColumnGenerator(
            columnName = "name",
            type = GeneratorType.FULL_NAME,
            consistencyMode = ConsistencyMode.CONSISTENT
        )
        val workspaceSecret = generatorService.computeWorkspaceSecret(42L)

        val row = mapOf("_id" to "c001", "name" to "Alice Smith")
        val first  = generatorService.applyGenerators(row, listOf(generator), workspaceSecret)
        val second = generatorService.applyGenerators(row, listOf(generator), workspaceSecret)

        assertEquals(first["name"], second["name"],
            "CONSISTENT mode must produce the same masked name for the same original value")
        assertNotEquals("Alice Smith", first["name"],
            "CONSISTENT masked value must differ from original")
    }

    // ── 8. NULL generator nullifies the column ──────────────────────────────────

    @Test
    fun `NULL generator sets column to null in target`() {
        val generators = listOf(
            buildColumnGenerator(columnName = "ssn", type = GeneratorType.NULL)
        )
        val sourceRows = source.fetchData("customers")
        val maskedRows = sourceRows.map { row -> generatorService.applyGenerators(row, generators) }
        target.writeData("customers", maskedRows)

        val result = target.fetchData("customers")
        assertEquals(3, result.size)
        result.forEach { row ->
            // The NULL generator sets ssn to null in the document map before writing.
            // MongoDB stores a BSON null for the field; when read back, the value is null.
            assertNull(row["ssn"], "SSN should be null for ${row["_id"]}")
            // Other fields remain
            assertNotNull(row["name"])
            assertNotNull(row["email"])
        }
    }

    // ── 9. Empty source collection writes zero rows ──────────────────────────────

    @Test
    fun `empty source collection results in zero rows written to target`() {
        val empty = source.fetchData("nonexistent_collection")
        assertEquals(0, empty.size)

        val written = target.writeData("output", empty)
        assertEquals(0, written)
        assertEquals(0, target.fetchData("output").size)
    }
}
