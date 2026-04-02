package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "column_comments")
class ColumnComment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var tableName: String,

    @Column(nullable = false)
    var columnName: String,

    @Column(nullable = false)
    var userId: Long,

    @Column(nullable = false, columnDefinition = "TEXT")
    var comment: String,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() { createdAt = LocalDateTime.now() }
}
