package com.opendatamask.application.service

import com.opendatamask.domain.port.input.DataConnectionUseCase

import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.adapter.output.connector.ConnectorFactory
import com.opendatamask.domain.port.input.dto.ConnectionTestResult
import com.opendatamask.domain.port.input.dto.DataConnectionRequest
import com.opendatamask.domain.port.input.dto.DataConnectionResponse
import com.opendatamask.domain.model.DataConnection
import com.opendatamask.adapter.output.persistence.DataConnectionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DataConnectionService(
    private val dataConnectionRepository: DataConnectionRepository,
    private val encryptionPort: EncryptionPort,
    private val connectorFactory: ConnectorFactory
) : DataConnectionUseCase {

    @Transactional
    override fun createConnection(workspaceId: Long, request: DataConnectionRequest): DataConnectionResponse {
        val connection = DataConnection(
            workspaceId = workspaceId,
            name = request.name,
            type = request.type,
            connectionString = encryptionPort.encrypt(request.connectionString),
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
        connection.name = request.name
        connection.type = request.type
        connection.connectionString = encryptionPort.encrypt(request.connectionString)
        connection.username = request.username
        connection.password = request.password?.let { encryptionPort.encrypt(it) }
        connection.database = request.database
        connection.isSource = request.isSource
        connection.isDestination = request.isDestination
        return dataConnectionRepository.save(connection).toResponse()
    }

    @Transactional
    override fun deleteConnection(workspaceId: Long, connectionId: Long) {
        val connection = findConnection(workspaceId, connectionId)
        dataConnectionRepository.delete(connection)
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

    private fun DataConnection.toResponse() = DataConnectionResponse(
        id = id,
        workspaceId = workspaceId,
        name = name,
        type = type,
        username = username,
        database = database,
        isSource = isSource,
        isDestination = isDestination,
        createdAt = createdAt
    )
}
