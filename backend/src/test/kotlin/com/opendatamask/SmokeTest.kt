package com.opendatamask

import com.opendatamask.application.service.AuthService
import com.opendatamask.application.service.GeneratorService
import com.opendatamask.application.service.WorkspaceService
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.port.input.dto.RegisterRequest
import com.opendatamask.domain.port.input.dto.WorkspaceRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@Tag("smoke")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SmokeTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var authService: AuthService

    @Autowired
    private lateinit var workspaceService: WorkspaceService

    @Autowired
    private lateinit var generatorService: GeneratorService

    @Test
    fun `health endpoint returns UP`() {
        val response = restTemplate.getForEntity(
            "http://localhost:$port/actuator/health",
            Map::class.java
        )
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("UP", response.body?.get("status"))
    }

    @Test
    fun `application context loads successfully`() {
        assertNotNull(authService)
        assertNotNull(workspaceService)
        assertNotNull(generatorService)
    }

    @Test
    fun `basic masking operation generates non-blank NAME value`() {
        val masked = generatorService.generateValue(GeneratorType.NAME, "John Doe", null)
        assertNotNull(masked)
        assertTrue((masked as String).isNotBlank())
        assertNotEquals("John Doe", masked)
    }

    @Test
    fun `basic masking operation generates valid EMAIL value`() {
        val masked = generatorService.generateValue(GeneratorType.EMAIL, "user@example.com", null)
        assertNotNull(masked)
        val maskedStr = masked as String
        assertTrue(maskedStr.contains("@"), "Masked email should contain @")
        assertTrue(maskedStr.isNotBlank())
    }

    @Test
    fun `user registration and workspace creation flow`() {
        val unique = System.nanoTime()
        val authResponse = authService.register(
            RegisterRequest(
                username = "smoke_user_$unique",
                email = "smoke_$unique@example.com",
                password = "SmokeTest123!"
            )
        )
        assertNotNull(authResponse.token)
        assertNotNull(authResponse.userId)

        val workspace = workspaceService.createWorkspace(
            WorkspaceRequest(name = "Smoke Test Workspace $unique", description = "Created by smoke test"),
            authResponse.userId
        )
        assertNotNull(workspace.id)
        assertEquals("Smoke Test Workspace $unique", workspace.name)

        workspaceService.deleteWorkspace(workspace.id, authResponse.userId)

        assertThrows(NoSuchElementException::class.java) {
            workspaceService.getWorkspace(workspace.id)
        }
    }
}
