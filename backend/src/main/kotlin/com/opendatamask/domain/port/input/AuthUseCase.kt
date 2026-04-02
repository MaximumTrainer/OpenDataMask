package com.opendatamask.domain.port.input

import com.opendatamask.adapter.input.rest.dto.AuthResponse
import com.opendatamask.adapter.input.rest.dto.LoginRequest
import com.opendatamask.adapter.input.rest.dto.RegisterRequest

interface AuthUseCase {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
}
