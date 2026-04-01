package com.opendatamask.connector

import java.sql.DriverManager
import java.util.Properties

class AzureSQLConnector(
    private val connectionString: String,
    private val username: String?,
    private val password: String?
) : DatabaseConnector {

    override fun testConnection(): Boolean {
        return try {
            getConnection().use { true }
        } catch (e: Exception) {
            false
        }
    }

    override fun listTables(): List<String> {
        return getConnection().use { conn ->
            val tables = mutableListOf<String>()
            val meta = conn.metaData
            val rs = meta.getTables(null, "dbo", "%", arrayOf("TABLE"))
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"))
            }
            tables
        }
    }

    override fun listColumns(tableName: String): List<ColumnInfo> {
        return getConnection().use { conn ->
            val columns = mutableListOf<ColumnInfo>()
            val meta = conn.metaData
            val rs = meta.getColumns(null, null, tableName, null)
            while (rs.next()) {
                columns.add(
                    ColumnInfo(
                        name = rs.getString("COLUMN_NAME"),
                        type = rs.getString("TYPE_NAME"),
                        nullable = rs.getInt("NULLABLE") == java.sql.DatabaseMetaData.columnNullable
                    )
                )
            }
            columns
        }
    }

    override fun fetchData(tableName: String, limit: Int?, whereClause: String?): List<Map<String, Any?>> {
        // Use square-bracket quoting for SQL Server identifier safety; strip brackets from input
        val sanitizedTable = tableName.replace("[", "").replace("]", "")
        val wherePart = if (!whereClause.isNullOrBlank()) " WHERE $whereClause" else ""
        val query = if (limit != null) {
            "SELECT TOP $limit * FROM [$sanitizedTable]$wherePart"
        } else {
            "SELECT * FROM [$sanitizedTable]$wherePart"
        }
        return getConnection().use { conn ->
            conn.prepareStatement(query).use { stmt ->
                val rs = stmt.executeQuery()
                val meta = rs.metaData
                val colCount = meta.columnCount
                val rows = mutableListOf<Map<String, Any?>>()
                while (rs.next()) {
                    val row = mutableMapOf<String, Any?>()
                    for (i in 1..colCount) {
                        row[meta.getColumnName(i)] = rs.getObject(i)
                    }
                    rows.add(row)
                }
                rows
            }
        }
    }

    override fun createTable(tableName: String, columns: List<ColumnInfo>) {
        val sanitizedTable = tableName.replace("[", "").replace("]", "")
        val colDefs = columns.joinToString(", ") { col ->
            val sanitizedCol = col.name.replace("[", "").replace("]", "")
            val nullConstraint = if (col.nullable) "" else " NOT NULL"
            "[$sanitizedCol] ${col.type}$nullConstraint"
        }
        val sql = "IF OBJECT_ID('[$sanitizedTable]', 'U') IS NULL CREATE TABLE [$sanitizedTable] ($colDefs)"
        getConnection().use { conn -> conn.createStatement().use { it.execute(sql) } }
    }

    override fun truncateTable(tableName: String) {
        val sanitizedTable = tableName.replace("[", "").replace("]", "")
        getConnection().use { conn -> conn.createStatement().use { it.execute("DELETE FROM [$sanitizedTable]") } }
    }

    override fun writeData(tableName: String, rows: List<Map<String, Any?>>): Int {
        if (rows.isEmpty()) return 0
        val sanitizedTable = tableName.replace("[", "").replace("]", "")
        val columns = rows.first().keys.toList()
        val quotedCols = columns.joinToString(", ") { col ->
            "[${col.replace("[", "").replace("]", "")}]"
        }
        val placeholders = columns.joinToString(", ") { "?" }
        val sql = "INSERT INTO [$sanitizedTable] ($quotedCols) VALUES ($placeholders)"
        return getConnection().use { conn ->
            conn.prepareStatement(sql).use { stmt ->
                for (row in rows) {
                    columns.forEachIndexed { idx, col -> stmt.setObject(idx + 1, row[col]) }
                    stmt.addBatch()
                }
                stmt.executeBatch().sum()
            }
        }
    }

    private fun getConnection() = if (username != null && password != null) {
        val props = Properties().apply {
            setProperty("user", username)
            setProperty("password", password)
            setProperty("encrypt", "true")
            setProperty("trustServerCertificate", "false")
            setProperty("hostNameInCertificate", "*.database.windows.net")
            setProperty("loginTimeout", "30")
        }
        DriverManager.getConnection(connectionString, props)
    } else {
        DriverManager.getConnection(connectionString)
    }
}
