package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.JobLogResponse
import com.opendatamask.domain.port.input.dto.JobResponse

interface JobUseCase {
    fun createJob(workspaceId: Long, createdBy: Long, connectionPairId: Long? = null): JobResponse
    fun getJob(workspaceId: Long, jobId: Long): JobResponse
    fun listJobs(workspaceId: Long): List<JobResponse>
    fun getJobLogs(workspaceId: Long, jobId: Long): List<JobLogResponse>
    fun cancelJob(workspaceId: Long, jobId: Long): JobResponse
    fun createAndRunJob(workspaceId: Long, createdBy: Long, connectionPairId: Long? = null): JobResponse
}
