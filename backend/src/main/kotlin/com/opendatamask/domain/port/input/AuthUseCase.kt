package com.opendatamask.domain.port.input

import com.opendatamask.dto.AuthResponse
import com.opendatamask.dto.LoginRequest
import com.opendatamask.dto.RegisterRequest

interface AuthUseCase {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
}
