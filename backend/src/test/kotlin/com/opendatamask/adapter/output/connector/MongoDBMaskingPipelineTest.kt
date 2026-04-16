package com.opendatamask.adapter.output.connector

import com.opendatamask.application.service.GeneratorService
import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.ConsistencyMode
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.port.output.ColumnInfo
import com.opendatamask.domain.port.output.DatabaseConnector
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// ---------------------------------------------------------------------------
// InMemoryMongoDBConnector
//
// A test-only subclass of MongoDBConnector that keeps all data in-memory using
// a HashMap instead of talking to a real MongoDB server.  Calling
// createMongoClient() would normally connect to a server; that method is never
// reached because every public API method is overridden here.
//
// The upsert semantics mirror MongoDBConnector.writeData exactly:
//   * rows that carry an "_id" key → replace-or-insert (keyed on _id)
//   * rows without "_id"           → plain insert (new entry every time)
// ---------------------------------------------------------------------------
class InMemoryMongoDBConnector : MongoDBConnector("mongodb://localhost:27017", "testdb") {

    // collection-name → list of stored documents
    private val store: MutableMap<String, MutableList<MutableMap<String, Any?>>> = mutableMapOf()

    private fun col(name: String): MutableList<MutableMap<String, Any?>> =
        store.getOrPut(name) { mutableListOf() }

    override fun testConnection(): Boolean = true

    override fun listTables(): List<String> = store.keys.toList()

    override fun listColumns(tableName: String): List<ColumnInfo> =
        col(tableName).firstOrNull()?.keys?.map { ColumnInfo(it, "mixed") } ?: emptyList()

    override fun fetchData(tableName: String, limit: Int?, whereClause: String?): List<Map<String, Any?>> {
        val all: List<Map<String, Any?>> = col(tableName).toList()
        return if (limit != null) all.take(limit) else all
    }

    override fun createTable(tableName: String, columns: List<ColumnInfo>) {
        col(tableName) // ensure collection exists
    }

    override fun truncateTable(tableName: String) {
        col(tableName).clear()
    }

    override fun writeData(tableName: String, rows: List<Map<String, Any?>>): Int {
        if (rows.isEmpty()) return 0
        val collection = col(tableName)
        val (withId, withoutId) = rows.partition { it.containsKey("_id") }

        // Upsert: replace existing document with same _id, or insert if absent
        for (row in withId) {
            val id = row["_id"]
            val existing = collection.indexOfFirst { it["_id"] == id }
            if (existing >= 0) {
                collection[existing] = row.toMutableMap()
            } else {
                collection.add(row.toMutableMap())
            }
        }

        // Plain insert for rows without _id
        for (row in withoutId) {
            collection.add(row.toMutableMap())
        }

        return rows.size
    }

    // Test helper: returns a snapshot of the stored collection
    fun snapshot(collectionName: String): List<Map<String, Any?>> = col(collectionName).toList()
}

// ---------------------------------------------------------------------------
// MongoDBMaskingPipelineTest
//
// End-to-end pipeline: source collection → read → apply generators → write
// to target collection, then assert the results.
// ---------------------------------------------------------------------------
class MongoDBMaskingPipelineTest {

    private lateinit var source: InMemoryMongoDBConnector
    private lateinit var target: InMemoryMongoDBConnector
    private lateinit var generatorService: GeneratorService

    // Sample source data ─ a collection of customer documents
    private val sourceCustomers = listOf(
        mapOf("_id" to "c001", "name" to "Alice Smith", "email" to "alice@example.com",
              "phone" to "555-111-2222", "ssn" to "123-45-6789", "account_status" to "active"),
        mapOf("_id" to "c002", "name" to "Bob Jones",  "email" to "bob@example.com",
              "phone" to "555-333-4444", "ssn" to "987-65-4321", "account_status" to "inactive"),
        mapOf("_id" to "c003", "name" to "Carol White","email" to "carol@example.com",
              "phone" to "555-555-6666", "ssn" to "111-22-3333", "account_status" to "active")
    )

