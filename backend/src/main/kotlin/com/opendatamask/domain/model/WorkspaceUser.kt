package com.opendatamask.domain.model

import jakarta.persistence.*

enum class WorkspaceRole {
    ADMIN, USER, VIEWER
}

@Entity
@Table(
    name = "workspace_users",
    uniqueConstraints = [UniqueConstraint(columnNames = ["workspaceId", "userId"])]
)
class WorkspaceUser(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: WorkspaceRole = WorkspaceRole.USER
)
