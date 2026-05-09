package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.JobTableStats

interface JobTableStatsPort {
    fun save(stats: JobTableStats): JobTableStats
    fun findByJobId(jobId: Long): List<JobTableStats>
}
