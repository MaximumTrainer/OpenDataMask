package com.opendatamask.domain.port.input

import com.opendatamask.domain.model.PrivacyReport

interface PrivacyReportUseCase {
    fun generateCurrentConfigReport(workspaceId: Long): PrivacyReport
    fun generateJobReport(jobId: Long, workspaceId: Long): PrivacyReport
    fun getLatestCurrentReport(workspaceId: Long, withinHours: Long = 1): PrivacyReport?
    fun getJobReport(jobId: Long): PrivacyReport?
    fun getReportsForWorkspace(workspaceId: Long): List<PrivacyReport>
}
