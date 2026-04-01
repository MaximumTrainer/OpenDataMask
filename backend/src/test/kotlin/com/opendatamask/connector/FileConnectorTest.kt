package com.opendatamask.connector

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class FileConnectorTest {

    private fun createCsvConnector(csvContent: String): FileConnector {
        return FileConnector(csvContent.toByteArray(), "test.csv", "text/csv")
    }

    private fun createJsonConnector(jsonContent: String): FileConnector {
        return FileConnector(jsonContent.toByteArray(), "test.json", "application/json")
    }

    @Test
    fun `testConnection always returns true`() {
        val connector = createCsvConnector("id,name\n1,Alice")
        assertTrue(connector.testConnection())
    }

    @Test
    fun `listTables returns filename without extension`() {
        val connector = createCsvConnector("id,name\n1,Alice")
        val tables = connector.listTables()
        assertEquals(1, tables.size)
        assertEquals("test", tables[0])
    }

    @Test
    fun `fetchData parses CSV correctly`() {
        val csv = "id,name,email\n1,Alice,alice@example.com\n2,Bob,bob@example.com"
        val connector = createCsvConnector(csv)
        val rows = connector.fetchData("test")
        assertEquals(2, rows.size)
        assertEquals("Alice", rows[0]["name"])
        assertEquals("bob@example.com", rows[1]["email"])
    }

    @Test
    fun `fetchData respects row limit`() {
        val csv = "id,name\n1,Alice\n2,Bob\n3,Charlie"
        val connector = createCsvConnector(csv)
        val rows = connector.fetchData("test", limit = 2)
        assertEquals(2, rows.size)
    }

    @Test
    fun `fetchData parses JSON array correctly`() {
        val json = """[{"id":1,"name":"Alice"},{"id":2,"name":"Bob"}]"""
        val connector = createJsonConnector(json)
        val rows = connector.fetchData("test")
        assertEquals(2, rows.size)
        assertEquals("Alice", rows[0]["name"])
    }

    @Test
    fun `listColumns returns column names from CSV header`() {
        val csv = "id,name,email\n1,Alice,alice@example.com"
        val connector = createCsvConnector(csv)
        val columns = connector.listColumns("test")
        assertEquals(3, columns.size)
        assertTrue(columns.any { it.name == "id" })
        assertTrue(columns.any { it.name == "name" })
        assertTrue(columns.any { it.name == "email" })
    }

    @Test
    fun `writeData serializes to CSV and returns count`() {
        val connector = createCsvConnector("id,name\n1,Alice")
        val rows = listOf(
            mapOf("id" to "1", "name" to "Alice"),
            mapOf("id" to "2", "name" to "Bob")
        )
        val count = connector.writeData("output", rows)
        assertEquals(2, count)
        val output = connector.getOutputBytes()
        assertNotNull(output)
        assertTrue(output!!.isNotEmpty())
    }

    @Test
    fun `createTable is a no-op for file connectors`() {
        val connector = createCsvConnector("id,name\n1,Alice")
        // Should not throw
        connector.createTable("output", listOf(ColumnInfo("id", "VARCHAR")))
    }

    @Test
    fun `truncateTable clears output buffer`() {
        val connector = createCsvConnector("id,name\n1,Alice")
        connector.writeData("output", listOf(mapOf("id" to "1", "name" to "Alice")))
        connector.truncateTable("output")
        val output = connector.getOutputBytes()
        assertTrue(output == null || output.isEmpty())
    }
}
