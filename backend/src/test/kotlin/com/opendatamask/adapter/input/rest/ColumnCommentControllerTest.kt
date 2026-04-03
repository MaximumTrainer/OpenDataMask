package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.ColumnComment
import com.opendatamask.domain.model.User
import com.opendatamask.domain.model.UserRole
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.application.service.ColumnCommentService
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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.util.Optional

@WebMvcTest(
    ColumnCommentController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(ColumnCommentControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class ColumnCommentControllerTest {

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

    @MockBean private lateinit var commentService: ColumnCommentService
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeComment() = ColumnComment(
        id = 1L,
        workspaceId = 1L,
        tableName = "users",
        columnName = "email",
        userId = 1L,
        comment = "test comment",
        createdAt = LocalDateTime.now()
    )

    private fun makeUser() = User(
        id = 1L,
        username = "alice",
        email = "alice@test.com",
        passwordHash = "hashed",
        role = UserRole.USER
    )

    @Test
    fun `GET comments returns 200 with list`() {
        whenever(commentService.getComments(1L, "users", "email")).thenReturn(listOf(makeComment()))

        mockMvc.perform(get("/api/workspaces/1/tables/users/columns/email/comments"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].comment").value("test comment"))
    }

    @Test
    fun `GET returns empty list when no comments`() {
        whenever(commentService.getComments(1L, "users", "email")).thenReturn(emptyList())

        mockMvc.perform(get("/api/workspaces/1/tables/users/columns/email/comments"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    @WithMockUser(username = "alice")
    fun `POST add comment returns 201`() {
        val savedComment = makeComment()
        whenever(userRepository.findByUsername("alice")).thenReturn(Optional.of(makeUser()))
        whenever(commentService.addComment(1L, "users", "email", 1L, "test comment")).thenReturn(savedComment)

        mockMvc.perform(
            post("/api/workspaces/1/tables/users/columns/email/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"test comment\"}")
        )
            .andExpect(status().isCreated)
    }

    @Test
    @WithMockUser(username = "alice")
    fun `POST add comment returns 401 when user not found`() {
        whenever(userRepository.findByUsername("alice")).thenReturn(Optional.empty())

        mockMvc.perform(
            post("/api/workspaces/1/tables/users/columns/email/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"comment\":\"test comment\"}")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "alice")
    fun `POST add comment returns 400 when comment body missing`() {
        mockMvc.perform(
            post("/api/workspaces/1/tables/users/columns/email/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "alice")
    fun `DELETE comment returns 204`() {
        whenever(userRepository.findByUsername("alice")).thenReturn(Optional.of(makeUser()))
        doNothing().whenever(commentService).deleteComment(1L, 1L)

        mockMvc.perform(delete("/api/workspaces/1/tables/users/columns/email/comments/1"))
            .andExpect(status().isNoContent)
    }
}
