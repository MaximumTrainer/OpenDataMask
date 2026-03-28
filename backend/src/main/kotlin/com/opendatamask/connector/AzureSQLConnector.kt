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

    override fun fetchData(tableName: String, limit: Int?): List<Map<String, Any?>> {
        // Use square-bracket quoting for SQL Server identifier safety; strip brackets from input
        val sanitizedTable = tableName.replace("[", "").replace("]", "")
        val query = if (limit != null) {
            "SELECT TOP $limit * FROM [$sanitizedTable]"
        } else {
            "SELECT * FROM [$sanitizedTable]"
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
