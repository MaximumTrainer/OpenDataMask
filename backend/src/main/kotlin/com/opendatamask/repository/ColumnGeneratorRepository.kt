package com.opendatamask.repository

import com.opendatamask.domain.model.ColumnGenerator
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ColumnGeneratorRepository : JpaRepository<ColumnGenerator, Long> {
    fun findByTableConfigurationId(tableConfigurationId: Long): List<ColumnGenerator>
    fun deleteByTableConfigurationId(tableConfigurationId: Long)
}
