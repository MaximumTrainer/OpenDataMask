package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "stored_files")
class StoredFile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var filename: String,

    @Column(nullable = false)
    var contentType: String,

    @Column(nullable = false)
    var isSource: Boolean = true,

    @Column(nullable = false, columnDefinition = "TEXT")
    var encryptedContent: String,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() { createdAt = LocalDateTime.now() }
}
