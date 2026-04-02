package com.opendatamask.repository

import com.opendatamask.domain.model.InheritedConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface InheritedConfigRepository : JpaRepository<InheritedConfig, Long> {
    fun findByChildWorkspaceId(childWorkspaceId: Long): List<InheritedConfig>
    fun findByChildWorkspaceIdAndTableName(childWorkspaceId: Long, tableName: String): List<InheritedConfig>
    fun deleteByChildWorkspaceId(childWorkspaceId: Long)
}
