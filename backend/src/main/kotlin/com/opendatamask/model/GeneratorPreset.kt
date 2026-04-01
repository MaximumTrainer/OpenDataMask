package com.opendatamask.model

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "generator_presets")
class GeneratorPreset(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val generatorType: GeneratorType,

    @Column(columnDefinition = "TEXT")
    val generatorParams: String? = null,

    val workspaceId: Long? = null,

    @Column(nullable = false)
    val isSystem: Boolean = false,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
