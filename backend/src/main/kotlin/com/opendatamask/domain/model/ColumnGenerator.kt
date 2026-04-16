package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class GeneratorType {
    // Existing
    NAME, EMAIL, PHONE, ADDRESS, SSN, CREDIT_CARD, DATE, UUID, CONSTANT, NULL, CUSTOM,
    // Name variants
    FIRST_NAME, LAST_NAME, FULL_NAME,
    // Location
    STREET_ADDRESS, CITY, STATE, ZIP_CODE, COUNTRY, POSTAL_CODE, GPS_COORDINATES,
    // Credentials
    USERNAME, PASSWORD,
    // Financial
    IBAN, SWIFT_CODE, MONEY_AMOUNT, BTC_ADDRESS,
    // Identification
    PASSPORT_NUMBER, DRIVERS_LICENSE, BIRTH_DATE, GENDER,
    // Medical
    ICD_CODE, MEDICAL_RECORD_NUMBER, HEALTH_PLAN_NUMBER,
    // Network
    IP_ADDRESS, IPV6_ADDRESS, MAC_ADDRESS, URL,
    // Vehicle
    VIN, LICENSE_PLATE,
    // Other
    ORGANIZATION, ACCOUNT_NUMBER,
    // Personal extended
    TITLE, JOB_TITLE, NATIONALITY,
    // Business
    COMPANY_NAME, DEPARTMENT,
    // Financial extended
    CURRENCY_CODE,
    // Network extended
    DOMAIN_NAME, USER_AGENT,
    // Location extended
    LATITUDE, LONGITUDE, TIME_ZONE,
    // Data utilities
    BOOLEAN, LOREM, TIMESTAMP,
    // Composite / PK generators
    CONDITIONAL, PARTIAL_MASK, FORMAT_PRESERVING, SEQUENTIAL, RANDOM_INT,
    // Custom mapping strategies
    HASH, SCRAMBLE
}

enum class ConsistencyMode {
    RANDOM, CONSISTENT
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

    var presetId: Long? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var consistencyMode: ConsistencyMode = ConsistencyMode.RANDOM,

    var linkKey: String? = null,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}
