package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.port.input.dto.AuthResponse
import com.opendatamask.domain.port.input.dto.LoginRequest
import com.opendatamask.domain.port.input.dto.RegisterRequest
import com.opendatamask.application.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
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

    // Returns the currently authenticated user's details.
    // Used by the frontend to verify an active session (JWT or SAML).
    // Returns 200 with username when authenticated, or 401 if no valid session exists.
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal principal: UserDetails?): ResponseEntity<Map<String, String>> {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        return ResponseEntity.ok(
            mapOf(
                "username" to principal.username,
                "authenticated" to "true"
            )
        )
    }
}

