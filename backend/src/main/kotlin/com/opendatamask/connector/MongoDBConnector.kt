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

    override fun fetchData(tableName: String, limit: Int?): List<Map<String, Any?>> {
        return createMongoClient().use { client ->
            val collection = client.getDatabase(getDatabaseName()).getCollection(tableName)
            val cursor = if (limit != null) collection.find().limit(limit) else collection.find()
            cursor.map { doc ->
                doc.entries.associate { it.key to it.value }
            }.toList()
        }
    }
}
