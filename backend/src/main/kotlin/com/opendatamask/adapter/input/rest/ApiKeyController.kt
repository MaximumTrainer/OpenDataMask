package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.application.service.ApiKeyService
import com.opendatamask.domain.model.UserRole
import com.opendatamask.domain.port.input.dto.ApiKeyCreatedResponse
import com.opendatamask.domain.port.input.dto.ApiKeyResponse
import com.opendatamask.domain.port.input.dto.CreateApiKeyRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/api-keys")
@Tag(name = "API Keys", description = "Manage service account API keys")
class ApiKeyController(
    private val apiKeyService: ApiKeyService,
    private val userRepository: UserRepository
) {

    @PostMapping
    @Operation(summary = "Create a new API key")
    fun createApiKey(
        @RequestBody request: CreateApiKeyRequest,
        @AuthenticationPrincipal principal: UserDetails
    ): ResponseEntity<ApiKeyCreatedResponse> {
        val user = userRepository.findByUsername(principal.username)
            .orElseThrow { NoSuchElementException("User not found") }
        return ResponseEntity.status(HttpStatus.CREATED).body(apiKeyService.createApiKey(request, user.id))
    }

    @GetMapping
    @Operation(summary = "List API keys for the current user (admin sees all)")
    fun listApiKeys(@AuthenticationPrincipal principal: UserDetails): ResponseEntity<List<ApiKeyResponse>> {
        val user = userRepository.findByUsername(principal.username)
            .orElseThrow { NoSuchElementException("User not found") }
        val isAdmin = user.role == UserRole.ADMIN
        return ResponseEntity.ok(apiKeyService.listApiKeys(user.id, isAdmin))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single API key by ID")
    fun getApiKey(@PathVariable id: Long): ResponseEntity<ApiKeyResponse> =
        ResponseEntity.ok(apiKeyService.getApiKey(id))

    @PostMapping("/{id}/revoke")
    @Operation(summary = "Revoke an API key")
    fun revokeApiKey(@PathVariable id: Long): ResponseEntity<Void> {
        apiKeyService.revokeApiKey(id)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an API key permanently")
    fun deleteApiKey(@PathVariable id: Long): ResponseEntity<Void> {
        apiKeyService.deleteApiKey(id)
        return ResponseEntity.noContent().build()
    }
}
