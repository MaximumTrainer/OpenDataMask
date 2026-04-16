package com.opendatamask.application.service

import com.opendatamask.domain.port.input.ConnectionPairUseCase
import com.opendatamask.domain.port.output.ConnectionPairPort
import com.opendatamask.domain.port.output.DataConnectionPort
import com.opendatamask.domain.port.output.WorkspacePort
import com.opendatamask.domain.model.ConnectionPair
import com.opendatamask.domain.port.input.dto.ConnectionPairRequest
import com.opendatamask.domain.port.input.dto.ConnectionPairResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class ConnectionPairService(
    private val connectionPairRepository: ConnectionPairPort,
    private val dataConnectionRepository: DataConnectionPort,
    private val workspaceRepository: WorkspacePort
) : ConnectionPairUseCase {

    @Transactional
    override fun createConnectionPair(workspaceId: Long, request: ConnectionPairRequest): ConnectionPairResponse {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }
        validateConnectionsBelongToWorkspace(workspaceId, request.sourceConnectionId, request.destinationConnectionId)
        val pair = ConnectionPair(
            workspaceId = workspaceId,
            name = request.name,
            description = request.description,
            sourceConnectionId = request.sourceConnectionId,
            destinationConnectionId = request.destinationConnectionId
        )
        return connectionPairRepository.save(pair).toResponse()
    }

    @Transactional(readOnly = true)
    override fun getConnectionPair(workspaceId: Long, pairId: Long): ConnectionPairResponse {
        return findActivePair(workspaceId, pairId).toResponse()
    }

    @Transactional(readOnly = true)
    override fun listConnectionPairs(workspaceId: Long): List<ConnectionPairResponse> {
        return connectionPairRepository.findActiveByWorkspaceId(workspaceId).map { it.toResponse() }
    }

    @Transactional
    override fun updateConnectionPair(
        workspaceId: Long,
        pairId: Long,
        request: ConnectionPairRequest
    ): ConnectionPairResponse {
        val pair = findActivePair(workspaceId, pairId)
        validateConnectionsBelongToWorkspace(workspaceId, request.sourceConnectionId, request.destinationConnectionId)
        pair.name = request.name
        pair.description = request.description
        pair.sourceConnectionId = request.sourceConnectionId
        pair.destinationConnectionId = request.destinationConnectionId
        return connectionPairRepository.save(pair).toResponse()
    }

    @Transactional
    override fun deleteConnectionPair(workspaceId: Long, pairId: Long) {
        val pair = findActivePair(workspaceId, pairId)
        pair.deletedAt = LocalDateTime.now()
        connectionPairRepository.save(pair)
    }

    private fun findActivePair(workspaceId: Long, pairId: Long): ConnectionPair {
        val pair = connectionPairRepository.findById(pairId)
            .orElseThrow { NoSuchElementException("Connection pair not found: $pairId") }
        if (pair.workspaceId != workspaceId) {
            throw NoSuchElementException("Connection pair $pairId does not belong to workspace $workspaceId")
        }
        if (pair.deletedAt != null) {
            throw NoSuchElementException("Connection pair $pairId has been deleted")
        }
        return pair
    }

    private fun validateConnectionsBelongToWorkspace(workspaceId: Long, sourceId: Long, destinationId: Long) {
        if (sourceId == destinationId) {
            throw IllegalArgumentException("Source and destination connections must be distinct")
        }
        val source = dataConnectionRepository.findById(sourceId)
            .orElseThrow { NoSuchElementException("Source connection not found: $sourceId") }
        if (source.workspaceId != workspaceId) {
            throw IllegalArgumentException("Source connection $sourceId does not belong to workspace $workspaceId")
        }
        if (!source.isSource) {
            throw IllegalArgumentException("Connection $sourceId is not configured as a source connection")
        }
        val destination = dataConnectionRepository.findById(destinationId)
            .orElseThrow { NoSuchElementException("Destination connection not found: $destinationId") }
        if (destination.workspaceId != workspaceId) {
            throw IllegalArgumentException(
                "Destination connection $destinationId does not belong to workspace $workspaceId"
            )
        }
        if (!destination.isDestination) {
            throw IllegalArgumentException("Connection $destinationId is not configured as a destination connection")
        }
    }

    private fun ConnectionPair.toResponse() = ConnectionPairResponse(
        id = id,
        workspaceId = workspaceId,
        name = name,
        description = description,
        sourceConnectionId = sourceConnectionId,
        destinationConnectionId = destinationConnectionId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
