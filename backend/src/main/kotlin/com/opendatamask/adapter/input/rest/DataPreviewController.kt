package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.input.rest.dto.ColumnPreviewResult
import com.opendatamask.application.service.DataPreviewService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tables/{tableName}/columns/{columnName}")
class DataPreviewController(
    private val dataPreviewService: DataPreviewService
) {

    @GetMapping("/preview")
    fun previewColumn(
        @PathVariable workspaceId: Long,
        @PathVariable tableName: String,
        @PathVariable columnName: String,
        @RequestParam(defaultValue = "5") sampleSize: Int
    ): ResponseEntity<ColumnPreviewResult> =
        ResponseEntity.ok(
            dataPreviewService.previewColumn(workspaceId, tableName, columnName, sampleSize)
        )
}
