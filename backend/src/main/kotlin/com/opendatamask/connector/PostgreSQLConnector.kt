package com.opendatamask.connector

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

    override fun fetchData(tableName: String, limit: Int?): List<Map<String, Any?>> {
        // Use double-quoting for PostgreSQL identifier safety
        val quotedTable = "\"${tableName.replace("\"", "")}\""
        val query = if (limit != null) {
            "SELECT * FROM $quotedTable LIMIT $limit"
        } else {
            "SELECT * FROM $quotedTable"
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

    private fun getConnection() = DriverManager.getConnection(connectionString, username, password)
}
