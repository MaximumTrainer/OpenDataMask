package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.ColumnPreviewResult

interface DataPreviewUseCase {
    fun previewColumn(workspaceId: Long, tableName: String, columnName: String, sampleSize: Int = 5): ColumnPreviewResult
}
