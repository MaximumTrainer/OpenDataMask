package com.opendatamask.adapter.output.connector

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class AzureSQLConnectorTest {

    // Use H2 in MSSQLServer compatibility mode with null credentials so
    // AzureSQLConnector takes the simple DriverManager.getConnection(url) path.
    private val h2Url =
        "jdbc:h2:mem:aztest_${System.nanoTime()};DB_CLOSE_DELAY=-1;MODE=MSSQLServer;DATABASE_TO_UPPER=FALSE"
    private lateinit var connector: AzureSQLConnector

    @BeforeEach
    fun setup() {
        connector = AzureSQLConnector(h2Url, null, null)
        java.sql.DriverManager.getConnection(h2Url).use { conn ->
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
    fun `fetchData returns all rows without limit or filter`() {
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
    fun `truncateTable removes all rows`() {
        connector.truncateTable("test_users")
        val rows = connector.fetchData("test_users")
        assertTrue(rows.isEmpty())
    }

    @Test
    fun `listTables returns at least the test_users table`() {
        val tables = connector.listTables()
        assertTrue(tables.any { it.equals("test_users", ignoreCase = true) },
            "Expected test_users in $tables")
        assertTrue(tables.none { it.equals("INFORMATION_SCHEMA", ignoreCase = true) },
            "INFORMATION_SCHEMA should not appear as a table name in $tables")
    }

    @Test
    fun `listColumns returns columns for test_users`() {
        val columns = connector.listColumns("test_users")
        assertTrue(columns.isNotEmpty(), "Expected columns for test_users but got empty list")
        val names = columns.map { it.name.lowercase() }
        assertTrue(names.contains("id"), "Expected column 'id' in $names")
        assertTrue(names.contains("name"), "Expected column 'name' in $names")
        assertTrue(names.contains("email"), "Expected column 'email' in $names")
    }

    @Test
    fun `createTable creates a new table that can be written to`() {
        val columns = listOf(
            ColumnInfo("product_id", "INT", false),
            ColumnInfo("product_name", "VARCHAR(100)", true)
        )
        connector.createTable("az_products", columns)
        val rows = listOf(mapOf("product_id" to 1, "product_name" to "Widget"))
        val count = connector.writeData("az_products", rows)
        assertEquals(1, count)
    }

    @Test
    fun `createTable is idempotent when called twice with the same table name`() {
        val columns = listOf(ColumnInfo("item_id", "INT", false))
        connector.createTable("az_items", columns)
        // Second call must not throw even though the table already exists
        assertDoesNotThrow { connector.createTable("az_items", columns) }
    }

    @Test
    fun `listForeignKeys returns empty list when no FK constraints exist`() {
        val fks = connector.listForeignKeys("test_users")
        assertNotNull(fks)
        assertTrue(fks.isEmpty(), "Expected empty FK list for standalone test_users table")
    }

    @Test
    fun `listForeignKeys returns foreign key when FK constraint is defined`() {
        java.sql.DriverManager.getConnection(h2Url).use { conn ->
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS az_departments (dept_id INT PRIMARY KEY, dept_name VARCHAR(100))"
            )
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS az_employees (" +
                    "emp_id INT PRIMARY KEY, emp_name VARCHAR(100), dept_id INT, " +
                    "CONSTRAINT fk_az_dept FOREIGN KEY (dept_id) REFERENCES az_departments(dept_id)" +
                    ")"
            )
        }
        val fks = connector.listForeignKeys("az_employees")
        assertEquals(1, fks.size, "Expected 1 FK for az_employees but got ${fks.size}: $fks")
        val fk = fks[0]
        assertEquals("az_employees", fk.fromTable.lowercase())
        assertEquals("dept_id", fk.fromColumn.lowercase())
        assertEquals("az_departments", fk.toTable.lowercase())
        assertEquals("dept_id", fk.toColumn.lowercase())
    }
}
