package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.JobLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobLogRepository : JpaRepository<JobLog, Long> {
    fun findByJobIdOrderByTimestamp(jobId: Long): List<JobLog>
    fun deleteByJobId(jobId: Long)
}
