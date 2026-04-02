package com.opendatamask.service

import com.opendatamask.domain.model.Job
import com.opendatamask.domain.model.JobSchedule
import com.opendatamask.domain.model.JobStatus
import com.opendatamask.domain.model.ScheduledJobType
import com.opendatamask.adapter.output.persistence.JobScheduleRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Optional

class JobSchedulerServiceTest {

    private val scheduleRepo = mock<JobScheduleRepository>()
    private val jobService = mock<JobService>()
    private val service = JobSchedulerService(scheduleRepo, jobService)

    @Test
    fun `validateCron accepts valid expression`() {
        service.validateCron("0 0 * * * *")
        service.validateCron("0 0 0 * * *")
    }

    @Test
    fun `validateCron rejects invalid expression`() {
        assertThrows<IllegalArgumentException> {
            service.validateCron("not-a-cron")
        }
    }

    @Test
    fun `computeNextRun returns future time for valid cron`() {
        val result = service.computeNextRun("0 0 * * * *")
        assertNotNull(result)
        assertTrue(result!!.isAfter(LocalDateTime.now()))
    }

    @Test
    fun `createSchedule saves with nextRunAt set`() {
        val schedule = JobSchedule(workspaceId = 1L, cronExpression = "0 0 * * * *")
        whenever(scheduleRepo.save(any<JobSchedule>())).thenAnswer { it.arguments[0] as JobSchedule }
        val saved = service.createSchedule(schedule)
        assertNotNull(saved.nextRunAt)
        verify(scheduleRepo).save(schedule)
    }

    @Test
    fun `createSchedule throws on invalid cron`() {
        val schedule = JobSchedule(workspaceId = 1L, cronExpression = "invalid")
        assertThrows<IllegalArgumentException> {
            service.createSchedule(schedule)
        }
    }

    @Test
    fun `processSchedules triggers due jobs`() {
        val dueSchedule = JobSchedule(
            id = 1L, workspaceId = 1L, cronExpression = "0 0 * * * *",
            enabled = true, nextRunAt = LocalDateTime.now().minusMinutes(5)
        )
        whenever(scheduleRepo.findByEnabledTrue()).thenReturn(listOf(dueSchedule))
        val mockJob = Job(id = 99L, workspaceId = 1L, status = JobStatus.PENDING, createdBy = 0L)
        whenever(jobService.createJob(1L)).thenReturn(mockJob)
        whenever(scheduleRepo.save(any<JobSchedule>())).thenAnswer { it.arguments[0] as JobSchedule }

        service.processSchedules()

        verify(jobService).createJob(1L)
        verify(jobService).runJob(99L)
    }
}
