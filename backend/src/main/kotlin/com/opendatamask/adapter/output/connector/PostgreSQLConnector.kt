package com.opendatamask.adapter.output.connector

import java.sql.DriverManager

class PostgreSQLConnector(
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
            val rs = meta.getTables(null, "public", "%", arrayOf("TABLE"))
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
        // Use double-quoting for PostgreSQL identifier safety
        val quotedTable = "\"${tableName.replace("\"", "")}\""
        val wherePart = if (!whereClause.isNullOrBlank()) " WHERE $whereClause" else ""
        val query = if (limit != null) {
            "SELECT * FROM $quotedTable$wherePart LIMIT $limit"
        } else {
            "SELECT * FROM $quotedTable$wherePart"
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
        val quotedTable = "\"${tableName.replace("\"", "")}\""
        val colDefs = columns.joinToString(", ") { col ->
            val quotedCol = "\"${col.name.replace("\"", "")}\""
            val nullConstraint = if (col.nullable) "" else " NOT NULL"
            "$quotedCol ${col.type}$nullConstraint"
        }
        val sql = "CREATE TABLE IF NOT EXISTS $quotedTable ($colDefs)"
        getConnection().use { conn -> conn.createStatement().use { it.execute(sql) } }
    }

    override fun truncateTable(tableName: String) {
        val quotedTable = "\"${tableName.replace("\"", "")}\""
        getConnection().use { conn -> conn.createStatement().use { it.execute("DELETE FROM $quotedTable") } }
    }

    override fun writeData(tableName: String, rows: List<Map<String, Any?>>): Int {
        if (rows.isEmpty()) return 0
        val quotedTable = "\"${tableName.replace("\"", "")}\""
        val columns = rows.first().keys.toList()
        val quotedCols = columns.joinToString(", ") { "\"${it.replace("\"", "")}\"" }
        val placeholders = columns.joinToString(", ") { "?" }
        val sql = "INSERT INTO $quotedTable ($quotedCols) VALUES ($placeholders)"
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

    private fun getConnection() = DriverManager.getConnection(connectionString, username, password)
}
