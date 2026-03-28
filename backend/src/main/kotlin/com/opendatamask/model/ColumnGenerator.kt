package com.opendatamask.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class GeneratorType {
    NAME, EMAIL, PHONE, ADDRESS, SSN, CREDIT_CARD, DATE, UUID, CONSTANT, NULL, CUSTOM
}

@Entity
@Table(name = "column_generators")
class ColumnGenerator(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var tableConfigurationId: Long,

    @Column(nullable = false)
    var columnName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var generatorType: GeneratorType,

    @Column(length = 4096)
    var generatorParams: String? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}
