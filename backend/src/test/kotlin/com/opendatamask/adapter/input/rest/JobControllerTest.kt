package com.opendatamask.adapter.input.rest

import com.opendatamask.application.service.JobCompareService
import com.opendatamask.domain.port.input.dto.JobCompareResponse
import com.opendatamask.domain.port.input.dto.JobLogResponse
import com.opendatamask.domain.port.input.dto.JobResponse
import com.opendatamask.domain.port.input.dto.JobTableStatsResponse
import com.opendatamask.domain.model.JobStatus
import com.opendatamask.domain.model.LogLevel
import com.opendatamask.domain.model.User
import com.opendatamask.domain.model.WorkspacePermission
import com.opendatamask.adapter.output.persistence.UserRepository
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
import com.opendatamask.application.service.JobProgressEmitterRegistry
import com.opendatamask.application.service.JobService
import com.opendatamask.application.service.PermissionService
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
    @MockBean private lateinit var jobProgressEmitterRegistry: JobProgressEmitterRegistry
    @MockBean private lateinit var userRepository: UserRepository
    @MockBean private lateinit var permissionService: PermissionService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl
    @MockBean private lateinit var jobCompareService: JobCompareService

    private fun makeJobResponse(
        id: Long = 1L,
        workspaceId: Long = 1L,
        status: JobStatus = JobStatus.PENDING,
        rowsProcessed: Long = 0L,
        tablesProcessed: Int = 0,
        tablesTotal: Int = 0
    ) = JobResponse(
        id = id,
        workspaceId = workspaceId,
        status = status,
        startedAt = null,
        completedAt = null,
        createdAt = LocalDateTime.now(),
        errorMessage = null,
        createdBy = 1L,
        rowsProcessed = rowsProcessed,
        tablesProcessed = tablesProcessed,
        tablesTotal = tablesTotal
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
    fun `GET job by id includes progress fields in response`() {
        whenever(jobService.getJob(1L, 1L)).thenReturn(
            makeJobResponse(id = 1L, status = JobStatus.COMPLETED, rowsProcessed = 50L, tablesProcessed = 1, tablesTotal = 1)
        )

        mockMvc.perform(get("/api/workspaces/1/jobs/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.rowsProcessed").value(50))
            .andExpect(jsonPath("$.tablesProcessed").value(1))
            .andExpect(jsonPath("$.tablesTotal").value(1))
    }

    @Test
    @WithMockUser(username = "alice")
    fun `POST create and run job returns 201`() {
        val mockUser = User(id = 1L, username = "alice", email = "alice@example.com", passwordHash = "hash")
        whenever(userRepository.findByUsername("alice")).thenReturn(Optional.of(mockUser))
        whenever(jobService.createJob(1L, 1L, null, null, false)).thenReturn(makeJobResponse(id = 1L, status = JobStatus.PENDING))
        doNothing().whenever(jobService).runJob(1L)

        mockMvc.perform(post("/api/workspaces/1/jobs"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"))

        verify(jobService).createJob(1L, 1L, null, null, false)
        verify(jobService).runJob(1L)
    }

    @Test
    fun `GET job stats returns 200 with per-table stats list`() {
        val stats = listOf(
            JobTableStatsResponse(
                id = 1L, jobId = 1L, tableName = "users",
                rowsRead = 100, rowsWritten = 100, rowsSkipped = 0,
                startedAt = java.time.LocalDateTime.now(), completedAt = java.time.LocalDateTime.now(),
                elapsedMs = 250, rowsPerSecond = 400.0, errorMessage = null
            )
        )
        whenever(jobService.getJobTableStats(1L, 1L)).thenReturn(stats)

        mockMvc.perform(get("/api/workspaces/1/jobs/1/stats"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].tableName").value("users"))
            .andExpect(jsonPath("$[0].rowsRead").value(100))
            .andExpect(jsonPath("$[0].elapsedMs").value(250))
    }

    @Test
    fun `GET compare returns job comparison report`() {
        val compareResponse = JobCompareResponse(
            jobAId = 1L,
            jobBId = 2L,
            rowsDelta = 50L,
            tablesAddedInB = listOf("payments"),
            tablesRemovedInB = emptyList(),
            tablesInCommon = listOf("users")
        )
        whenever(jobCompareService.compareJobs(1L, 1L, 2L)).thenReturn(compareResponse)

        mockMvc.perform(get("/api/workspaces/1/jobs/compare?jobA=1&jobB=2"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.jobAId").value(1))
            .andExpect(jsonPath("$.jobBId").value(2))
            .andExpect(jsonPath("$.rowsDelta").value(50))
            .andExpect(jsonPath("$.tablesAddedInB[0]").value("payments"))
    }
}
