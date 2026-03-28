package com.opendatamask.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class LogLevel {
    INFO, WARN, ERROR
}

@Entity
@Table(name = "job_logs")
class JobLog(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var jobId: Long,

    @Column(nullable = false, length = 8192)
    var message: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var level: LogLevel = LogLevel.INFO,

    @Column(nullable = false)
    var timestamp: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        timestamp = LocalDateTime.now()
    }
}
