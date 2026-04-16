package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.ConnectionPair
import java.util.Optional

interface ConnectionPairPort {
    fun findById(id: Long): Optional<ConnectionPair>
    fun findByWorkspaceId(workspaceId: Long): List<ConnectionPair>
    fun findActiveByWorkspaceId(workspaceId: Long): List<ConnectionPair>
    fun save(connectionPair: ConnectionPair): ConnectionPair
    fun deleteById(id: Long)
}
