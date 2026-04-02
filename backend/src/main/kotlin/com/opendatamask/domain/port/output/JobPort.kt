package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.Job
import com.opendatamask.domain.model.JobStatus
import java.util.Optional

interface JobPort {
    fun findById(id: Long): Optional<Job>
    fun findByWorkspaceId(workspaceId: Long): List<Job>
    fun findByWorkspaceIdOrderByCreatedAtDesc(workspaceId: Long): List<Job>
    fun findByStatus(status: JobStatus): List<Job>
    fun save(job: Job): Job
    fun deleteById(id: Long)
}
