package com.opendatamask.application.service

import com.opendatamask.domain.model.User
import com.opendatamask.domain.model.UserRole
import com.opendatamask.domain.port.input.dto.AuthResponse
import com.opendatamask.domain.port.output.TokenPort
import com.opendatamask.domain.port.output.UserPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OidcUserMappingService(
    private val userPort: UserPort,
    private val tokenPort: TokenPort
) {

    @Transactional
    fun resolveUser(email: String, preferredUsername: String, roles: List<String>): AuthResponse {
        val existing = userPort.findByEmail(email)
        if (existing.isPresent) {
            val user = existing.get()
            val token = tokenPort.generateToken(user.username)
            return AuthResponse(token = token, userId = user.id, username = user.username, email = user.email, role = user.role)
        }

        val username = resolveUniqueUsername(preferredUsername)
        val role = when {
            roles.any { it.equals("admin", ignoreCase = true) } -> UserRole.ADMIN
            roles.any { it.equals("viewer", ignoreCase = true) } -> UserRole.VIEWER
            else -> UserRole.USER
        }

        val user = User(
            username = username,
            email = email,
            passwordHash = UUID.randomUUID().toString(),
            role = role
        )
        val saved = userPort.save(user)
        val token = tokenPort.generateToken(saved.username)
        return AuthResponse(token = token, userId = saved.id, username = saved.username, email = saved.email, role = saved.role)
    }

    private fun resolveUniqueUsername(base: String): String {
        if (!userPort.existsByUsername(base)) return base
        val candidate = "${base}_oidc"
        return if (!userPort.existsByUsername(candidate)) candidate else "${base}_${UUID.randomUUID().toString().take(8)}"
    }
}
