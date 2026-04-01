package com.opendatamask.service

import com.opendatamask.model.JobSchedule
import com.opendatamask.model.ScheduledJobType
import com.opendatamask.repository.JobScheduleRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Service
class JobSchedulerService(
    private val jobScheduleRepository: JobScheduleRepository,
    private val jobService: JobService
) {
    private val logger = LoggerFactory.getLogger(JobSchedulerService::class.java)

    fun createSchedule(schedule: JobSchedule): JobSchedule {
        validateCron(schedule.cronExpression)
        schedule.nextRunAt = computeNextRun(schedule.cronExpression)
        return jobScheduleRepository.save(schedule)
    }

    fun updateSchedule(id: Long, schedule: JobSchedule): JobSchedule {
        val existing = jobScheduleRepository.findById(id).orElseThrow { NoSuchElementException("Schedule not found: $id") }
        validateCron(schedule.cronExpression)
        existing.cronExpression = schedule.cronExpression
        existing.enabled = schedule.enabled
        existing.jobType = schedule.jobType
        existing.nextRunAt = computeNextRun(schedule.cronExpression)
        return jobScheduleRepository.save(existing)
    }

    fun deleteSchedule(id: Long) = jobScheduleRepository.deleteById(id)

    fun listSchedules(workspaceId: Long): List<JobSchedule> = jobScheduleRepository.findByWorkspaceId(workspaceId)

    fun validateCron(expression: String) {
        try {
            CronExpression.parse(expression)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid cron expression '$expression': ${e.message}")
        }
    }

    fun computeNextRun(cronExpression: String): LocalDateTime? {
        return try {
            val cron = CronExpression.parse(cronExpression)
            cron.next(ZonedDateTime.now())?.toLocalDateTime()
        } catch (e: Exception) { null }
    }

    @Scheduled(fixedRate = 60_000)
    fun processSchedules() {
        val now = LocalDateTime.now()
        jobScheduleRepository.findByEnabledTrue()
            .filter { schedule -> schedule.nextRunAt?.isBefore(now) == true }
            .forEach { schedule ->
                try {
                    logger.info("Triggering scheduled job for workspace ${schedule.workspaceId} (schedule ${schedule.id})")
                    val job = jobService.createJob(schedule.workspaceId)
                    jobService.runJob(job.id)
                    schedule.lastRunAt = now
                    schedule.lastJobId = job.id
                    schedule.nextRunAt = computeNextRun(schedule.cronExpression)
                    jobScheduleRepository.save(schedule)
                } catch (e: Exception) {
                    logger.error("Scheduled job failed for workspace ${schedule.workspaceId}: ${e.message}")
                    schedule.nextRunAt = computeNextRun(schedule.cronExpression)
                    jobScheduleRepository.save(schedule)
                }
            }
    }
}
