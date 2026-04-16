package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.ConnectionSchemaResponse
import com.opendatamask.domain.port.input.dto.ConnectionTestResult
import com.opendatamask.domain.port.input.dto.DataConnectionRequest
import com.opendatamask.domain.port.input.dto.DataConnectionResponse

interface DataConnectionUseCase {
    fun createConnection(workspaceId: Long, request: DataConnectionRequest): DataConnectionResponse
    fun getConnection(workspaceId: Long, connectionId: Long): DataConnectionResponse
    fun listConnections(workspaceId: Long): List<DataConnectionResponse>
    fun updateConnection(workspaceId: Long, connectionId: Long, request: DataConnectionRequest): DataConnectionResponse
    fun deleteConnection(workspaceId: Long, connectionId: Long)
    fun testConnection(workspaceId: Long, connectionId: Long): ConnectionTestResult
    fun browseConnectionSchema(workspaceId: Long, connectionId: Long): ConnectionSchemaResponse
}
