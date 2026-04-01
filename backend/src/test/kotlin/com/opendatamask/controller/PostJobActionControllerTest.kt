package com.opendatamask.controller

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.opendatamask.model.ActionType
import com.opendatamask.model.PostJobAction
import com.opendatamask.security.JwtAuthenticationFilter
import com.opendatamask.security.JwtTokenProvider
import com.opendatamask.security.UserDetailsServiceImpl
import com.opendatamask.service.PostJobActionService
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
        val action = PostJobAction(workspaceId = 1L, actionType = ActionType.WEBHOOK, config = """{"url":"http://example.com"}""")
        whenever(service.createAction(any<PostJobAction>())).thenReturn(action.copy(id = 1L))
        mockMvc.perform(
            post("/api/workspaces/1/actions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(action))
        ).andExpect(status().isCreated)
    }

    @Test
    fun `DELETE action returns 204`() {
        mockMvc.perform(delete("/api/workspaces/1/actions/42"))
            .andExpect(status().isNoContent)
        verify(service).deleteAction(42L)
    }

    @Test
    fun `PUT update action returns 200`() {
        val action = PostJobAction(workspaceId = 1L, actionType = ActionType.WEBHOOK, config = """{"url":"http://example.com"}""")
        whenever(service.updateAction(eq(42L), any<PostJobAction>())).thenReturn(action.copy(id = 42L))
        mockMvc.perform(
            put("/api/workspaces/1/actions/42")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(action))
        ).andExpect(status().isOk)
        verify(service).updateAction(eq(42L), any<PostJobAction>())
    }
}
