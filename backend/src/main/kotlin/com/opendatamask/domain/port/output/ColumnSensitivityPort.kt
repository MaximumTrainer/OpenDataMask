package com.opendatamask.domain.port.output

import com.opendatamask.model.ColumnSensitivity

interface ColumnSensitivityPort {
    fun findByWorkspaceId(workspaceId: Long): List<ColumnSensitivity>
    fun findByWorkspaceIdAndTableNameAndColumnName(workspaceId: Long, tableName: String, columnName: String): ColumnSensitivity?
    fun save(sensitivity: ColumnSensitivity): ColumnSensitivity
    fun saveAll(sensitivities: List<ColumnSensitivity>): List<ColumnSensitivity>
    fun deleteById(id: Long)
}
