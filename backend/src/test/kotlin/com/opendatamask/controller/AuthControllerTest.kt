package com.opendatamask.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.opendatamask.dto.LoginRequest
import com.opendatamask.dto.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `POST api-auth-register returns 201 with token`() {
        val request = RegisterRequest(
            username = "ctrluser_${System.nanoTime()}",
            email = "ctrluser_${System.nanoTime()}@example.com",
            password = "password123"
        )

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.token") { exists() }
            jsonPath("$.username") { value(request.username) }
            jsonPath("$.email") { value(request.email) }
            jsonPath("$.role") { value("USER") }
        }
    }

    @Test
    fun `POST api-auth-register returns 400 for invalid data`() {
        val request = mapOf(
            "username" to "ab",
            "email" to "notanemail",
            "password" to "short"
        )

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST api-auth-login returns 200 with token`() {
        val username = "loginctrl_${System.nanoTime()}"
        val email = "loginctrl_${System.nanoTime()}@example.com"
        val password = "password123"

        val registerRequest = RegisterRequest(username = username, email = email, password = password)
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(registerRequest)
        }.andExpect { status { isCreated() } }

        val loginRequest = LoginRequest(username = username, password = password)
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { exists() }
            jsonPath("$.username") { value(username) }
        }
    }

    @Test
    fun `POST api-auth-login returns 400 for wrong password`() {
        val username = "wrongpass_${System.nanoTime()}"
        val email = "wrongpass_${System.nanoTime()}@example.com"

        val registerRequest = RegisterRequest(username = username, email = email, password = "correctpass")
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(registerRequest)
        }.andExpect { status { isCreated() } }

        val loginRequest = LoginRequest(username = username, password = "wrongpassword")
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginRequest)
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `POST api-auth-register returns 400 for duplicate username`() {
        val username = "dupctrl_${System.nanoTime()}"

        val request1 = RegisterRequest(username = username, email = "first_${System.nanoTime()}@example.com", password = "password123")
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request1)
        }.andExpect { status { isCreated() } }

        val request2 = RegisterRequest(username = username, email = "second_${System.nanoTime()}@example.com", password = "password456")
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request2)
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
