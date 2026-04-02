package com.opendatamask.service

import com.opendatamask.dto.*
import com.opendatamask.domain.model.Workspace
import com.opendatamask.domain.model.WorkspaceRole
import com.opendatamask.domain.model.WorkspaceUser
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.adapter.output.persistence.WorkspaceRepository
import com.opendatamask.adapter.output.persistence.WorkspaceUserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WorkspaceService(
    private val workspaceRepository: WorkspaceRepository,
    private val workspaceUserRepository: WorkspaceUserRepository,
    private val userRepository: UserRepository,
    private val workspaceInheritanceService: WorkspaceInheritanceService,
    private val sensitivityScanService: SensitivityScanService
) {

    @Transactional
    fun createWorkspace(request: WorkspaceRequest, ownerId: Long): WorkspaceResponse {
        val workspace = Workspace(
            name = request.name,
            description = request.description,
            ownerId = ownerId,
            parentWorkspaceId = request.parentWorkspaceId,
            inheritanceEnabled = request.inheritanceEnabled
        )
        val saved = workspaceRepository.save(workspace)

        // Add owner as ADMIN
        workspaceUserRepository.save(
            WorkspaceUser(workspaceId = saved.id, userId = ownerId, role = WorkspaceRole.ADMIN)
        )

        if (saved.inheritanceEnabled && saved.parentWorkspaceId != null) {
            workspaceInheritanceService.inheritFromParent(saved.id, saved.parentWorkspaceId)
        }

        Thread { sensitivityScanService.scanWorkspace(saved.id) }.start()

        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun getWorkspace(workspaceId: Long): WorkspaceResponse {
        val workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }
        return workspace.toResponse()
    }

    @Transactional(readOnly = true)
    fun listWorkspaces(userId: Long): List<WorkspaceResponse> {
        val memberWorkspaceIds = workspaceUserRepository.findByUserId(userId).map { it.workspaceId }
        val ownedWorkspaces = workspaceRepository.findByOwnerId(userId)
        val memberWorkspaces = memberWorkspaceIds
            .mapNotNull { workspaceRepository.findById(it).orElse(null) }

        return (ownedWorkspaces + memberWorkspaces)
            .distinctBy { it.id }
            .map { it.toResponse() }
    }

    @Transactional
    fun updateWorkspace(workspaceId: Long, request: WorkspaceRequest, userId: Long): WorkspaceResponse {
        val workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }

        requireWorkspaceAdmin(workspaceId, userId)

        workspace.name = request.name
        workspace.description = request.description
        return workspaceRepository.save(workspace).toResponse()
    }

    @Transactional
    fun deleteWorkspace(workspaceId: Long, userId: Long) {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }

        requireWorkspaceAdmin(workspaceId, userId)
        workspaceUserRepository.findByWorkspaceId(workspaceId)
            .forEach { workspaceUserRepository.delete(it) }
        workspaceRepository.deleteById(workspaceId)
    }

    @Transactional
    fun addUserToWorkspace(workspaceId: Long, request: WorkspaceUserRequest, requestingUserId: Long): WorkspaceUserResponse {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }
        requireWorkspaceAdmin(workspaceId, requestingUserId)

        val targetUser = userRepository.findById(request.userId)
            .orElseThrow { NoSuchElementException("User not found: ${request.userId}") }

        val existing = workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, request.userId)
        val role = WorkspaceRole.valueOf(request.role.uppercase())

        val workspaceUser = if (existing.isPresent) {
            val wu = existing.get()
            wu.role = role
            workspaceUserRepository.save(wu)
        } else {
            workspaceUserRepository.save(
                WorkspaceUser(workspaceId = workspaceId, userId = request.userId, role = role)
            )
        }

        return WorkspaceUserResponse(
            id = workspaceUser.id,
            workspaceId = workspaceUser.workspaceId,
            userId = workspaceUser.userId,
            username = targetUser.username,
            email = targetUser.email,
            role = workspaceUser.role.name
        )
    }

    @Transactional
    fun removeUserFromWorkspace(workspaceId: Long, userId: Long, requestingUserId: Long) {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }
        requireWorkspaceAdmin(workspaceId, requestingUserId)

        workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
            .orElseThrow { NoSuchElementException("User $userId is not a member of workspace $workspaceId") }
        workspaceUserRepository.deleteByWorkspaceIdAndUserId(workspaceId, userId)
    }

    @Transactional(readOnly = true)
    fun getUsersInWorkspace(workspaceId: Long): List<WorkspaceUserResponse> {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }

        return workspaceUserRepository.findByWorkspaceId(workspaceId).mapNotNull { wu ->
            userRepository.findById(wu.userId).map { user ->
                WorkspaceUserResponse(
                    id = wu.id,
                    workspaceId = wu.workspaceId,
                    userId = wu.userId,
                    username = user.username,
                    email = user.email,
                    role = wu.role.name
                )
            }.orElse(null)
        }
    }

    fun requireWorkspaceMember(workspaceId: Long, userId: Long) {
        workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
            .orElseThrow { SecurityException("User $userId is not a member of workspace $workspaceId") }
    }

    fun requireWorkspaceAdmin(workspaceId: Long, userId: Long) {
        val workspaceUser = workspaceUserRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
            .orElseThrow { SecurityException("User $userId is not a member of workspace $workspaceId") }
        if (workspaceUser.role != WorkspaceRole.ADMIN) {
            throw SecurityException("User $userId does not have ADMIN role in workspace $workspaceId")
        }
    }

    private fun Workspace.toResponse() = WorkspaceResponse(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        parentWorkspaceId = parentWorkspaceId,
        inheritanceEnabled = inheritanceEnabled
    )
}
