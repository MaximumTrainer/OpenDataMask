package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.ColumnSensitivity
import com.opendatamask.domain.model.ConfidenceLevel
import com.opendatamask.domain.model.SensitivityScanLog
import com.opendatamask.domain.model.SensitivityType
import com.opendatamask.adapter.output.persistence.ColumnSensitivityRepository
import com.opendatamask.adapter.output.persistence.SensitivityScanLogEntryRepository
import com.opendatamask.application.service.SensitivityScanService
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
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(
    SensitivityScanController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(SensitivityScanControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class SensitivityScanControllerTest {

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

    @MockBean private lateinit var sensitivityScanService: SensitivityScanService
    @MockBean private lateinit var columnSensitivityRepository: ColumnSensitivityRepository
    @MockBean private lateinit var sensitivityScanLogEntryRepository: SensitivityScanLogEntryRepository
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeScanLog(workspaceId: Long = 1L) = SensitivityScanLog(
        id = 1L,
        workspaceId = workspaceId,
        status = "COMPLETED",
        columnsScanned = 10,
        sensitiveColumnsFound = 3
    )

    private fun makeColumnSensitivity(workspaceId: Long = 1L) = ColumnSensitivity(
        id = 1L,
        workspaceId = workspaceId,
        tableName = "users",
        columnName = "email",
        isSensitive = true,
        sensitivityType = SensitivityType.EMAIL,
        confidenceLevel = ConfidenceLevel.HIGH
    )

    @Test
    fun `POST run sensitivity scan returns 200 with SensitivityScanLog`() {
        val log = makeScanLog()
        whenever(sensitivityScanService.scanWorkspace(1L)).thenReturn(log)

        mockMvc.perform(post("/api/workspaces/1/sensitivity-scan/run"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.workspaceId").value(1))
            .andExpect(jsonPath("$.status").value("COMPLETED"))
    }

    @Test
    fun `GET sensitivity scan status returns 200`() {
        whenever(sensitivityScanService.getLatestLog(1L)).thenReturn(makeScanLog())

        mockMvc.perform(get("/api/workspaces/1/sensitivity-scan/status"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.columnsScanned").value(10))
    }

    @Test
    fun `GET sensitivity scan results returns 200 with list`() {
        whenever(columnSensitivityRepository.findByWorkspaceId(1L))
            .thenReturn(listOf(makeColumnSensitivity()))

        mockMvc.perform(get("/api/workspaces/1/sensitivity-scan/results"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].tableName").value("users"))
    }

    @Test
    fun `GET sensitivity scan log returns 200 with list`() {
        val log = makeScanLog()
        whenever(sensitivityScanService.getScanLogs(1L)).thenReturn(listOf(log))
        whenever(sensitivityScanLogEntryRepository.findByScanLogId(1L)).thenReturn(emptyList())

        mockMvc.perform(get("/api/workspaces/1/sensitivity-scan/log"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET sensitivity scan log download returns 200 with Content-Disposition attachment header`() {
        whenever(sensitivityScanService.getScanLogs(1L)).thenReturn(emptyList())

        mockMvc.perform(get("/api/workspaces/1/sensitivity-scan/log/download"))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("sensitivity-scan-log-workspace-1")))
    }

    @Test
    fun `PATCH update column sensitivity returns 200`() {
        val entity = makeColumnSensitivity()
        whenever(columnSensitivityRepository.findByWorkspaceIdAndTableNameAndColumnName(1L, "users", "email"))
            .thenReturn(entity)
        whenever(columnSensitivityRepository.save(any())).thenReturn(entity)

        mockMvc.perform(
            patch("/api/workspaces/1/sensitivity-scan/columns/users/email")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"isSensitive":true,"sensitivityType":"EMAIL","confidenceLevel":"HIGH"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tableName").value("users"))
            .andExpect(jsonPath("$.columnName").value("email"))
    }
}
