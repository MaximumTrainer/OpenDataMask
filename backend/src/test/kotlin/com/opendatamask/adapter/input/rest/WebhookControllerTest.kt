package com.opendatamask.adapter.input.rest

import com.opendatamask.domain.model.Webhook
import com.opendatamask.domain.model.WebhookTriggerType
import com.opendatamask.domain.model.WebhookType
import com.opendatamask.application.service.WebhookService
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

@WebMvcTest(
    WebhookController::class,
    excludeAutoConfiguration = [
        SecurityAutoConfiguration::class,
        SecurityFilterAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ]
)
@Import(WebhookControllerTest.TestSecurityConfig::class)
@ActiveProfiles("test")
class WebhookControllerTest {

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

    @MockBean private lateinit var webhookService: WebhookService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeWebhook(id: Long = 1L) = Webhook(
        id = id,
        workspaceId = 1L,
        name = "test-webhook",
        enabled = true,
        triggerType = WebhookTriggerType.DATA_GENERATION,
        webhookType = WebhookType.CUSTOM,
        url = "https://example.com"
    )

    private val validRequestBody = """{"name":"test","enabled":true,"triggerType":"DATA_GENERATION","webhookType":"CUSTOM","url":"https://example.com"}"""

    @Test
    fun `GET list webhooks returns 200 with list`() {
        whenever(webhookService.listWebhooks(1L)).thenReturn(listOf(makeWebhook(1L), makeWebhook(2L)))

        mockMvc.perform(get("/api/workspaces/1/webhooks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].name").value("test-webhook"))
    }

    @Test
    fun `POST create webhook returns 200`() {
        whenever(webhookService.createWebhook(eq(1L), any())).thenReturn(makeWebhook())

        mockMvc.perform(
            post("/api/workspaces/1/webhooks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("test-webhook"))
            .andExpect(jsonPath("$.triggerType").value("DATA_GENERATION"))
    }

    @Test
    fun `PUT update webhook returns 200`() {
        whenever(webhookService.updateWebhook(eq(1L), eq(1L), any())).thenReturn(makeWebhook())

        mockMvc.perform(
            put("/api/workspaces/1/webhooks/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
    }

    @Test
    fun `DELETE webhook returns 204`() {
        doNothing().whenever(webhookService).deleteWebhook(1L, 1L)

        mockMvc.perform(delete("/api/workspaces/1/webhooks/1"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `POST test webhook returns 200`() {
        doNothing().whenever(webhookService).testWebhook(1L, 1L)

        mockMvc.perform(post("/api/workspaces/1/webhooks/1/test"))
            .andExpect(status().isOk)

        verify(webhookService).testWebhook(1L, 1L)
    }

    @Test
    fun `GET list webhooks returns 200 with empty list when no webhooks`() {
        whenever(webhookService.listWebhooks(1L)).thenReturn(emptyList())

        mockMvc.perform(get("/api/workspaces/1/webhooks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }
}
