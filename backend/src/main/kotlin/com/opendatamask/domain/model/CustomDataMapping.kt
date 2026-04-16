package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class MappingAction {
    MIGRATE_AS_IS, MASK
}

enum class MaskingStrategy {
    FAKE, HASH, NULL
}

@Entity
@Table(name = "custom_data_mappings")
class CustomDataMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var connectionId: Long,

    @Column(nullable = false)
    var tableName: String,

    @Column(nullable = false)
    var columnName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var action: MappingAction,

    @Enumerated(EnumType.STRING)
    @Column
    var maskingStrategy: MaskingStrategy? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var fakeGeneratorType: GeneratorType? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
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
