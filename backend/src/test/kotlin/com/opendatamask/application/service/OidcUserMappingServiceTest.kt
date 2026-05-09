package com.opendatamask.application.service

import com.opendatamask.domain.model.User
import com.opendatamask.domain.model.UserRole
import com.opendatamask.domain.port.output.TokenPort
import com.opendatamask.domain.port.output.UserPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class OidcUserMappingServiceTest {

    @Mock private lateinit var userPort: UserPort
    @Mock private lateinit var tokenPort: TokenPort

    @InjectMocks private lateinit var service: OidcUserMappingService

    @Test
    fun `should return existing user JWT when OIDC email already registered`() {
        val existing = User(id = 5L, username = "alice", email = "alice@example.com", passwordHash = "x")
        whenever(userPort.findByEmail("alice@example.com")).thenReturn(Optional.of(existing))
        whenever(tokenPort.generateToken("alice")).thenReturn("jwt-token")

        val result = service.resolveUser("alice@example.com", "alice", emptyList())

        assertThat(result.token).isEqualTo("jwt-token")
        assertThat(result.userId).isEqualTo(5L)
        assertThat(result.username).isEqualTo("alice")
    }

    @Test
    fun `should create new USER-role account on first OIDC login with no roles claim`() {
        whenever(userPort.findByEmail("new@example.com")).thenReturn(Optional.empty())
        whenever(userPort.existsByUsername("new")).thenReturn(false)
        val saved = User(id = 10L, username = "new", email = "new@example.com", passwordHash = "hashed")
        whenever(userPort.save(any())).thenReturn(saved)
        whenever(tokenPort.generateToken("new")).thenReturn("new-token")

        val result = service.resolveUser("new@example.com", "new", emptyList())

        assertThat(result.token).isEqualTo("new-token")
        assertThat(result.role).isEqualTo(UserRole.USER)
    }

    @Test
    fun `should assign ADMIN role when OIDC roles claim contains admin`() {
        whenever(userPort.findByEmail("boss@example.com")).thenReturn(Optional.empty())
        whenever(userPort.existsByUsername("boss")).thenReturn(false)
        val saved = User(id = 11L, username = "boss", email = "boss@example.com", passwordHash = "hashed", role = UserRole.ADMIN)
        whenever(userPort.save(any())).thenReturn(saved)
        whenever(tokenPort.generateToken("boss")).thenReturn("admin-token")

        val result = service.resolveUser("boss@example.com", "boss", listOf("admin"))

        assertThat(result.role).isEqualTo(UserRole.ADMIN)
    }

    @Test
    fun `should deduplicate username when OIDC name conflicts with existing local account`() {
        whenever(userPort.findByEmail("dup@example.com")).thenReturn(Optional.empty())
        whenever(userPort.existsByUsername("dupuser")).thenReturn(true)
        whenever(userPort.existsByUsername("dupuser_oidc")).thenReturn(false)
        val saved = User(id = 12L, username = "dupuser_oidc", email = "dup@example.com", passwordHash = "hashed")
        whenever(userPort.save(any())).thenReturn(saved)
        whenever(tokenPort.generateToken("dupuser_oidc")).thenReturn("tok")

        val result = service.resolveUser("dup@example.com", "dupuser", emptyList())

        assertThat(result.username).isEqualTo("dupuser_oidc")
    }

    @Test
    fun `should assign VIEWER role when OIDC roles claim contains viewer`() {
        whenever(userPort.findByEmail("viewer@example.com")).thenReturn(Optional.empty())
        whenever(userPort.existsByUsername("viewer")).thenReturn(false)
        val saved = User(id = 13L, username = "viewer", email = "viewer@example.com", passwordHash = "hashed", role = UserRole.VIEWER)
        whenever(userPort.save(any())).thenReturn(saved)
        whenever(tokenPort.generateToken("viewer")).thenReturn("viewer-tok")

        val result = service.resolveUser("viewer@example.com", "viewer", listOf("viewer"))

        assertThat(result.role).isEqualTo(UserRole.VIEWER)
    }
}
