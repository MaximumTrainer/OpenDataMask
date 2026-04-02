package com.opendatamask.domain.port.input

import com.opendatamask.adapter.input.rest.dto.WorkspaceRequest
import com.opendatamask.adapter.input.rest.dto.WorkspaceResponse
import com.opendatamask.adapter.input.rest.dto.WorkspaceUserRequest
import com.opendatamask.adapter.input.rest.dto.WorkspaceUserResponse

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