    @BeforeEach
    fun setUp() {
        source = InMemoryMongoDBConnector()
        target = InMemoryMongoDBConnector()
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

    // ── 1. Basic masking pipeline ────────────────────────────────────────────

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

        val result = target.snapshot("customers")
        assertEquals(3, result.size)

        result.forEachIndexed { idx, row ->
            val original = sourceCustomers[idx]

            // _id is preserved (not in generator list → passed through)
            assertEquals(original["_id"], row["_id"],
                "Row $idx: _id should be preserved")

            // account_status is not in generators → passed through unchanged
            assertEquals(original["account_status"], row["account_status"],
                "Row $idx: account_status should be passed through")

            // Sensitive columns must be replaced with non-blank values
            assertNotNull(row["name"],  "Row $idx: masked name must not be null")
            assertNotNull(row["email"], "Row $idx: masked email must not be null")
            assertNotNull(row["phone"], "Row $idx: masked phone must not be null")
            assertNotNull(row["ssn"],   "Row $idx: masked ssn must not be null")

            assertTrue((row["name"]  as String).isNotBlank(), "Row $idx: masked name must not be blank")
            assertTrue((row["email"] as String).isNotBlank(), "Row $idx: masked email must not be blank")
            assertTrue((row["phone"] as String).isNotBlank(), "Row $idx: masked phone must not be blank")

            // Masked values must differ from originals (false-positive probability negligible)
            assertNotEquals(original["name"],  row["name"],  "Row $idx: name must be masked")
            assertNotEquals(original["email"], row["email"], "Row $idx: email must be masked")
        }
    }

    // ── 2. Passthrough mode – write source data as-is ───────────────────────

    @Test
    fun `passthrough mode copies all rows unchanged`() {
        val sourceRows = source.fetchData("customers")
        target.writeData("customers_passthrough", sourceRows)

        val result = target.snapshot("customers_passthrough")
        assertEquals(sourceCustomers.size, result.size)

        result.forEachIndexed { idx, row ->
            assertEquals(sourceCustomers[idx]["name"],  row["name"])
            assertEquals(sourceCustomers[idx]["email"], row["email"])
            assertEquals(sourceCustomers[idx]["_id"],   row["_id"])
        }
    }

    // ── 3. Row limit is respected when fetching from source ─────────────────

    @Test
    fun `fetchData respects row limit`() {
        val rows = source.fetchData("customers", limit = 2)
        assertEquals(2, rows.size)
    }

    // ── 4. Upsert path: masked rows with _id replace existing documents ──────

    @Test
    fun `upsert updates existing documents in target preserving _id`() {
        // First pass – write original data to target
        val sourceRows = source.fetchData("customers")
        target.writeData("customers", sourceRows)
        assertEquals(3, target.snapshot("customers").size)

        // Second pass – mask and upsert (should replace, not add)
        val generators = listOf(
            buildColumnGenerator(columnName = "name",  type = GeneratorType.FULL_NAME),
            buildColumnGenerator(columnName = "email", type = GeneratorType.EMAIL)
        )
        val maskedRows = sourceRows.map { row -> generatorService.applyGenerators(row, generators) }
        target.writeData("customers", maskedRows)

        // Still 3 documents (upsert replaced, did not duplicate)
        val result = target.snapshot("customers")
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

    // ── 5. Mixed batch: rows with and without _id ─────────────────────────

    @Test
    fun `writeData handles mixed batch of rows with and without _id`() {
        val mixedRows = listOf(
            mapOf("_id" to "x001", "field" to "upsert-me"),
            mapOf(              "field" to "insert-me")
        )
        val count = target.writeData("mixed", mixedRows)
        assertEquals(2, count)

        val stored = target.snapshot("mixed")
        assertEquals(2, stored.size)
        assertEquals(1, stored.count { it.containsKey("_id") })
        assertEquals(1, stored.count { !it.containsKey("_id") })
    }

    // ── 6. Truncate clears target before re-run ───────────────────────────

    @Test
    fun `truncateTable then writeData yields clean result`() {
        // Write once
        target.writeData("customers", source.fetchData("customers"))
        assertEquals(3, target.snapshot("customers").size)

        // Truncate and re-write a single row
        target.truncateTable("customers")
        assertEquals(0, target.snapshot("customers").size)

        target.writeData("customers", listOf(sourceCustomers.first()))
        assertEquals(1, target.snapshot("customers").size)
    }

    // ── 7. Consistent masking produces identical output for same input ─────

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

    // ── 8. NULL generator nullifies the column ───────────────────────────

    @Test
    fun `NULL generator sets column to null in target`() {
        val generators = listOf(
            buildColumnGenerator(columnName = "ssn", type = GeneratorType.NULL)
        )
        val sourceRows = source.fetchData("customers")
        val maskedRows = sourceRows.map { row -> generatorService.applyGenerators(row, generators) }
        target.writeData("customers", maskedRows)

        val result = target.snapshot("customers")
        result.forEach { row ->
            assertNull(row["ssn"], "SSN should be nullified")
            // Other fields remain
            assertNotNull(row["name"])
            assertNotNull(row["email"])
        }
    }

    // ── 9. Empty source collection writes zero rows ───────────────────────

    @Test
    fun `empty source collection results in zero rows written to target`() {
        val empty = source.fetchData("nonexistent_collection")
        assertEquals(0, empty.size)

        val written = target.writeData("output", empty)
        assertEquals(0, written)
        assertEquals(0, target.snapshot("output").size)
    }
}
