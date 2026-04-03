package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.PostJobAction
import com.opendatamask.domain.port.output.PostJobActionPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PostJobActionRepository : JpaRepository<PostJobAction, Long>, PostJobActionPort {
    override fun findById(id: Long): Optional<PostJobAction>
    override fun findByWorkspaceId(workspaceId: Long): List<PostJobAction>
    override fun findByWorkspaceIdAndEnabled(workspaceId: Long, enabled: Boolean): List<PostJobAction>
    override fun save(action: PostJobAction): PostJobAction
    override fun deleteById(id: Long)
    override fun deleteByWorkspaceId(workspaceId: Long)
}
