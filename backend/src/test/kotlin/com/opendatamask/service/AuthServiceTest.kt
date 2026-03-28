package com.opendatamask.service

import com.opendatamask.dto.LoginRequest
import com.opendatamask.dto.RegisterRequest
import com.opendatamask.model.UserRole
import com.opendatamask.repository.UserRepository
import com.opendatamask.security.JwtTokenProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @Test
    fun `register creates user with correct data`() {
        val request = RegisterRequest(
            username = "testuser",
            email = "testuser@example.com",
            password = "password123",
            role = UserRole.USER
        )

        val response = authService.register(request)

        assertNotNull(response.token)
        assertEquals("testuser", response.username)
        assertEquals("testuser@example.com", response.email)
        assertEquals(UserRole.USER, response.role)

        val savedUser = userRepository.findByUsername("testuser")
        assertTrue(savedUser.isPresent)
        assertTrue(passwordEncoder.matches("password123", savedUser.get().passwordHash))
    }

    @Test
    fun `register throws exception for duplicate username`() {
        val request = RegisterRequest(
            username = "duplicateuser",
            email = "first@example.com",
            password = "password123"
        )
        authService.register(request)

        val duplicateRequest = RegisterRequest(
            username = "duplicateuser",
            email = "second@example.com",
            password = "password456"
        )

        assertThrows<IllegalArgumentException> {
            authService.register(duplicateRequest)
        }
    }

    @Test
    fun `register throws exception for duplicate email`() {
        val request = RegisterRequest(
            username = "user1",
            email = "same@example.com",
            password = "password123"
        )
        authService.register(request)

        val duplicateRequest = RegisterRequest(
            username = "user2",
            email = "same@example.com",
            password = "password456"
        )

        assertThrows<IllegalArgumentException> {
            authService.register(duplicateRequest)
        }
    }

    @Test
    fun `login succeeds with correct credentials`() {
        val registerRequest = RegisterRequest(
            username = "loginuser",
            email = "loginuser@example.com",
            password = "password123"
        )
        authService.register(registerRequest)

        val loginRequest = LoginRequest(username = "loginuser", password = "password123")
        val response = authService.login(loginRequest)

        assertNotNull(response.token)
        assertEquals("loginuser", response.username)
        assertTrue(jwtTokenProvider.validateToken(response.token))
    }

    @Test
    fun `login throws exception with wrong password`() {
        val registerRequest = RegisterRequest(
            username = "loginuser2",
            email = "loginuser2@example.com",
            password = "correctpassword"
        )
        authService.register(registerRequest)

        val loginRequest = LoginRequest(username = "loginuser2", password = "wrongpassword")

        assertThrows<IllegalArgumentException> {
            authService.login(loginRequest)
        }
    }

    @Test
    fun `login throws exception for non-existent user`() {
        val loginRequest = LoginRequest(username = "nonexistent", password = "password")

        assertThrows<IllegalArgumentException> {
            authService.login(loginRequest)
        }
    }

    @Test
    fun `register creates admin user when role is ADMIN`() {
        val request = RegisterRequest(
            username = "adminuser",
            email = "admin@example.com",
            password = "adminpass123",
            role = UserRole.ADMIN
        )

        val response = authService.register(request)
        assertEquals(UserRole.ADMIN, response.role)
    }
}
