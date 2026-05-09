package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.AuditLog
import com.opendatamask.domain.port.output.AuditLogPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AuditLogRepository : JpaRepository<AuditLog, Long>, AuditLogPort {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.workspaceId = :workspaceId
          AND (:from IS NULL OR a.timestamp >= :from)
          AND (:to IS NULL OR a.timestamp <= :to)
        ORDER BY a.timestamp DESC
        LIMIT :limit
    """)
    override fun findByWorkspaceId(
        @Param("workspaceId") workspaceId: Long,
        @Param("from") from: Instant?,
        @Param("to") to: Instant?,
        @Param("limit") limit: Int
    ): List<AuditLog>

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:from IS NULL OR a.timestamp >= :from)
          AND (:to IS NULL OR a.timestamp <= :to)
        ORDER BY a.timestamp DESC
        LIMIT :limit
    """)
    override fun findAll(
        @Param("from") from: Instant?,
        @Param("to") to: Instant?,
        @Param("limit") limit: Int
    ): List<AuditLog>
}
