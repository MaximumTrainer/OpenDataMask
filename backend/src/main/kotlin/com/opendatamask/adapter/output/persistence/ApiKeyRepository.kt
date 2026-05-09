package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.ApiKey
import com.opendatamask.domain.port.output.ApiKeyPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ApiKeyRepository : JpaRepository<ApiKey, Long>, ApiKeyPort {
    override fun findById(id: Long): Optional<ApiKey>
    override fun findByCreatedBy(createdBy: Long): List<ApiKey>

    @Query("SELECT a FROM ApiKey a WHERE a.revokedAt IS NULL AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP)")
    override fun findAllActive(): List<ApiKey>
}
