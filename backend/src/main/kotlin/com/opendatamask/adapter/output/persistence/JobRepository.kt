package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.Job
import com.opendatamask.domain.model.JobStatus
import com.opendatamask.domain.port.output.JobPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface JobRepository : JpaRepository<Job, Long>, JobPort {
    override fun findById(id: Long): Optional<Job>
    override fun findByWorkspaceId(workspaceId: Long): List<Job>
    override fun findByWorkspaceIdOrderByCreatedAtDesc(workspaceId: Long): List<Job>
    override fun findByStatus(status: JobStatus): List<Job>
    override fun save(job: Job): Job
    override fun deleteById(id: Long)
}
