package com.opendatamask.application.service

import com.opendatamask.domain.model.ApiKey
import com.opendatamask.domain.port.output.ApiKeyPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import com.opendatamask.domain.port.input.dto.CreateApiKeyRequest
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class ApiKeyServiceTest {

    @Mock private lateinit var apiKeyPort: ApiKeyPort
    @Mock private lateinit var passwordEncoder: PasswordEncoder
    @InjectMocks private lateinit var apiKeyService: ApiKeyService

    @Test
    fun `should generate a key with odm_ prefix when creating an API key`() {
        val request = CreateApiKeyRequest(name = "CI pipeline")
        whenever(passwordEncoder.encode(any())).thenReturn("hashed")
        whenever(apiKeyPort.save(any())).thenAnswer { it.arguments[0] as ApiKey }

        val result = apiKeyService.createApiKey(request, createdBy = 1L)

        assertTrue(result.key.startsWith("odm_"), "Key should start with odm_ prefix")
    }

    @Test
    fun `should store hashed key and prefix but never the raw key`() {
        val request = CreateApiKeyRequest(name = "My key")
        whenever(passwordEncoder.encode(any())).thenReturn("bcrypt-hash")
        whenever(apiKeyPort.save(any())).thenAnswer { it.arguments[0] as ApiKey }

        val result = apiKeyService.createApiKey(request, createdBy = 1L)

        verify(apiKeyPort).save(argThat { key ->
            key.keyHash == "bcrypt-hash" && key.keyPrefix == result.key.take(8)
        })
        assertNotEquals(result.key, result.apiKey.keyPrefix)
    }

    @Test
    fun `should associate key with workspace when workspaceId is provided`() {
        val request = CreateApiKeyRequest(name = "Scoped key", workspaceId = 42L)
        whenever(passwordEncoder.encode(any())).thenReturn("hash")
        whenever(apiKeyPort.save(any())).thenAnswer { it.arguments[0] as ApiKey }

        apiKeyService.createApiKey(request, createdBy = 1L)

        verify(apiKeyPort).save(argThat { key -> key.workspaceId == 42L })
    }

    @Test
    fun `should set expiry when expiresAt is provided`() {
        val expiry = Instant.now().plusSeconds(86400)
        val request = CreateApiKeyRequest(name = "Expiring key", expiresAt = expiry)
        whenever(passwordEncoder.encode(any())).thenReturn("hash")
        whenever(apiKeyPort.save(any())).thenAnswer { it.arguments[0] as ApiKey }

        apiKeyService.createApiKey(request, createdBy = 1L)

        verify(apiKeyPort).save(argThat { key -> key.expiresAt == expiry })
    }

    @Test
    fun `should list all keys for admin users`() {
        val keys = listOf(makeApiKey(id = 1L, createdBy = 1L), makeApiKey(id = 2L, createdBy = 2L))
        whenever(apiKeyPort.findAll()).thenReturn(keys)

        val result = apiKeyService.listApiKeys(userId = 1L, isAdmin = true)

        assertEquals(2, result.size)
        verify(apiKeyPort).findAll()
        verify(apiKeyPort, never()).findByCreatedBy(any())
    }

    @Test
    fun `should list only own keys for non-admin users`() {
        val keys = listOf(makeApiKey(id = 1L, createdBy = 99L))
        whenever(apiKeyPort.findByCreatedBy(99L)).thenReturn(keys)

        val result = apiKeyService.listApiKeys(userId = 99L, isAdmin = false)

        assertEquals(1, result.size)
        verify(apiKeyPort).findByCreatedBy(99L)
        verify(apiKeyPort, never()).findAll()
    }

    @Test
    fun `should revoke key by setting revokedAt`() {
        val key = makeApiKey(id = 1L)
        whenever(apiKeyPort.findById(1L)).thenReturn(Optional.of(key))
        whenever(apiKeyPort.save(any())).thenAnswer { it.arguments[0] as ApiKey }

        apiKeyService.revokeApiKey(1L)

        verify(apiKeyPort).save(argThat { k -> k.revokedAt != null })
    }

    @Test
    fun `should return null when key is not found in findAndValidateKey`() {
        whenever(apiKeyPort.findAllActive()).thenReturn(emptyList())

        val result = apiKeyService.findAndValidateKey("odm_invalid_key")

        assertNull(result)
    }

    @Test
    fun `should return matching key when raw key matches a stored hash`() {
        val key = makeApiKey(id = 1L, keyHash = "stored-hash")
        whenever(apiKeyPort.findAllActive()).thenReturn(listOf(key))
        whenever(passwordEncoder.matches("odm_rawkey", "stored-hash")).thenReturn(true)

        val result = apiKeyService.findAndValidateKey("odm_rawkey")

        assertNotNull(result)
        assertEquals(1L, result!!.id)
    }

    @Test
    fun `should throw when revoking a non-existent key`() {
        whenever(apiKeyPort.findById(999L)).thenReturn(Optional.empty())

        assertThrows(NoSuchElementException::class.java) {
            apiKeyService.revokeApiKey(999L)
        }
    }

    private fun makeApiKey(
        id: Long = 1L,
        createdBy: Long = 1L,
        keyHash: String = "hash"
    ) = ApiKey(
        id = id,
        name = "Test key",
        keyPrefix = "odm_test",
        keyHash = keyHash,
        createdBy = createdBy
    )
}
