package com.opendatamask.domain.port.input

import com.opendatamask.model.SensitivityScanLog

interface SensitivityScanUseCase {
    fun scanWorkspace(workspaceId: Long): SensitivityScanLog
    fun getLatestLog(workspaceId: Long): SensitivityScanLog?
    fun getScanLogs(workspaceId: Long): List<SensitivityScanLog>
}
