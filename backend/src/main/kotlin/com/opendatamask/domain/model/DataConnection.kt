package com.opendatamask.domain.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class ConnectionType {
    POSTGRESQL, MONGODB, AZURE_SQL, MONGODB_COSMOS, FILE, MYSQL
}

@Entity
@Table(name = "data_connections")
class DataConnection(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var workspaceId: Long,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: ConnectionType,

    @Column(nullable = false, length = 2048)
    var connectionString: String,

    @Column
    var host: String? = null,

    @Column
    var username: String? = null,

    @Column(length = 2048)
    var password: String? = null,

    @Column
    var database: String? = null,

    @Column(nullable = false)
    var isSource: Boolean = false,

    @Column(nullable = false)
    var isDestination: Boolean = false,

    @Column(nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        createdAt = LocalDateTime.now()
    }
}
