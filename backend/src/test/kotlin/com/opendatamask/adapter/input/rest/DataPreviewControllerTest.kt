package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.port.input.dto.ColumnPreviewResult
import com.opendatamask.domain.port.input.dto.PreviewSample
import com.opendatamask.application.service.DataPreviewService
import com.opendatamask.infrastructure.security.JwtTokenProvider
import com.opendatamask.infrastructure.security.UserDetailsServiceImpl
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(
    DataPreviewController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class DataPreviewControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var dataPreviewService: DataPreviewService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makePreviewResult(
        tableName: String = "users",
        columnName: String = "email",
        generatorType: GeneratorType = GeneratorType.EMAIL,
        samples: List<PreviewSample> = listOf(
            PreviewSample("real@example.com", "fake@example.com"),
            PreviewSample("other@example.com", "masked@example.com")
        )
    ) = ColumnPreviewResult(
        tableName = tableName,
        columnName = columnName,
        generatorType = generatorType,
        samples = samples
    )

    @Test
    fun `GET preview returns 200 with result`() {
        whenever(dataPreviewService.previewColumn(1L, "users", "email", 5))
            .thenReturn(makePreviewResult())

        mockMvc.perform(get("/api/workspaces/1/tables/users/columns/email/preview"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tableName").value("users"))
            .andExpect(jsonPath("$.columnName").value("email"))
    }

    @Test
    fun `GET preview with default sampleSize uses 5`() {
        whenever(dataPreviewService.previewColumn(1L, "users", "email", 5))
            .thenReturn(makePreviewResult())

        mockMvc.perform(get("/api/workspaces/1/tables/users/columns/email/preview"))
            .andExpect(status().isOk)

        verify(dataPreviewService).previewColumn(1L, "users", "email", 5)
    }

    @Test
    fun `GET preview returns 404 when workspace not found`() {
        whenever(dataPreviewService.previewColumn(99L, "users", "email", 5))
            .thenThrow(NoSuchElementException("Workspace not found: 99"))

        mockMvc.perform(get("/api/workspaces/99/tables/users/columns/email/preview"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `GET preview with custom sampleSize=10`() {
        whenever(dataPreviewService.previewColumn(1L, "users", "email", 10))
            .thenReturn(makePreviewResult())

        mockMvc.perform(get("/api/workspaces/1/tables/users/columns/email/preview?sampleSize=10"))
            .andExpect(status().isOk)

        verify(dataPreviewService).previewColumn(1L, "users", "email", 10)
    }

    @Test
    fun `GET preview returns correct column data in response`() {
        whenever(dataPreviewService.previewColumn(1L, "orders", "phone", 5))
            .thenReturn(makePreviewResult(tableName = "orders", columnName = "phone", generatorType = GeneratorType.PHONE))

        mockMvc.perform(get("/api/workspaces/1/tables/orders/columns/phone/preview"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.tableName").value("orders"))
            .andExpect(jsonPath("$.columnName").value("phone"))
            .andExpect(jsonPath("$.generatorType").value("PHONE"))
    }

    @Test
    fun `GET preview response contains samples`() {
        val samples = listOf(
            PreviewSample("real@example.com", "masked@example.com"),
            PreviewSample("other@example.com", "anon@example.com")
        )
        whenever(dataPreviewService.previewColumn(1L, "users", "email", 5))
            .thenReturn(makePreviewResult(samples = samples))

        mockMvc.perform(get("/api/workspaces/1/tables/users/columns/email/preview"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.samples.length()").value(2))
            .andExpect(jsonPath("$.samples[0].originalValue").value("real@example.com"))
            .andExpect(jsonPath("$.samples[0].maskedValue").value("masked@example.com"))
    }
}
