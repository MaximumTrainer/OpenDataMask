package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.CustomDataMappingUseCase
import com.opendatamask.domain.port.input.dto.BulkCustomDataMappingRequest
import com.opendatamask.domain.port.input.dto.CustomDataMappingRequest
import com.opendatamask.domain.port.input.dto.CustomDataMappingResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/mappings")
class CustomDataMappingController(
    private val customDataMappingService: CustomDataMappingUseCase
) {

    @PostMapping
    fun createMapping(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: CustomDataMappingRequest
    ): ResponseEntity<CustomDataMappingResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(customDataMappingService.createMapping(workspaceId, request))

    @GetMapping("/{mappingId}")
    fun getMapping(
        @PathVariable workspaceId: Long,
        @PathVariable mappingId: Long
    ): ResponseEntity<CustomDataMappingResponse> =
        ResponseEntity.ok(customDataMappingService.getMapping(workspaceId, mappingId))

    @GetMapping
    fun listMappings(
        @PathVariable workspaceId: Long,
        @RequestParam(required = false) connectionId: Long?,
        @RequestParam(required = false) tableName: String?
    ): ResponseEntity<List<CustomDataMappingResponse>> {
        val hasConnectionId = connectionId != null
        val hasTableName = !tableName.isNullOrBlank()
        if (hasConnectionId != hasTableName) {
            return ResponseEntity.badRequest().build()
        }
        val result = if (hasConnectionId && hasTableName) {
            customDataMappingService.listMappingsForTable(workspaceId, connectionId!!, tableName!!)
        } else {
            customDataMappingService.listMappings(workspaceId)
        }
        return ResponseEntity.ok(result)
    }

    @PutMapping("/{mappingId}")
    fun updateMapping(
        @PathVariable workspaceId: Long,
        @PathVariable mappingId: Long,
        @Valid @RequestBody request: CustomDataMappingRequest
    ): ResponseEntity<CustomDataMappingResponse> =
        ResponseEntity.ok(customDataMappingService.updateMapping(workspaceId, mappingId, request))

    @DeleteMapping("/{mappingId}")
    fun deleteMapping(
        @PathVariable workspaceId: Long,
        @PathVariable mappingId: Long
    ): ResponseEntity<Void> {
        customDataMappingService.deleteMapping(workspaceId, mappingId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/bulk")
    fun saveBulkMappings(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: BulkCustomDataMappingRequest
    ): ResponseEntity<List<CustomDataMappingResponse>> =
        ResponseEntity.ok(customDataMappingService.saveBulkMappings(workspaceId, request))
}
