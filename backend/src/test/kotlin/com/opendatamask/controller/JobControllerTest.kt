package com.opendatamask.controller

import com.opendatamask.dto.JobLogResponse
import com.opendatamask.dto.JobResponse
import com.opendatamask.domain.model.JobStatus
import com.opendatamask.domain.model.LogLevel
import com.opendatamask.domain.model.User
import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.security.JwtTokenProvider
import com.opendatamask.security.UserDetailsServiceImpl
import com.opendatamask.service.JobService
import com.opendatamask.service.PermissionService
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
    JobController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(JobControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class JobControllerTest {

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

    @MockBean private lateinit var jobService: JobService
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var permissionService: PermissionService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeJobResponse(id: Long = 1L, workspaceId: Long = 1L, status: JobStatus = JobStatus.PENDING) =
        JobResponse(
            id = id,
            workspaceId = workspaceId,
            status = status,
            startedAt = null,
            completedAt = null,
            createdAt = LocalDateTime.now(),
            errorMessage = null,
            createdBy = 1L
        )

    private fun makeJobLogResponse(id: Long = 1L, jobId: Long = 1L) =
        JobLogResponse(
            id = id,
            jobId = jobId,
            message = "Job started",
            level = LogLevel.INFO,
            timestamp = LocalDateTime.now()
        )

    @Test
    fun `GET list jobs returns 200`() {
        whenever(jobService.listJobs(1L)).thenReturn(listOf(makeJobResponse(id = 1L), makeJobResponse(id = 2L)))

        mockMvc.perform(get("/api/workspaces/1/jobs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `GET job by id returns 200`() {
        whenever(jobService.getJob(1L, 1L)).thenReturn(makeJobResponse(id = 1L, status = JobStatus.COMPLETED))

        mockMvc.perform(get("/api/workspaces/1/jobs/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
    }

    @Test
    fun `GET job by id returns 404 when not found`() {
        whenever(jobService.getJob(1L, 99L)).thenThrow(NoSuchElementException("Job not found: 99"))

        mockMvc.perform(get("/api/workspaces/1/jobs/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET job logs returns 200`() {
        whenever(jobService.getJobLogs(1L, 1L)).thenReturn(
            listOf(makeJobLogResponse(id = 1L), makeJobLogResponse(id = 2L))
        )

        mockMvc.perform(get("/api/workspaces/1/jobs/1/logs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].message").value("Job started"))
    }

    @Test
    fun `POST cancel job returns 200`() {
        whenever(jobService.cancelJob(1L, 1L)).thenReturn(makeJobResponse(id = 1L, status = JobStatus.CANCELLED))

        mockMvc.perform(post("/api/workspaces/1/jobs/1/cancel"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))
    }

    @Test
    @WithMockUser(username = "alice")
    fun `POST create and run job returns 201`() {
        val mockUser = User(id = 1L, username = "alice", email = "alice@example.com", passwordHash = "hash")
        whenever(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser))
        whenever(jobService.createJob(1L, 1L)).thenReturn(makeJobResponse(id = 1L, status = JobStatus.PENDING))
        doNothing().whenever(jobService).runJob(1L)

        mockMvc.perform(post("/api/workspaces/1/jobs"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"))

        verify(jobService).createJob(1L, 1L)
        verify(jobService).runJob(1L)
    }
}
