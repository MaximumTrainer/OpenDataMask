package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "connection_pairs")
class ConnectionPair(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var name: String,

    @Column
    var description: String? = null,

    @Column(nullable = false)
    var sourceConnectionId: Long,

    @Column(nullable = false)
    var destinationConnectionId: Long,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    // Soft-delete timestamp; null means the pair is active.
    @Column
    var deletedAt: LocalDateTime? = null
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
