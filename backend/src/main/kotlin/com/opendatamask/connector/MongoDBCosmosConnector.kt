package com.opendatamask.connector

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import java.util.concurrent.TimeUnit

class MongoDBCosmosConnector(
    connectionString: String,
    database: String?
) : MongoDBConnector(connectionString, database) {

    override fun createMongoClient(): MongoClient {
        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(connectionString))
            .applyToConnectionPoolSettings { builder ->
                builder.minSize(0)
                    .maxSize(10)
                    .maxWaitTime(30, TimeUnit.SECONDS)
            }
            .applyToSocketSettings { builder ->
                builder.connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
            }
            .applyToClusterSettings { builder ->
                builder.serverSelectionTimeout(30, TimeUnit.SECONDS)
            }
            .retryWrites(false)
            .build()
        return MongoClients.create(settings)
    }
}
