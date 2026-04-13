package com.opendatamask.adapter.input.rest

import com.fasterxml.jackson.databind.ObjectMapper
import com.opendatamask.domain.port.input.dto.LoginRequest
import com.opendatamask.domain.port.input.dto.RegisterRequest
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
    fun `GET api-auth-me returns 200 with username for authenticated user`() {
        val username = "mectrl_${System.nanoTime()}"
        val email = "mectrl_${System.nanoTime()}@example.com"
        val password = "password123"

        val registerRequest = RegisterRequest(username = username, email = email, password = password)
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(registerRequest)
        }.andExpect { status { isCreated() } }

        val loginResult = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest(username = username, password = password))
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { exists() }
        }.andReturn()

        val token = objectMapper.readTree(loginResult.response.contentAsString)["token"].asText()

        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
                .header("Authorization", "Bearer $token")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.username").value(username)
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.authenticated").value("true")
        )
    }

    @Test
    fun `GET api-auth-me returns 401 for unauthenticated request`() {
        mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/auth/me")
        ).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized
        )
    }
}

