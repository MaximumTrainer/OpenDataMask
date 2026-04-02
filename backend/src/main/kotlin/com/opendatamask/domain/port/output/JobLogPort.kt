package com.opendatamask.domain.port.output

import com.opendatamask.model.JobLog

interface JobLogPort {
    fun findByJobIdOrderByTimestamp(jobId: Long): List<JobLog>
    fun save(log: JobLog): JobLog
    fun deleteByJobId(jobId: Long)
}
