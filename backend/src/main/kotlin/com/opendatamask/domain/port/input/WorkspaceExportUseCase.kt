package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.WorkspaceConfigDto

interface WorkspaceExportUseCase {
    fun export(workspaceId: Long): WorkspaceConfigDto
    fun import(workspaceId: Long, config: WorkspaceConfigDto)
}
