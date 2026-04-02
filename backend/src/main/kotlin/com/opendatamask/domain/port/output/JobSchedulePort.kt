package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.JobSchedule
import java.util.Optional

interface JobSchedulePort {
    fun findById(id: Long): Optional<JobSchedule>
    fun findByWorkspaceId(workspaceId: Long): List<JobSchedule>
    fun findByEnabledTrue(): List<JobSchedule>
    fun save(schedule: JobSchedule): JobSchedule
    fun deleteById(id: Long)
}
