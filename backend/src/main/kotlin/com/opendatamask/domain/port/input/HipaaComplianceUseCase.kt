package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.HipaaComplianceReport

interface HipaaComplianceUseCase {
    fun getComplianceReport(workspaceId: Long): HipaaComplianceReport
}
