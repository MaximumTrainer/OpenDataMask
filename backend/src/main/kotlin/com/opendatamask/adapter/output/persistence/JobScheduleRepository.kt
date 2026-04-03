package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.JobSchedule
import com.opendatamask.domain.port.output.JobSchedulePort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface JobScheduleRepository : JpaRepository<JobSchedule, Long>, JobSchedulePort {
    override fun findById(id: Long): Optional<JobSchedule>
    override fun findByWorkspaceId(workspaceId: Long): List<JobSchedule>
    override fun findByEnabledTrue(): List<JobSchedule>
    override fun save(schedule: JobSchedule): JobSchedule
    override fun deleteById(id: Long)
}
