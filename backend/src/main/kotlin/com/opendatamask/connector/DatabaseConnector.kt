package com.opendatamask.connector

data class ColumnInfo(
    val name: String,
    val type: String,
    val nullable: Boolean = true
)

data class ForeignKeyInfo(
    val fromTable: String,
    val fromColumn: String,
    val toTable: String,
    val toColumn: String
)

interface DatabaseConnector {
    fun testConnection(): Boolean
    fun listTables(): List<String>
    fun listColumns(tableName: String): List<ColumnInfo>
    fun fetchData(tableName: String, limit: Int? = null, whereClause: String? = null): List<Map<String, Any?>>
    fun createTable(tableName: String, columns: List<ColumnInfo>)
    fun truncateTable(tableName: String)
    fun writeData(tableName: String, rows: List<Map<String, Any?>>): Int
    fun listForeignKeys(tableName: String): List<ForeignKeyInfo> = emptyList()
}
