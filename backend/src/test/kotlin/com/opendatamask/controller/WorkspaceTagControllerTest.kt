package com.opendatamask.controller

import com.opendatamask.domain.model.WorkspaceTag
import com.opendatamask.adapter.output.persistence.WorkspaceTagRepository
import com.opendatamask.security.JwtTokenProvider
import com.opendatamask.security.UserDetailsServiceImpl
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
    WorkspaceTagController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class WorkspaceTagControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockBean private lateinit var tagRepository: WorkspaceTagRepository
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    @Test
    fun `GET tags returns 200`() {
        whenever(tagRepository.findByWorkspaceId(1L)).thenReturn(listOf(WorkspaceTag(workspaceId = 1L, tag = "pii")))
        mockMvc.perform(get("/api/workspaces/1/tags"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0]").value("pii"))
    }

    @Test
    fun `POST tag returns 201`() {
        val saved = WorkspaceTag(workspaceId = 1L, tag = "pii")
        whenever(tagRepository.save(any<WorkspaceTag>())).thenReturn(saved)
        mockMvc.perform(
            post("/api/workspaces/1/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"tag": "pii"}""")
        ).andExpect(status().isCreated)
    }

    @Test
    fun `DELETE tag returns 204`() {
        mockMvc.perform(delete("/api/workspaces/1/tags/pii"))
            .andExpect(status().isNoContent)
        verify(tagRepository).deleteByWorkspaceIdAndTag(1L, "pii")
    }
}
