package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.JobTableStats
import com.opendatamask.domain.port.output.JobTableStatsPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobTableStatsRepository : JpaRepository<JobTableStats, Long>, JobTableStatsPort {
    override fun findByJobId(jobId: Long): List<JobTableStats>
    override fun save(stats: JobTableStats): JobTableStats
}
