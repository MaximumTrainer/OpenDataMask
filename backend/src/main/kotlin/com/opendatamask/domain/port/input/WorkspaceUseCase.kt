package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.WorkspaceRequest
import com.opendatamask.domain.port.input.dto.WorkspaceResponse
import com.opendatamask.domain.port.input.dto.WorkspaceUserRequest
import com.opendatamask.domain.port.input.dto.WorkspaceUserResponse
import com.opendatamask.domain.port.input.dto.WorkspaceStatsResponse

interface WorkspaceUseCase {
    fun createWorkspace(request: WorkspaceRequest, ownerId: Long): WorkspaceResponse
    fun getWorkspace(workspaceId: Long): WorkspaceResponse
    fun listWorkspaces(userId: Long): List<WorkspaceResponse>
    fun updateWorkspace(workspaceId: Long, request: WorkspaceRequest, userId: Long): WorkspaceResponse
    fun deleteWorkspace(workspaceId: Long, userId: Long)
    fun addUserToWorkspace(workspaceId: Long, request: WorkspaceUserRequest, requestingUserId: Long): WorkspaceUserResponse
    fun removeUserFromWorkspace(workspaceId: Long, userId: Long, requestingUserId: Long)
    fun getUsersInWorkspace(workspaceId: Long): List<WorkspaceUserResponse>
    fun getStats(workspaceId: Long): WorkspaceStatsResponse
}
