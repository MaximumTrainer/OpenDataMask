package com.opendatamask.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class SchemaChangeHandling {
    BLOCK_ALL, BLOCK_EXPOSING, NEVER_BLOCK
}

@Entity
@Table(name = "workspaces")
class Workspace(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String,

    @Column
    var description: String? = null,

    @Column(nullable = false)
    var ownerId: Long,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var schemaChangeHandling: SchemaChangeHandling = SchemaChangeHandling.BLOCK_EXPOSING
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
