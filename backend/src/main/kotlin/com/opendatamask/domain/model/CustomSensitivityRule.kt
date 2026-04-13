package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.Instant

enum class MatcherType {
    CONTAINS, STARTS_WITH, ENDS_WITH, REGEX
}

data class CustomRuleMatcher(
    val matcherType: MatcherType,
    val value: String,
    val caseSensitive: Boolean = false
)

@Entity
@Table(name = "custom_sensitivity_rules")
class CustomSensitivityRule(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var dataTypeFilter: GenericDataType = GenericDataType.ANY,

    @Column(columnDefinition = "TEXT", nullable = false)
    var matchersJson: String = "[]",

    var linkedPresetId: Long? = null,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
