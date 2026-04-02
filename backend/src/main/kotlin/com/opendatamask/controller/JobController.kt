package com.opendatamask.controller

import com.opendatamask.dto.JobLogResponse
import com.opendatamask.dto.JobResponse
import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.repository.UserRepository
import com.opendatamask.service.JobService
import com.opendatamask.service.PermissionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/jobs")
class JobController(
    private val jobService: JobService,
    private val userRepository: UserRepository,
    private val permissionService: PermissionService
) {

    @PostMapping
    fun createAndRunJob(
        @PathVariable workspaceId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<JobResponse> {
        val userId = getUserId(userDetails)
        permissionService.requirePermission(userId, workspaceId, WorkspacePermission.RUN_JOBS)
        val job = jobService.createJob(workspaceId, userId)
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

    private fun getUserId(userDetails: UserDetails): Long {
        return userRepository.findByUsername(userDetails.username)
            .orElseThrow { NoSuchElementException("User not found") }.id
    }
}
