package com.opendatamask.adapter.input.rest

import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.application.service.ApiKeyService
import com.opendatamask.domain.model.User
import com.opendatamask.domain.model.UserRole
import com.opendatamask.domain.port.input.dto.ApiKeyCreatedResponse
import com.opendatamask.domain.port.input.dto.ApiKeyResponse
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.User as SpringUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.Optional

@WebMvcTest(
    ApiKeyController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(ApiKeyControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class ApiKeyControllerTest {

    @TestConfiguration
    @EnableWebSecurity
    class TestSecurityConfig {
        @Bean
        fun testSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
            http.csrf { it.disable() }
                .authorizeHttpRequests { it.anyRequest().permitAll() }
            return http.build()
        }
    }

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var apiKeyService: ApiKeyService
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeOdmUser(id: Long = 1L) = User(
        id = id, username = "alice", email = "alice@example.com",
        passwordHash = "hash", role = UserRole.USER
    )

    private fun makeApiKeyResponse(id: Long = 1L) = ApiKeyResponse(
        id = id,
        name = "CI/CD key",
        keyPrefix = "odmk1234",
        createdBy = 1L,
        workspaceId = null,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        expiresAt = null,
        revokedAt = null,
        lastUsedAt = null,
        isActive = true
    )

    @Test
    fun `POST create API key returns 201 with key and metadata`() {
        val odmUser = makeOdmUser()
        val apiKeyResponse = makeApiKeyResponse()
        val created = ApiKeyCreatedResponse(key = "odmk1234abcdefghijklmnop", apiKey = apiKeyResponse)

        whenever(userRepository.findByUsername("alice")).thenReturn(Optional.of(odmUser))
        whenever(apiKeyService.createApiKey(any(), eq(1L))).thenReturn(created)

        mockMvc.perform(
            post("/api/api-keys")
                .with(user("alice"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"CI/CD key"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.key").value("odmk1234abcdefghijklmnop"))
            .andExpect(jsonPath("$.apiKey.name").value("CI/CD key"))
            .andExpect(jsonPath("$.apiKey.keyPrefix").value("odmk1234"))
            .andExpect(jsonPath("$.apiKey.isActive").value(true))
    }

    @Test
    fun `GET list API keys returns 200 with list`() {
        val odmUser = makeOdmUser()
        whenever(userRepository.findByUsername("alice")).thenReturn(Optional.of(odmUser))
        whenever(apiKeyService.listApiKeys(1L, false)).thenReturn(listOf(makeApiKeyResponse(1L), makeApiKeyResponse(2L)))

        mockMvc.perform(get("/api/api-keys").with(user("alice")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("CI/CD key"))
    }

    @Test
    fun `GET list API keys as admin returns all keys`() {
        val adminUser = makeOdmUser().apply { role = UserRole.ADMIN }
        whenever(userRepository.findByUsername("alice")).thenReturn(Optional.of(adminUser))
        whenever(apiKeyService.listApiKeys(1L, true)).thenReturn(listOf(makeApiKeyResponse()))

        mockMvc.perform(get("/api/api-keys").with(user("alice")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))

        verify(apiKeyService).listApiKeys(1L, true)
    }

    @Test
    fun `GET single API key returns 200`() {
        whenever(apiKeyService.getApiKey(1L)).thenReturn(makeApiKeyResponse())

        mockMvc.perform(get("/api/api-keys/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.isActive").value(true))
    }

    @Test
    fun `POST revoke API key returns 204`() {
        doNothing().whenever(apiKeyService).revokeApiKey(1L)

        mockMvc.perform(post("/api/api-keys/1/revoke"))
            .andExpect(status().isNoContent)

        verify(apiKeyService).revokeApiKey(1L)
    }

    @Test
    fun `DELETE API key returns 204`() {
        doNothing().whenever(apiKeyService).deleteApiKey(1L)

        mockMvc.perform(delete("/api/api-keys/1"))
            .andExpect(status().isNoContent)

        verify(apiKeyService).deleteApiKey(1L)
    }
}
