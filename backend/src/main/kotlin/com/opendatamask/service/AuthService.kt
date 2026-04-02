package com.opendatamask.service

import com.opendatamask.dto.AuthResponse
import com.opendatamask.dto.LoginRequest
import com.opendatamask.dto.RegisterRequest
import com.opendatamask.domain.model.User
import com.opendatamask.domain.model.UserRole
import com.opendatamask.repository.UserRepository
import com.opendatamask.security.JwtTokenProvider
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username '${request.username}' is already taken")
        }
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email '${request.email}' is already registered")
        }

        val user = User(
            username = request.username,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password),
            role = UserRole.USER
        )
        userRepository.save(user)

        val token = jwtTokenProvider.generateToken(user.username)
        return AuthResponse(
            token = token,
            username = user.username,
            email = user.email,
            role = user.role
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByUsername(request.username)
            .orElseThrow { IllegalArgumentException("Invalid username or password") }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid username or password")
        }

        val token = jwtTokenProvider.generateToken(user.username)
        return AuthResponse(
            token = token,
            username = user.username,
            email = user.email,
            role = user.role
        )
    }
}
