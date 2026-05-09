package com.opendatamask.infrastructure.config

import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.application.service.ApiKeyService
import com.opendatamask.domain.port.output.ApiKeyPort
import com.opendatamask.infrastructure.security.ApiKeyAuthenticationFilter
import com.opendatamask.infrastructure.security.JwtAuthenticationFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val userDetailsService: UserDetailsService,
    private val corsConfig: CorsConfig,
    private val passwordEncoder: PasswordEncoder
) {

    @Bean
    @ConditionalOnBean(ApiKeyPort::class)
    fun apiKeyAuthenticationFilter(apiKeyService: ApiKeyService, userRepository: UserRepository): ApiKeyAuthenticationFilter =
        ApiKeyAuthenticationFilter(apiKeyService, userRepository)

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return provider
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    // API security filter chain (highest priority, order=1): handles /api/** endpoints.
    // Uses JWT Bearer tokens. Returns HTTP 401 for unauthenticated requests instead of
    // redirecting to an IdP. CSRF is disabled since JWT-only requests use Bearer tokens.
    // Session policy is NEVER: does not create new sessions but will read an existing one
    // (e.g. a SAML session initiated by the browser-based flow), allowing SAML-authenticated
    // users to call API endpoints using their session cookie.
    @Bean
    @Order(1)
    fun apiSecurityFilterChain(http: HttpSecurity, apiKeyFilters: List<ApiKeyAuthenticationFilter>): SecurityFilterChain {
        http
            .securityMatcher("/api/**")
            .cors { it.configurationSource(corsConfig.corsConfigurationSource()) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.NEVER) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/docs/**", "/api/swagger-ui/**", "/api/swagger-ui.html").permitAll()
                    .requestMatchers("/api/**").authenticated()
            }
            .exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            }
            .authenticationProvider(authenticationProvider())
        apiKeyFilters.forEach { filter ->
            http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter::class.java)
        }
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    // Fallback web security filter chain (lowest priority, order=4): handles all non-API routes.
    // Protects against CSRF using a cookie-based token repository so the SPA can read the
    // XSRF-TOKEN cookie and send it back via X-XSRF-TOKEN on mutating requests.
    // When OidcSecurityConfig is active (order=2) it intercepts OIDC redirect paths before this chain.
    // When SamlSecurityConfig is active (order=3) it intercepts browser routes before this chain.
    // The /saml2/**, /login/**, /error matchers here are the fallback for when SAML is NOT
    // active; they mirror the SamlSecurityConfig matchers intentionally so those paths are
    // always accessible regardless of whether the SAML SP library is on the classpath.
    @Bean
    @Order(4)
    fun webSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfig.corsConfigurationSource()) }
            .csrf { it.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers(
                        AntPathRequestMatcher("/saml2/**"),
                        AntPathRequestMatcher("/login/**"),
                        AntPathRequestMatcher("/error")
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            .formLogin { it.disable() }

        return http.build()
    }
}

