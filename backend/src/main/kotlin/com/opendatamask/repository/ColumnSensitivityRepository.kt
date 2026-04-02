package com.opendatamask.repository

import com.opendatamask.domain.model.ColumnSensitivity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ColumnSensitivityRepository : JpaRepository<ColumnSensitivity, Long> {
    fun findByWorkspaceId(workspaceId: Long): List<ColumnSensitivity>
    fun findByWorkspaceIdAndTableNameAndColumnName(
        workspaceId: Long,
        tableName: String,
        columnName: String
    ): ColumnSensitivity?
}
