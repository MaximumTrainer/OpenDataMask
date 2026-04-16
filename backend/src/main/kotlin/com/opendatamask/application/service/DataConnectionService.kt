package com.opendatamask.application.service

import com.opendatamask.domain.port.input.DataConnectionUseCase

import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.domain.port.output.ConnectorFactoryPort
import com.opendatamask.domain.port.output.DataConnectionPort
import com.opendatamask.domain.port.input.dto.ConnectionTestResult
import com.opendatamask.domain.port.input.dto.DataConnectionRequest
import com.opendatamask.domain.port.input.dto.DataConnectionResponse
import com.opendatamask.domain.model.ConnectionType
import com.opendatamask.domain.model.DataConnection
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DataConnectionService(
    private val dataConnectionRepository: DataConnectionPort,
    private val encryptionPort: EncryptionPort,
    private val connectorFactory: ConnectorFactoryPort
) : DataConnectionUseCase {

    @Transactional
    override fun createConnection(workspaceId: Long, request: DataConnectionRequest): DataConnectionResponse {
        val connStr = request.connectionString
        if (connStr.isNullOrBlank()) {
            throw IllegalArgumentException("Connection string is required when creating a connection")
        }
        val connection = DataConnection(
            workspaceId = workspaceId,
            name = request.name,
            type = request.type,
            connectionString = encryptionPort.encrypt(connStr),
            host = extractHost(request.type, connStr),
            username = request.username,
            password = request.password?.let { encryptionPort.encrypt(it) },
            database = request.database,
            isSource = request.isSource,
            isDestination = request.isDestination
        )
        return dataConnectionRepository.save(connection).toResponse()
    }

    @Transactional(readOnly = true)
    override fun getConnection(workspaceId: Long, connectionId: Long): DataConnectionResponse {
        val connection = findConnection(workspaceId, connectionId)
        return connection.toResponse()
    }

    @Transactional(readOnly = true)
    override fun listConnections(workspaceId: Long): List<DataConnectionResponse> {
        return dataConnectionRepository.findByWorkspaceId(workspaceId).map { it.toResponse() }
    }

    @Transactional
    override fun updateConnection(workspaceId: Long, connectionId: Long, request: DataConnectionRequest): DataConnectionResponse {
        val connection = findConnection(workspaceId, connectionId)
        // If the connector type is changing, a new connection string must be provided because the
        // existing stored string is for the old type and would be invalid for the new one.
        if (request.type != connection.type && request.connectionString.isNullOrBlank()) {
            throw IllegalArgumentException(
                "A new connection string is required when changing the connection type"
            )
        }
        connection.name = request.name
        connection.type = request.type
        if (!request.connectionString.isNullOrBlank()) {
            connection.connectionString = encryptionPort.encrypt(request.connectionString)
            connection.host = extractHost(request.type, request.connectionString)
        }
        connection.username = request.username
        if (!request.password.isNullOrBlank()) {
            connection.password = encryptionPort.encrypt(request.password)
        }
        connection.database = request.database
        connection.isSource = request.isSource
        connection.isDestination = request.isDestination
        return dataConnectionRepository.save(connection).toResponse()
    }

    @Transactional
    override fun deleteConnection(workspaceId: Long, connectionId: Long) {
        val connection = findConnection(workspaceId, connectionId)
        dataConnectionRepository.deleteById(connection.id!!)
    }

    override fun testConnection(workspaceId: Long, connectionId: Long): ConnectionTestResult {
        val connection = findConnection(workspaceId, connectionId)
        val decryptedConnectionString = encryptionPort.decrypt(connection.connectionString)
        val decryptedPassword = connection.password?.let { encryptionPort.decrypt(it) }

        return try {
            val connector = connectorFactory.createConnector(
                type = connection.type,
                connectionString = decryptedConnectionString,
                username = connection.username,
                password = decryptedPassword,
                database = connection.database
            )
            val success = connector.testConnection()
            if (success) {
                ConnectionTestResult(success = true, message = "Connection successful")
            } else {
                ConnectionTestResult(success = false, message = "Connection failed")
            }
        } catch (e: Exception) {
            ConnectionTestResult(success = false, message = "Connection failed: ${e.message}")
        }
    }

    private fun findConnection(workspaceId: Long, connectionId: Long): DataConnection {
        val connection = dataConnectionRepository.findById(connectionId)
            .orElseThrow { NoSuchElementException("Connection not found: $connectionId") }
        if (connection.workspaceId != workspaceId) {
            throw NoSuchElementException("Connection $connectionId does not belong to workspace $workspaceId")
        }
        return connection
    }

    // Extracts the host (and port) portion from a connection string for display purposes.
    private fun extractHost(type: ConnectionType, connectionString: String): String? {
        return try {
            when (type) {
                ConnectionType.POSTGRESQL, ConnectionType.MYSQL, ConnectionType.AZURE_SQL -> {
                    // JDBC URL formats:
                    //   jdbc:postgresql://host:port/db
                    //   jdbc:mysql://host:port/db
                    //   jdbc:sqlserver://host:port;databaseName=db;... (semicolon-delimited params)
                    val afterSlashes = connectionString.substringAfter("//", "")
                    afterSlashes.substringBefore("/").substringBefore(";").substringBefore("?").ifBlank { null }
                }
                ConnectionType.MONGODB, ConnectionType.MONGODB_COSMOS -> {
                    // MongoDB URI: mongodb://[user:pass@]host:port[/db]
                    val afterSlashes = connectionString.substringAfter("//", "")
                    val hostPart = afterSlashes.substringBefore("/").substringBefore("?")
                    // Strip credentials (user:pass@)
                    val withoutCreds = if (hostPart.contains("@")) hostPart.substringAfter("@") else hostPart
                    withoutCreds.ifBlank { null }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun DataConnection.toResponse() = DataConnectionResponse(
        id = id,
        workspaceId = workspaceId,
        name = name,
        type = type,
        host = host,
        username = username,
        database = database,
        isSource = isSource,
        isDestination = isDestination,
        createdAt = createdAt
    )
}
