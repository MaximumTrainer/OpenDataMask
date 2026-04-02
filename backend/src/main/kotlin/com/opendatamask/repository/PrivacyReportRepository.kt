package com.opendatamask.repository

import com.opendatamask.domain.model.PrivacyReport
import org.springframework.data.jpa.repository.JpaRepository

interface PrivacyReportRepository : JpaRepository<PrivacyReport, Long> {
    fun findByWorkspaceIdOrderByGeneratedAtDesc(workspaceId: Long): List<PrivacyReport>
    fun findByJobId(jobId: Long): List<PrivacyReport>
}
