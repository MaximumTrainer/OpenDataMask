package com.opendatamask.repository

import com.opendatamask.domain.model.JobSchedule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface JobScheduleRepository : JpaRepository<JobSchedule, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<JobSchedule>
    fun findByEnabledTrue(): List<JobSchedule>
}
