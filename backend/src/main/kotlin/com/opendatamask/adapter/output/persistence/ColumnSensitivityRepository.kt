package com.opendatamask.adapter.output.persistence

import com.opendatamask.domain.model.ColumnSensitivity
import com.opendatamask.domain.port.output.ColumnSensitivityPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ColumnSensitivityRepository : JpaRepository<ColumnSensitivity, Long>, ColumnSensitivityPort {
    override fun findByWorkspaceId(workspaceId: Long): List<ColumnSensitivity>
    override fun findByWorkspaceIdAndTableNameAndColumnName(
        workspaceId: Long,
        tableName: String,
        columnName: String
    ): ColumnSensitivity?
    override fun save(sensitivity: ColumnSensitivity): ColumnSensitivity
    override fun deleteById(id: Long)
}
