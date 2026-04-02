package com.opendatamask.domain.port.input

import com.opendatamask.dto.JobLogResponse
import com.opendatamask.dto.JobResponse

interface JobUseCase {
    fun createJob(workspaceId: Long, createdBy: Long): JobResponse
    fun getJob(jobId: Long): JobResponse
    fun listJobs(workspaceId: Long): List<JobResponse>
    fun getJobLogs(jobId: Long): List<JobLogResponse>
    fun cancelJob(jobId: Long): JobResponse
    fun createAndRunJob(workspaceId: Long, createdBy: Long): JobResponse
}
