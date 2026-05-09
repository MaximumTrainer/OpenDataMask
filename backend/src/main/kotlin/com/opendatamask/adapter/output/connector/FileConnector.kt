package com.opendatamask.adapter.output.connector

class FileConnector(
    private val sourceBytes: ByteArray,
    private val filename: String,
    private val contentType: String
) : DatabaseConnector {

    private val tableName = filename.substringBeforeLast(".")
    private var outputBytes: ByteArray? = null

    override fun testConnection(): Boolean = true

    override fun listTables(): List<String> = listOf(tableName)

    override fun listColumns(tableName: String): List<ColumnInfo> {
        return when {
            contentType.contains("csv", ignoreCase = true) || filename.endsWith(".csv") -> parseCsvColumns()
            contentType.contains("json", ignoreCase = true) || filename.endsWith(".json") -> parseJsonColumns()
            else -> emptyList()
        }
    }

    override fun fetchData(tableName: String, limit: Int?, whereClause: String?, selectedAttributes: List<String>?): List<Map<String, Any?>> {
        val rows = when {
            contentType.contains("csv", ignoreCase = true) || filename.endsWith(".csv") -> parseCsv()
            contentType.contains("json", ignoreCase = true) || filename.endsWith(".json") -> parseJson()
            else -> emptyList()
        }
        val limited = if (limit != null) rows.take(limit) else rows
        return if (!selectedAttributes.isNullOrEmpty()) {
            val normalizedSelected = selectedAttributes.map { it.lowercase() }.toSet()
            limited.map { row ->
                row.filterKeys { it.lowercase() in normalizedSelected }
            }
        } else {
            limited
        }
    }

    override fun createTable(tableName: String, columns: List<ColumnInfo>) {
        // No-op for file connectors — output format is determined at write time
    }

    override fun truncateTable(tableName: String) {
        outputBytes = null
    }

    override fun writeData(tableName: String, rows: List<Map<String, Any?>>): Int {
        if (rows.isEmpty()) {
            outputBytes = ByteArray(0)
            return 0
        }
        outputBytes = when {
            contentType.contains("csv", ignoreCase = true) || filename.endsWith(".csv") -> writeCsv(rows)
            contentType.contains("json", ignoreCase = true) || filename.endsWith(".json") -> writeJson(rows)
            else -> writeCsv(rows)
        }
        return rows.size
    }

    fun getOutputBytes(): ByteArray? = outputBytes

    private fun parseCsvColumns(): List<ColumnInfo> {
        val text = String(sourceBytes, Charsets.UTF_8)
        val header = text.lineSequence().firstOrNull()?.let { parseCsvLine(it) } ?: return emptyList()
        return header.map { ColumnInfo(name = it.trim(), type = "VARCHAR", nullable = true) }
    }

    private fun parseCsv(): List<Map<String, Any?>> {
        val text = String(sourceBytes, Charsets.UTF_8)
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val headers = parseCsvLine(lines[0]).map { it.trim() }
        return lines.drop(1).map { line ->
            val values = parseCsvLine(line)
            headers.mapIndexed { i, h -> h to (values.getOrNull(i)?.trimQuotes() as Any?) }.toMap()
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' && !inQuotes -> inQuotes = true
                ch == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                ch == '"' && inQuotes -> inQuotes = false
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun String.trimQuotes(): String = if (startsWith('"') && endsWith('"')) substring(1, length - 1) else this

    private fun parseJsonColumns(): List<ColumnInfo> {
        val rows = parseJson()
        return rows.firstOrNull()?.keys?.map { ColumnInfo(name = it, type = "mixed", nullable = true) } ?: emptyList()
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJson(): List<Map<String, Any?>> {
        val text = String(sourceBytes, Charsets.UTF_8).trim()
        return when {
            text.startsWith("[") -> {
                com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                    .readValue(text, List::class.java) as List<Map<String, Any?>>
            }
            text.startsWith("{") -> {
                val map = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
                    .readValue(text, Map::class.java) as Map<String, Any?>
                listOf(map)
            }
            else -> emptyList()
        }
    }

    private fun writeCsv(rows: List<Map<String, Any?>>): ByteArray {
        if (rows.isEmpty()) return ByteArray(0)
        val sb = StringBuilder()
        val headers = rows.first().keys.toList()
        sb.appendLine(headers.joinToString(","))
        rows.forEach { row ->
            sb.appendLine(headers.joinToString(",") { k ->
                val v = row[k]?.toString() ?: ""
                if (v.contains(",")) "\"$v\"" else v
            })
        }
        return sb.toString().toByteArray(Charsets.UTF_8)
    }

    private fun writeJson(rows: List<Map<String, Any?>>): ByteArray {
        return com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
            .writeValueAsBytes(rows)
    }
}
