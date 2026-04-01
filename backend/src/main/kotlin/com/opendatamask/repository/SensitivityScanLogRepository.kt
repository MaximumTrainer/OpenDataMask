package com.opendatamask.repository

import com.opendatamask.model.SensitivityScanLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SensitivityScanLogRepository : JpaRepository<SensitivityScanLog, Long> {
    fun findByWorkspaceIdOrderByStartedAtDesc(workspaceId: Long): List<SensitivityScanLog>
    fun findTopByWorkspaceIdOrderByStartedAtDesc(workspaceId: Long): SensitivityScanLog?
}
