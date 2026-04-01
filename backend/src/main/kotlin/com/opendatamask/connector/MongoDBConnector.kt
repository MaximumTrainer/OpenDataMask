package com.opendatamask.connector

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.bson.Document

open class MongoDBConnector(
    protected val connectionString: String,
    protected val database: String?
) : DatabaseConnector {

    protected open fun createMongoClient(): MongoClient {
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(connectionString))
            .build()
        return MongoClients.create(settings)
    }

    protected fun getDatabaseName(): String {
        return database ?: ConnectionString(connectionString).database
            ?: throw IllegalStateException("No database specified")
    }

    override fun testConnection(): Boolean {
        return try {
            createMongoClient().use { client ->
                client.getDatabase(getDatabaseName()).runCommand(Document("ping", 1))
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun listTables(): List<String> {
        return createMongoClient().use { client ->
            client.getDatabase(getDatabaseName()).listCollectionNames().toList()
        }
    }

    override fun listColumns(tableName: String): List<ColumnInfo> {
        return createMongoClient().use { client ->
            val collection = client.getDatabase(getDatabaseName()).getCollection(tableName)
            val sample = collection.find().limit(1).firstOrNull()
            sample?.keys?.map { ColumnInfo(name = it, type = "mixed", nullable = true) } ?: emptyList()
        }
    }

    override fun fetchData(tableName: String, limit: Int?, whereClause: String?): List<Map<String, Any?>> {
        // whereClause is expected to be a MongoDB query filter as JSON, e.g. {"age": {"$gt": 18}}
        return createMongoClient().use { client ->
            val collection = client.getDatabase(getDatabaseName()).getCollection(tableName)
            val filter = if (!whereClause.isNullOrBlank()) Document.parse(whereClause) else Document()
            val cursor = if (limit != null) collection.find(filter).limit(limit) else collection.find(filter)
            cursor.map { doc ->
                doc.entries.associate { it.key to it.value }
            }.toList()
        }
    }

    override fun createTable(tableName: String, columns: List<ColumnInfo>) {
        createMongoClient().use { client ->
            val db = client.getDatabase(getDatabaseName())
            try {
                db.createCollection(tableName)
            } catch (e: Exception) {
                // Collection already exists — no action needed
            }
        }
    }

    override fun truncateTable(tableName: String) {
        createMongoClient().use { client ->
            client.getDatabase(getDatabaseName()).getCollection(tableName).deleteMany(Document())
        }
    }

    override fun writeData(tableName: String, rows: List<Map<String, Any?>>): Int {
        if (rows.isEmpty()) return 0
        createMongoClient().use { client ->
            val collection = client.getDatabase(getDatabaseName()).getCollection(tableName)
            collection.insertMany(rows.map { Document(it) })
        }
        return rows.size
    }
}
