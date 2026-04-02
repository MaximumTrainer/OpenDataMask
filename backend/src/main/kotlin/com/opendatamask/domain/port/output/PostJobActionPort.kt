package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.PostJobAction
import java.util.Optional

interface PostJobActionPort {
    fun findById(id: Long): Optional<PostJobAction>
    fun findByWorkspaceId(workspaceId: Long): List<PostJobAction>
    fun findByWorkspaceIdAndEnabled(workspaceId: Long, enabled: Boolean): List<PostJobAction>
    fun save(action: PostJobAction): PostJobAction
    fun deleteById(id: Long)
    fun deleteByWorkspaceId(workspaceId: Long)
}
