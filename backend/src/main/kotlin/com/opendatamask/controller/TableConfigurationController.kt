package com.opendatamask.controller

import com.opendatamask.dto.*
import com.opendatamask.service.TableConfigurationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tables")
class TableConfigurationController(
    private val tableConfigurationService: TableConfigurationService
) {

    @PostMapping
    fun createTableConfiguration(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: TableConfigurationRequest
    ): ResponseEntity<TableConfigurationResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tableConfigurationService.createTableConfiguration(workspaceId, request))
    }

    @GetMapping("/{tableId}")
    fun getTableConfiguration(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long
    ): ResponseEntity<TableConfigurationResponse> {
        return ResponseEntity.ok(tableConfigurationService.getTableConfiguration(workspaceId, tableId))
    }

    @GetMapping
    fun listTableConfigurations(@PathVariable workspaceId: Long): ResponseEntity<List<TableConfigurationResponse>> {
        return ResponseEntity.ok(tableConfigurationService.listTableConfigurations(workspaceId))
    }

    @PutMapping("/{tableId}")
    fun updateTableConfiguration(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long,
        @Valid @RequestBody request: TableConfigurationRequest
    ): ResponseEntity<TableConfigurationResponse> {
        return ResponseEntity.ok(tableConfigurationService.updateTableConfiguration(workspaceId, tableId, request))
    }

    @DeleteMapping("/{tableId}")
    fun deleteTableConfiguration(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long
    ): ResponseEntity<Void> {
        tableConfigurationService.deleteTableConfiguration(workspaceId, tableId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{tableId}/generators")
    fun addColumnGenerator(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long,
        @Valid @RequestBody request: ColumnGeneratorRequest
    ): ResponseEntity<ColumnGeneratorResponse> {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(tableConfigurationService.addColumnGenerator(tableId, request))
    }

    @GetMapping("/{tableId}/generators")
    fun listColumnGenerators(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long
    ): ResponseEntity<List<ColumnGeneratorResponse>> {
        return ResponseEntity.ok(tableConfigurationService.listColumnGenerators(tableId))
    }

    @PutMapping("/{tableId}/generators/{generatorId}")
    fun updateColumnGenerator(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long,
        @PathVariable generatorId: Long,
        @Valid @RequestBody request: ColumnGeneratorRequest
    ): ResponseEntity<ColumnGeneratorResponse> {
        return ResponseEntity.ok(
            tableConfigurationService.updateColumnGenerator(tableId, generatorId, request)
        )
    }

    @DeleteMapping("/{tableId}/generators/{generatorId}")
    fun deleteColumnGenerator(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long,
        @PathVariable generatorId: Long
    ): ResponseEntity<Void> {
        tableConfigurationService.deleteColumnGenerator(tableId, generatorId)
        return ResponseEntity.noContent().build()
    }
}
