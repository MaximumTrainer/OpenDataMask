package com.opendatamask.domain.port.input

import com.opendatamask.model.JobSchedule

interface JobScheduleUseCase {
    fun createSchedule(schedule: JobSchedule): JobSchedule
    fun updateSchedule(id: Long, schedule: JobSchedule): JobSchedule
    fun deleteSchedule(id: Long)
    fun listSchedules(workspaceId: Long): List<JobSchedule>
}
