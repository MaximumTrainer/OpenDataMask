package com.opendatamask.domain.port.output

import com.opendatamask.model.PrivacyReport
import java.util.Optional

interface PrivacyReportPort {
    fun findById(id: Long): Optional<PrivacyReport>
    fun findByWorkspaceIdOrderByGeneratedAtDesc(workspaceId: Long): List<PrivacyReport>
    fun findByJobId(jobId: Long): List<PrivacyReport>
    fun save(report: PrivacyReport): PrivacyReport
}
