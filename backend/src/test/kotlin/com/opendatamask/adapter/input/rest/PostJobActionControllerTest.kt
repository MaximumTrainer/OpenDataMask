package com.opendatamask.adapter.input.rest

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opendatamask.adapter.input.rest.dto.PostJobActionRequest
import com.opendatamask.domain.model.ActionType
import com.opendatamask.domain.model.PostJobAction
import com.opendatamask.infrastructure.security.JwtAuthenticationFilter
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
import com.opendatamask.application.service.PostJobActionService
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(
    PostJobActionController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class PostJobActionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var service: PostJobActionService

    @MockBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockBean
    private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Test
    fun `GET actions for workspace returns 200`() {
        whenever(service.listActions(1L)).thenReturn(emptyList())
        mockMvc.perform(get("/api/workspaces/1/actions"))
            .andExpect(status().isOk)
    }

    @Test
    fun `POST create action returns 201`() {
        val request = PostJobActionRequest(actionType = ActionType.WEBHOOK, config = """{"url":"http://example.com"}""")
        val saved = PostJobAction(id = 1L, workspaceId = 1L, actionType = ActionType.WEBHOOK, config = """{"url":"http://example.com"}""")
        whenever(service.createAction(any<PostJobAction>())).thenReturn(saved)
        mockMvc.perform(
            post("/api/workspaces/1/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
    }

    @Test
    fun `DELETE action returns 204`() {
        mockMvc.perform(delete("/api/workspaces/1/actions/42"))
            .andExpect(status().isNoContent)
        verify(service).deleteAction(42L)
    }

    @Test
    fun `PUT update action returns 200 with correct response body`() {
        val request = PostJobActionRequest(
            actionType = ActionType.WEBHOOK,
            config = """{"url":"http://example.com"}"""
        )
        val returned = PostJobAction(
            id = 42L,
            workspaceId = 1L,
            actionType = ActionType.WEBHOOK,
            config = """{"url":"http://example.com"}"""
        )
        whenever(service.updateAction(eq(1L), eq(42L), any<PostJobActionRequest>())).thenReturn(returned)
        mockMvc.perform(
            put("/api/workspaces/1/actions/42")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(42))
            .andExpect(jsonPath("$.workspaceId").value(1))
            .andExpect(jsonPath("$.actionType").value("WEBHOOK"))
            .andExpect(jsonPath("$.config").value("""{"url":"http://example.com"}"""))
        verify(service).updateAction(eq(1L), eq(42L), any<PostJobActionRequest>())
    }
}
