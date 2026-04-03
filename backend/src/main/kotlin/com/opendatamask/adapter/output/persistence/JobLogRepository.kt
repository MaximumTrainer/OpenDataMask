package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.JobLog
import com.opendatamask.domain.port.output.JobLogPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobLogRepository : JpaRepository<JobLog, Long>, JobLogPort {
    override fun findByJobIdOrderByTimestamp(jobId: Long): List<JobLog>
    override fun save(log: JobLog): JobLog
    override fun deleteByJobId(jobId: Long)
}
