package com.opendatamask.connector

import com.opendatamask.model.ConnectionType
import com.opendatamask.service.FileStorageService
import org.springframework.stereotype.Component

@Component
class ConnectorFactory(
    private val fileStorageService: FileStorageService? = null
) {

    fun createConnector(
        type: ConnectionType,
        connectionString: String,
        username: String? = null,
        password: String? = null,
        database: String? = null
    ): DatabaseConnector {
        return when (type) {
            ConnectionType.POSTGRESQL -> PostgreSQLConnector(
                connectionString = connectionString,
                username = username,
                password = password
            )
            ConnectionType.MONGODB -> MongoDBConnector(
                connectionString = connectionString,
                database = database
            )
            ConnectionType.AZURE_SQL -> AzureSQLConnector(
                connectionString = connectionString,
                username = username,
                password = password
            )
            ConnectionType.MONGODB_COSMOS -> MongoDBCosmosConnector(
                connectionString = connectionString,
                database = database
            )
            ConnectionType.FILE -> {
                val fileId = connectionString.toLongOrNull()
                    ?: throw IllegalArgumentException("FILE connection requires a numeric file ID in connectionString")
                val retrieved = fileStorageService?.retrieveFile(fileId)
                    ?: throw IllegalStateException("FileStorageService not available for FILE connector")
                FileConnector(retrieved.content, retrieved.filename, retrieved.contentType)
            }
        }
    }
}
