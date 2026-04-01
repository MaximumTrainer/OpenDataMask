package com.opendatamask.connector

import java.sql.DriverManager

class MySQLConnector(
    private val connectionString: String,
    private val username: String?,
    private val password: String?,
    private val database: String?
) : DatabaseConnector {

    private fun getConnection() = DriverManager.getConnection(connectionString, username, password)

    override fun testConnection(): Boolean = try {
        getConnection().use { it.isValid(5) }
    } catch (e: Exception) {
        false
    }

    override fun listTables(): List<String> {
        getConnection().use { conn ->
            val db = database ?: conn.catalog ?: return emptyList()
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SHOW TABLES FROM `$db`")
                val tables = mutableListOf<String>()
                while (rs.next()) tables.add(rs.getString(1))
                return tables
            }
        }
    }

    override fun listColumns(tableName: String): List<ColumnInfo> {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SHOW COLUMNS FROM `$tableName`")
                val columns = mutableListOf<ColumnInfo>()
                while (rs.next()) {
                    columns.add(
                        ColumnInfo(
                            name = rs.getString("Field"),
                            type = rs.getString("Type"),
                            nullable = rs.getString("Null") == "YES"
                        )
                    )
                }
                return columns
            }
        }
    }

    override fun fetchData(tableName: String, limit: Int?, whereClause: String?): List<Map<String, Any?>> {
        getConnection().use { conn ->
            val sql = buildString {
                append("SELECT * FROM `$tableName`")
                if (whereClause != null) append(" WHERE $whereClause")
                if (limit != null) append(" LIMIT $limit")
            }
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery(sql)
                val meta = rs.metaData
                val columns = (1..meta.columnCount).map { meta.getColumnName(it) }
                val rows = mutableListOf<Map<String, Any?>>()
                while (rs.next()) {
                    rows.add(columns.associateWith { col -> rs.getObject(col) })
                }
                return rows
            }
        }
    }

    override fun createTable(tableName: String, columns: List<ColumnInfo>) {
        if (columns.isEmpty()) return
        getConnection().use { conn ->
            val colDefs = columns.joinToString(", ") { col ->
                val nullable = if (col.nullable) "" else " NOT NULL"
                "`${col.name}` ${col.type}$nullable"
            }
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE IF NOT EXISTS `$tableName` ($colDefs)")
            }
        }
    }

    override fun truncateTable(tableName: String) {
        getConnection().use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("TRUNCATE TABLE `$tableName`")
            }
        }
    }

    override fun writeData(tableName: String, rows: List<Map<String, Any?>>): Int {
        if (rows.isEmpty()) return 0
        getConnection().use { conn ->
            val columns = rows.first().keys.toList()
            val placeholders = columns.joinToString(", ") { "?" }
            val colNames = columns.joinToString(", ") { "`$it`" }
            val sql = "INSERT INTO `$tableName` ($colNames) VALUES ($placeholders)"
            conn.prepareStatement(sql).use { ps ->
                for (row in rows) {
                    columns.forEachIndexed { i, col -> ps.setObject(i + 1, row[col]) }
                    ps.addBatch()
                }
                val counts = ps.executeBatch()
                return counts.sum()
            }
        }
    }
}
