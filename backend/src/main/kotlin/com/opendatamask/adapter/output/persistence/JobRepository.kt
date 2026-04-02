package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.Job
import com.opendatamask.domain.model.JobStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobRepository : JpaRepository<Job, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<Job>
    fun findByWorkspaceIdOrderByCreatedAtDesc(workspaceId: Long): List<Job>
    fun findByStatus(status: JobStatus): List<Job>
}
