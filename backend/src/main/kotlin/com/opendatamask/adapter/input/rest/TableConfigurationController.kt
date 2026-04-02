package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.input.rest.dto.*
import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.application.service.PermissionService
import com.opendatamask.application.service.TableConfigurationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tables")
class TableConfigurationController(
    private val tableConfigurationService: TableConfigurationService,
    private val permissionService: PermissionService,
    private val userRepository: UserRepository
) {

    @PostMapping
    fun createTableConfiguration(
        @PathVariable workspaceId: Long,
        @Valid @RequestBody request: TableConfigurationRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<TableConfigurationResponse> {
        permissionService.requirePermission(getUserId(userDetails), workspaceId, WorkspacePermission.CONFIGURE_GENERATORS)
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
        @Valid @RequestBody request: TableConfigurationRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<TableConfigurationResponse> {
        permissionService.requirePermission(getUserId(userDetails), workspaceId, WorkspacePermission.CONFIGURE_GENERATORS)
        return ResponseEntity.ok(tableConfigurationService.updateTableConfiguration(workspaceId, tableId, request))
    }

    @DeleteMapping("/{tableId}")
    fun deleteTableConfiguration(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        permissionService.requirePermission(getUserId(userDetails), workspaceId, WorkspacePermission.CONFIGURE_GENERATORS)
        tableConfigurationService.deleteTableConfiguration(workspaceId, tableId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{tableId}/generators")
    fun addColumnGenerator(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long,
        @Valid @RequestBody request: ColumnGeneratorRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ColumnGeneratorResponse> {
        permissionService.requirePermission(getUserId(userDetails), workspaceId, WorkspacePermission.CONFIGURE_GENERATORS)
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
        @Valid @RequestBody request: ColumnGeneratorRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ColumnGeneratorResponse> {
        permissionService.requirePermission(getUserId(userDetails), workspaceId, WorkspacePermission.CONFIGURE_GENERATORS)
        return ResponseEntity.ok(
            tableConfigurationService.updateColumnGenerator(tableId, generatorId, request)
        )
    }

    @DeleteMapping("/{tableId}/generators/{generatorId}")
    fun deleteColumnGenerator(
        @PathVariable workspaceId: Long,
        @PathVariable tableId: Long,
        @PathVariable generatorId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        permissionService.requirePermission(getUserId(userDetails), workspaceId, WorkspacePermission.CONFIGURE_GENERATORS)
        tableConfigurationService.deleteColumnGenerator(tableId, generatorId)
        return ResponseEntity.noContent().build()
    }

    private fun getUserId(userDetails: UserDetails): Long =
        userRepository.findByUsername(userDetails.username)
            .orElseThrow { NoSuchElementException("User not found") }.id
}
