package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.JobSchedule
import com.opendatamask.domain.model.ScheduledJobType
import com.opendatamask.application.service.JobSchedulerService
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
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(
    JobScheduleController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(JobScheduleControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class JobScheduleControllerTest {

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

    @MockBean private lateinit var schedulerService: JobSchedulerService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeSchedule(id: Long = 1L, workspaceId: Long = 1L) = JobSchedule(
        id = id,
        workspaceId = workspaceId,
        cronExpression = "0 0 * * *",
        enabled = true,
        jobType = ScheduledJobType.FULL_GENERATION,
        nextRunAt = LocalDateTime.now().plusHours(1)
    )

    @Test
    fun `GET list schedules returns 200 with list`() {
        whenever(schedulerService.listSchedules(1L)).thenReturn(listOf(makeSchedule(1L), makeSchedule(2L)))

        mockMvc.perform(get("/api/workspaces/1/schedules"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `POST create schedule returns 201`() {
        whenever(schedulerService.createSchedule(any())).thenReturn(makeSchedule())

        mockMvc.perform(
            post("/api/workspaces/1/schedules")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"workspaceId":1,"cronExpression":"0 0 * * *","enabled":true,"jobType":"FULL_GENERATION"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.cronExpression").value("0 0 * * *"))
    }

    @Test
    fun `PUT update schedule returns 200`() {
        whenever(schedulerService.updateSchedule(eq(1L), any())).thenReturn(makeSchedule())

        mockMvc.perform(
            put("/api/workspaces/1/schedules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"workspaceId":1,"cronExpression":"0 0 * * *","enabled":true,"jobType":"FULL_GENERATION"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `DELETE schedule returns 204`() {
        doNothing().whenever(schedulerService).deleteSchedule(1L)

        mockMvc.perform(delete("/api/workspaces/1/schedules/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `POST validate-cron returns 200 with valid true and nextRun when cron is valid`() {
        doNothing().whenever(schedulerService).validateCron(any())
        whenever(schedulerService.computeNextRun(any())).thenReturn(LocalDateTime.now().plusDays(1))

        mockMvc.perform(
            post("/api/workspaces/1/schedules/validate-cron")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"cronExpression":"0 0 * * *"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.nextRun").exists())
    }

    @Test
    fun `POST validate-cron returns 200 with valid false and error when cron is invalid`() {
        whenever(schedulerService.validateCron(any()))
            .thenThrow(IllegalArgumentException("Invalid cron expression"))

        mockMvc.perform(
            post("/api/workspaces/1/schedules/validate-cron")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"cronExpression":"bad-cron"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.error").exists())
    }
}
