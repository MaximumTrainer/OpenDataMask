package com.opendatamask.infrastructure.security

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

// Integration tests verifying that the security filter chains enforce the expected
// access-control rules:
//  - Unauthenticated requests to /api/** return HTTP 401 (not a redirect).
//  - Public endpoints (/api/auth/**, /actuator/health) are accessible without credentials.
//  - Authenticated requests (WithMockUser) to /api/** are permitted.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityFilterChainTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    // ── Unauthenticated access ──────────────────────────────────────────────

    @Test
    fun `unauthenticated GET to protected API endpoint returns 401`() {
        mockMvc.perform(get("/api/workspaces"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `unauthenticated GET to arbitrary api path returns 401`() {
        mockMvc.perform(get("/api/some/protected/resource"))
            .andExpect(status().isUnauthorized)
    }

    // ── Authenticated access ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser", roles = ["USER"])
    fun `authenticated request to API endpoint is permitted (not 401 or 403)`() {
        // A real endpoint may return 404 since we're not seeding data, but the
        // security layer must not return 401/403 for an authenticated user.
        val result = mockMvc.perform(get("/api/workspaces")).andReturn()
        val responseStatus = result.response.status
        Assertions.assertNotEquals(401, responseStatus, "Security should not block authenticated user with 401")
        Assertions.assertNotEquals(403, responseStatus, "Security should not block authenticated user with 403")
    }

    // ── Public endpoints ────────────────────────────────────────────────────

    @Test
    fun `actuator health is accessible without authentication`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
    }
}
