package com.opendatamask.domain.port.input

import com.opendatamask.adapter.input.rest.dto.JobLogResponse
import com.opendatamask.adapter.input.rest.dto.JobResponse

interface JobUseCase {
    fun createJob(workspaceId: Long, createdBy: Long): JobResponse
    fun getJob(workspaceId: Long, jobId: Long): JobResponse
    fun listJobs(workspaceId: Long): List<JobResponse>
    fun getJobLogs(workspaceId: Long, jobId: Long): List<JobLogResponse>
    fun cancelJob(workspaceId: Long, jobId: Long): JobResponse
    fun createAndRunJob(workspaceId: Long, createdBy: Long): JobResponse
}
