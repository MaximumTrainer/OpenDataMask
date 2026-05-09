package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.JobLogResponse
import com.opendatamask.domain.port.input.dto.CreateJobRequest
import com.opendatamask.domain.port.input.dto.JobResponse
import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.application.service.JobProgressEmitterRegistry
import com.opendatamask.application.service.JobService
import com.opendatamask.application.service.PermissionService
import com.opendatamask.application.service.JobCompareService
import com.opendatamask.domain.port.input.dto.JobCompareResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import io.swagger.v3.oas.annotations.Operation

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/jobs")
class JobController(
    private val jobService: JobService,
    private val userRepository: UserRepository,
    private val permissionService: PermissionService,
    private val jobProgressEmitterRegistry: JobProgressEmitterRegistry,
    private val jobCompareService: JobCompareService
) {

    @PostMapping
    fun createAndRunJob(
        @PathVariable workspaceId: Long,
        @RequestBody(required = false) request: CreateJobRequest?,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<JobResponse> {
        val userId = getUserId(userDetails)
        permissionService.requirePermission(userId, workspaceId, WorkspacePermission.RUN_JOBS)
        val job = jobService.createJob(workspaceId, userId, request?.connectionPairId, request?.name, request?.dryRun ?: false)
        jobService.runJob(job.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(job)
    }

    @GetMapping
    fun listJobs(@PathVariable workspaceId: Long): ResponseEntity<List<JobResponse>> {
        return ResponseEntity.ok(jobService.listJobs(workspaceId))
    }

    @GetMapping("/{jobId}")
    fun getJob(
        @PathVariable workspaceId: Long,
        @PathVariable jobId: Long
    ): ResponseEntity<JobResponse> {
        return ResponseEntity.ok(jobService.getJob(workspaceId, jobId))
    }

    @GetMapping("/{jobId}/logs")
    fun getJobLogs(
        @PathVariable workspaceId: Long,
        @PathVariable jobId: Long
    ): ResponseEntity<List<JobLogResponse>> {
        return ResponseEntity.ok(jobService.getJobLogs(workspaceId, jobId))
    }

    @PostMapping("/{jobId}/cancel")
    fun cancelJob(
        @PathVariable workspaceId: Long,
        @PathVariable jobId: Long
    ): ResponseEntity<JobResponse> {
        return ResponseEntity.ok(jobService.cancelJob(workspaceId, jobId))
    }

    @GetMapping("/{jobId}/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @Operation(summary = "Stream real-time job progress via Server-Sent Events")
    fun streamJobProgress(
        @PathVariable workspaceId: Long,
        @PathVariable jobId: Long
    ): SseEmitter {
        jobService.getJob(workspaceId, jobId)
        return jobProgressEmitterRegistry.register(jobId)
    }

    @GetMapping("/{jobId}/stats")
    @Operation(summary = "Get per-table throughput statistics for a completed job")
    fun getJobTableStats(
        @PathVariable workspaceId: Long,
        @PathVariable jobId: Long
    ): ResponseEntity<List<com.opendatamask.domain.port.input.dto.JobTableStatsResponse>> {
        return ResponseEntity.ok(jobService.getJobTableStats(workspaceId, jobId))
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare two job runs — rows delta, tables added/removed")
    fun compareJobs(
        @PathVariable workspaceId: Long,
        @RequestParam jobA: Long,
        @RequestParam jobB: Long
    ): ResponseEntity<JobCompareResponse> {
        return ResponseEntity.ok(jobCompareService.compareJobs(workspaceId, jobA, jobB))
    }

    private fun getUserId(userDetails: UserDetails): Long {
        return userRepository.findByUsername(userDetails.username)
            .orElseThrow { NoSuchElementException("User not found") }.id
    }
}

