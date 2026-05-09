package com.opendatamask.infrastructure.security

import com.opendatamask.adapter.output.persistence.ApiKeyRepository
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.application.service.ApiKeyService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter

class ApiKeyAuthenticationFilter(
    private val apiKeyService: ApiKeyService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {

    companion object {
        const val API_KEY_HEADER = "X-API-Key"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val rawKey = request.getHeader(API_KEY_HEADER)

        if (rawKey != null && SecurityContextHolder.getContext().authentication == null) {
            val apiKey = apiKeyService.findAndValidateKey(rawKey)
            if (apiKey != null) {
                val user = userRepository.findById(apiKey.createdBy).orElse(null)
                if (user != null) {
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
                    val authentication = UsernamePasswordAuthenticationToken(user.username, null, authorities)
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                    // Record usage asynchronously to avoid blocking the request
                    apiKeyService.recordUsage(apiKey)
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
