package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.PrivacyReport
import com.opendatamask.domain.port.output.PrivacyReportPort
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PrivacyReportRepository : JpaRepository<PrivacyReport, Long>, PrivacyReportPort {
    override fun findById(id: Long): Optional<PrivacyReport>
    override fun findByWorkspaceIdOrderByGeneratedAtDesc(workspaceId: Long): List<PrivacyReport>
    override fun findByJobId(jobId: Long): List<PrivacyReport>
    override fun save(report: PrivacyReport): PrivacyReport
}
