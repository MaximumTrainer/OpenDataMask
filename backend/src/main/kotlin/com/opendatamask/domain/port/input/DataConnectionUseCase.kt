package com.opendatamask.domain.port.input

import com.opendatamask.dto.ConnectionTestResult
import com.opendatamask.dto.DataConnectionRequest
import com.opendatamask.dto.DataConnectionResponse

interface DataConnectionUseCase {
    fun createConnection(workspaceId: Long, request: DataConnectionRequest): DataConnectionResponse
    fun getConnection(workspaceId: Long, connectionId: Long): DataConnectionResponse
    fun listConnections(workspaceId: Long): List<DataConnectionResponse>
    fun updateConnection(workspaceId: Long, connectionId: Long, request: DataConnectionRequest): DataConnectionResponse
    fun deleteConnection(workspaceId: Long, connectionId: Long)
    fun testConnection(workspaceId: Long, connectionId: Long): ConnectionTestResult
}
