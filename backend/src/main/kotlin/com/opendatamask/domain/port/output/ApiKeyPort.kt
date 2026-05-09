package com.opendatamask.domain.port.output

import com.opendatamask.domain.model.ApiKey
import java.util.Optional

interface ApiKeyPort {
    fun save(apiKey: ApiKey): ApiKey
    fun findById(id: Long): Optional<ApiKey>
    fun findByCreatedBy(userId: Long): List<ApiKey>
    fun findAll(): List<ApiKey>
    fun deleteById(id: Long)
    // Finds all active keys; the caller matches the raw key against each hash
    fun findAllActive(): List<ApiKey>
}
