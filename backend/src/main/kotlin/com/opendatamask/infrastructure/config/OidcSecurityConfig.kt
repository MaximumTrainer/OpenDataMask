package com.opendatamask.infrastructure.config

import com.opendatamask.application.service.OidcUserMappingService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

// OIDC/OAuth2 Single Sign-On security filter chain.
//
// Activated only when the following env vars are set:
//   OIDC_ISSUER_URI       – IdP discovery URL (e.g. https://accounts.google.com)
//   OIDC_CLIENT_ID        – OAuth2 client ID
//   OIDC_CLIENT_SECRET    – OAuth2 client secret
//
// After a successful OIDC login, Spring redirects to the frontend callback URL with
// an ODM JWT token as a query parameter: /auth/callback?token=<jwt>
//
// To enable OIDC SSO, set the env vars above; they are mapped to:
//   spring.security.oauth2.client.registration.oidc.issuer-uri
//   spring.security.oauth2.client.registration.oidc.client-id
//   spring.security.oauth2.client.registration.oidc.client-secret
@Configuration
@ConditionalOnProperty(
    name = ["spring.security.oauth2.client.registration.oidc.issuer-uri"],
    matchIfMissing = false
)
class OidcSecurityConfig(
    private val corsConfig: CorsConfig,
    private val oidcUserMappingService: OidcUserMappingService
) {

    @Bean
    @Order(2)
    fun oidcSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfig.corsConfigurationSource()) }
            .csrf { it.disable() }
            .securityMatcher("/oauth2/**", "/login/oauth2/**", "/api/auth/oidc/**")
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
            .oauth2Login { oauth2 ->
                oauth2.successHandler(oidcSuccessHandler())
            }

        return http.build()
    }

    @Bean
    fun oidcSuccessHandler(): AuthenticationSuccessHandler =
        AuthenticationSuccessHandler { request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication ->
            val oidcUser = authentication.principal as OidcUser
            val email = oidcUser.email ?: ""
            val preferredUsername = oidcUser.preferredUsername ?: oidcUser.name ?: email.substringBefore('@')
            @Suppress("UNCHECKED_CAST")
            val roles: List<String> = (oidcUser.claims["roles"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

            val authResponse = oidcUserMappingService.resolveUser(email, preferredUsername, roles)
            val frontendBase = request.getHeader("Origin") ?: "http://localhost:5173"
            response.sendRedirect("$frontendBase/auth/callback?token=${authResponse.token}")
        }
}
