package com.opendatamask.domain.port.input

import com.opendatamask.domain.port.input.dto.AuthResponse
import com.opendatamask.domain.port.input.dto.LoginRequest
import com.opendatamask.domain.port.input.dto.RegisterRequest

interface AuthUseCase {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
}
