package com.opendatamask.domain.port.input

import com.opendatamask.dto.WorkspaceRequest
import com.opendatamask.dto.WorkspaceResponse
import com.opendatamask.dto.WorkspaceUserRequest
import com.opendatamask.dto.WorkspaceUserResponse

interface WorkspaceUseCase {
    fun createWorkspace(request: WorkspaceRequest, ownerId: Long): WorkspaceResponse
    fun getWorkspace(workspaceId: Long): WorkspaceResponse
    fun listWorkspaces(userId: Long): List<WorkspaceResponse>
    fun updateWorkspace(workspaceId: Long, request: WorkspaceRequest, userId: Long): WorkspaceResponse
    fun deleteWorkspace(workspaceId: Long, userId: Long)
    fun addUserToWorkspace(workspaceId: Long, request: WorkspaceUserRequest, requestingUserId: Long): WorkspaceUserResponse
    fun removeUserFromWorkspace(workspaceId: Long, userId: Long, requestingUserId: Long)
    fun getUsersInWorkspace(workspaceId: Long): List<WorkspaceUserResponse>
}
