package com.opendatamask.adapter.output.connector

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PostgreSQLConnectorTest {

    private lateinit var connector: PostgreSQLConnector
    private val h2Url = "jdbc:h2:mem:pgtest_${System.nanoTime()};DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_UPPER=FALSE"

    @BeforeEach
    fun setup() {
        connector = PostgreSQLConnector(h2Url, "sa", "")
        java.sql.DriverManager.getConnection(h2Url, "sa", "").use { conn ->
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS test_users (id INT, name VARCHAR(100), email VARCHAR(200))"
            )
            conn.createStatement().execute("DELETE FROM test_users")
            conn.createStatement().execute("INSERT INTO test_users VALUES (1, 'Alice', 'alice@example.com')")
            conn.createStatement().execute("INSERT INTO test_users VALUES (2, 'Bob', 'bob@example.com')")
        }
    }

    @Test
    fun `testConnection returns true for valid connection`() {
        assertTrue(connector.testConnection())
    }

    @Test
    fun `fetchData returns all rows without limit`() {
        val rows = connector.fetchData("test_users")
        assertEquals(2, rows.size)
    }

    @Test
    fun `fetchData respects row limit`() {
        val rows = connector.fetchData("test_users", limit = 1)
        assertEquals(1, rows.size)
    }

    @Test
    fun `fetchData with whereClause filters rows`() {
        val rows = connector.fetchData("test_users", whereClause = "name = 'Alice'")
        assertEquals(1, rows.size)
        val name = rows[0]["NAME"] ?: rows[0]["name"]
        assertEquals("Alice", name)
    }

    @Test
    fun `writeData inserts rows and returns count`() {
        val rows = listOf(
            mapOf("id" to 3, "name" to "Charlie", "email" to "charlie@example.com")
        )
        val count = connector.writeData("test_users", rows)
        assertEquals(1, count)
        val all = connector.fetchData("test_users")
        assertEquals(3, all.size)
    }

    @Test
    fun `writeData returns 0 for empty list`() {
        assertEquals(0, connector.writeData("test_users", emptyList()))
    }

    @Test
    fun `createTable creates a new table`() {
        val columns = listOf(
            ColumnInfo("product_id", "INT", false),
            ColumnInfo("product_name", "VARCHAR(100)", true)
        )
        connector.createTable("products", columns)
        val rows = listOf(mapOf("product_id" to 1, "product_name" to "Widget"))
        val count = connector.writeData("products", rows)
        assertEquals(1, count)
    }

    @Test
    fun `truncateTable removes all rows`() {
        connector.truncateTable("test_users")
        val rows = connector.fetchData("test_users")
        assertTrue(rows.isEmpty())
    }
}
