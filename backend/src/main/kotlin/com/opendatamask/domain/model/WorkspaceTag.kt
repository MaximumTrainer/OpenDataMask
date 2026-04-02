package com.opendatamask.domain.model

import jakarta.persistence.*

@Entity
@Table(
    name = "workspace_tags",
    uniqueConstraints = [UniqueConstraint(columnNames = ["workspaceId", "tag"])]
)
class WorkspaceTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false, length = 100)
    var tag: String
)
