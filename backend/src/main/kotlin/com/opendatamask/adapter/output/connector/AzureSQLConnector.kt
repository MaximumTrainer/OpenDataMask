package com.opendatamask.adapter.output.connector

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
            // Use null schema to cover dbo and any other user schemas; exclude INFORMATION_SCHEMA
            val rs = meta.getTables(null, null, "%", arrayOf("TABLE"))
            while (rs.next()) {
                val schema = rs.getString("TABLE_SCHEM") ?: ""
                if (!schema.equals("INFORMATION_SCHEMA", ignoreCase = true)) {
                    tables.add(rs.getString("TABLE_NAME"))
                }
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

    override fun fetchData(tableName: String, limit: Int?, whereClause: String?, selectedAttributes: List<String>?): List<Map<String, Any?>> {
        // Use square-bracket quoting for SQL Server identifier safety; strip brackets from input
        val sanitizedTable = tableName.replace("[", "").replace("]", "")
        val selectPart = if (!selectedAttributes.isNullOrEmpty()) {
            selectedAttributes.joinToString(", ") { col ->
                "[${col.replace("[", "").replace("]", "")}]"
            }
        } else {
            "*"
        }
        val wherePart = if (!whereClause.isNullOrBlank()) " WHERE $whereClause" else ""
        val query = if (limit != null) {
            "SELECT TOP $limit $selectPart FROM [$sanitizedTable]$wherePart"
        } else {
            "SELECT $selectPart FROM [$sanitizedTable]$wherePart"
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
        val sql = "CREATE TABLE [$sanitizedTable] ($colDefs)"
        try {
            getConnection().use { conn -> conn.createStatement().use { it.execute(sql) } }
        } catch (e: java.sql.SQLException) {
            // Tolerate "table already exists": H2 raises state 42S01, SQL Server error code 2714
            val alreadyExists = e.sqlState?.startsWith("42S01") == true ||
                e.errorCode == 2714 ||
                e.message?.contains("already exists", ignoreCase = true) == true
            if (!alreadyExists) throw e
        }
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

    override fun listForeignKeys(tableName: String): List<ForeignKeyInfo> {
        return getConnection().use { conn ->
            val fks = mutableListOf<ForeignKeyInfo>()
            val rs = conn.metaData.getImportedKeys(null, null, tableName)
            while (rs.next()) {
                fks.add(
                    ForeignKeyInfo(
                        fromTable = rs.getString("FKTABLE_NAME"),
                        fromColumn = rs.getString("FKCOLUMN_NAME"),
                        toTable = rs.getString("PKTABLE_NAME"),
                        toColumn = rs.getString("PKCOLUMN_NAME")
                    )
                )
            }
            fks
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
