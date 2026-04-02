package com.opendatamask.controller

import com.opendatamask.domain.model.JobSchedule
import com.opendatamask.service.JobSchedulerService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/schedules")
class JobScheduleController(
    private val schedulerService: JobSchedulerService
) {
    @GetMapping
    fun list(@PathVariable workspaceId: Long): ResponseEntity<List<JobSchedule>> =
        ResponseEntity.ok(schedulerService.listSchedules(workspaceId))

    @PostMapping
    fun create(
        @PathVariable workspaceId: Long,
        @RequestBody schedule: JobSchedule
    ): ResponseEntity<JobSchedule> {
        val toSave = schedule.also { it.workspaceId = workspaceId }
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulerService.createSchedule(toSave))
    }

    @PutMapping("/{scheduleId}")
    fun update(
        @PathVariable workspaceId: Long,
        @PathVariable scheduleId: Long,
        @RequestBody schedule: JobSchedule
    ): ResponseEntity<JobSchedule> =
        ResponseEntity.ok(schedulerService.updateSchedule(scheduleId, schedule))

    @DeleteMapping("/{scheduleId}")
    fun delete(
        @PathVariable workspaceId: Long,
        @PathVariable scheduleId: Long
    ): ResponseEntity<Void> {
        schedulerService.deleteSchedule(scheduleId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/validate-cron")
    fun validateCron(@RequestBody body: Map<String, String>): ResponseEntity<Map<String, Any>> {
        val expression = body["cronExpression"] ?: return ResponseEntity.badRequest().build()
        return try {
            schedulerService.validateCron(expression)
            val nextRun = schedulerService.computeNextRun(expression)
            ResponseEntity.ok(mapOf("valid" to true, "nextRun" to nextRun.toString()))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.ok(mapOf("valid" to false, "error" to (e.message ?: "Invalid")))
        }
    }
}
