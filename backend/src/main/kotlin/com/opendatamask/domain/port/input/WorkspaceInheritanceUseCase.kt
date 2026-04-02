package com.opendatamask.domain.port.input

import com.opendatamask.domain.model.InheritedConfig
import com.opendatamask.domain.model.Workspace

interface WorkspaceInheritanceUseCase {
    fun inheritFromParent(childWorkspaceId: Long, parentWorkspaceId: Long)
    fun syncWithParent(childWorkspaceId: Long)
    fun markAsOverridden(inheritedConfigId: Long)
    fun listChildWorkspaces(parentWorkspaceId: Long): List<Workspace>
    fun listInheritedConfigs(childWorkspaceId: Long): List<InheritedConfig>
}
