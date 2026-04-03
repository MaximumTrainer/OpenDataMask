package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.SensitivityScanLog
import com.opendatamask.domain.port.output.SensitivityScanLogPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SensitivityScanLogRepository : JpaRepository<SensitivityScanLog, Long>, SensitivityScanLogPort {
    override fun findByWorkspaceIdOrderByStartedAtDesc(workspaceId: Long): List<SensitivityScanLog>
    override fun findTopByWorkspaceIdOrderByStartedAtDesc(workspaceId: Long): SensitivityScanLog?
    override fun save(log: SensitivityScanLog): SensitivityScanLog
}
