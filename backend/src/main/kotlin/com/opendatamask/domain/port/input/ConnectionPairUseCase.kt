package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.ConnectionPairRequest
import com.opendatamask.domain.port.input.dto.ConnectionPairResponse

interface ConnectionPairUseCase {
    fun createConnectionPair(workspaceId: Long, request: ConnectionPairRequest): ConnectionPairResponse
    fun getConnectionPair(workspaceId: Long, pairId: Long): ConnectionPairResponse
    fun listConnectionPairs(workspaceId: Long): List<ConnectionPairResponse>
    fun updateConnectionPair(workspaceId: Long, pairId: Long, request: ConnectionPairRequest): ConnectionPairResponse
    fun deleteConnectionPair(workspaceId: Long, pairId: Long)
}
