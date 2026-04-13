package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.AuthResponse
import com.opendatamask.domain.port.input.dto.LoginRequest
import com.opendatamask.domain.port.input.dto.RegisterRequest
import com.opendatamask.application.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> {
        return ResponseEntity.ok(authService.login(request))
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))
    }

    // Returns the currently authenticated user's name.
    // Works for both JWT (principal is a UserDetails) and SAML (principal is a
    // Saml2AuthenticatedPrincipal) since both implement Authentication.getName().
    // The /api/auth/** path is permitAll() so unauthenticated requests reach this endpoint;
    // in that case authentication is null or not authenticated and we return 401 explicitly.
    @GetMapping("/me")
    fun me(authentication: Authentication?): ResponseEntity<Map<String, String>> {
        if (authentication == null || !authentication.isAuthenticated) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        return ResponseEntity.ok(
            mapOf(
                "username" to authentication.name,
                "authenticated" to "true"
            )
        )
    }
}

