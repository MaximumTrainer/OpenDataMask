package com.opendatamask.domain.port.output

import com.opendatamask.model.SensitivityScanLog

interface SensitivityScanLogPort {
    fun findByWorkspaceIdOrderByStartedAtDesc(workspaceId: Long): List<SensitivityScanLog>
    fun findTopByWorkspaceIdOrderByStartedAtDesc(workspaceId: Long): SensitivityScanLog?
    fun save(log: SensitivityScanLog): SensitivityScanLog
}
