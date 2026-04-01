package com.opendatamask.security

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider

    private val secret = "test-jwt-secret-for-testing-only-not-production"
    private val expiration = 86400000L // 24 hours

    @BeforeEach
    fun setup() {
        jwtTokenProvider = JwtTokenProvider(secret, expiration)
    }

    @Test
    fun `generateToken returns non-blank token`() {
        val token = jwtTokenProvider.generateToken("alice")
        assertNotNull(token)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `getUsernameFromToken returns correct username`() {
        val token = jwtTokenProvider.generateToken("alice")
        assertEquals("alice", jwtTokenProvider.getUsernameFromToken(token))
    }

    @Test
    fun `validateToken returns true for a fresh token`() {
        val token = jwtTokenProvider.generateToken("bob")
        assertTrue(jwtTokenProvider.validateToken(token))
    }

    @Test
    fun `validateToken returns false for an expired token`() {
        val expiredProvider = JwtTokenProvider(secret, jwtExpiration = -1L)
        val token = expiredProvider.generateToken("charlie")
        assertFalse(expiredProvider.validateToken(token))
    }

    @Test
    fun `validateToken returns false for a tampered token`() {
        val token = jwtTokenProvider.generateToken("dave")
        val tampered = token.dropLast(5) + "XXXXX"
        assertFalse(jwtTokenProvider.validateToken(tampered))
    }

    @Test
    fun `generateToken produces different tokens for different users`() {
        val token1 = jwtTokenProvider.generateToken("user1")
        val token2 = jwtTokenProvider.generateToken("user2")
        assertNotEquals(token1, token2)
    }
}
