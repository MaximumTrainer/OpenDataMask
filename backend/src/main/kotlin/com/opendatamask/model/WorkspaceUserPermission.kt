package com.opendatamask.model

import jakarta.persistence.*

@Entity
@Table(
    name = "workspace_user_permissions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["workspace_user_id", "permission"])]
)
class WorkspaceUserPermission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "workspace_user_id", nullable = false)
    val workspaceUserId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val permission: WorkspacePermission,

    @Column(nullable = false)
    val granted: Boolean = true
)
