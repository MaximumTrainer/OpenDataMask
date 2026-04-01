package com.opendatamask.controller

import com.opendatamask.model.StoredFile
import com.opendatamask.security.JwtAuthenticationFilter
import com.opendatamask.security.JwtTokenProvider
import com.opendatamask.security.UserDetailsServiceImpl
import com.opendatamask.service.FileStorageService
import com.opendatamask.service.RetrievedFile
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(
    FileController::class,
    excludeAutoConfiguration = [SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class]
)
@ActiveProfiles("test")
class FileControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockBean private lateinit var fileStorageService: FileStorageService
    @MockBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockBean private lateinit var userDetailsServiceImpl: UserDetailsServiceImpl

    private fun makeStoredFile(id: Long = 1L, workspaceId: Long = 1L) = StoredFile(
        id = id, workspaceId = workspaceId, filename = "data.csv",
        contentType = "text/csv", encryptedContent = "encrypted_base64",
        isSource = true, createdAt = LocalDateTime.now()
    )

    @Test
    fun `POST upload file returns 200 with file metadata`() {
        val stored = makeStoredFile()
        val mockFile = MockMultipartFile("file", "data.csv", "text/csv", "col1,col2\n1,2".toByteArray())

        whenever(fileStorageService.storeFile(any(), any(), any(), any(), any())).thenReturn(stored)

        mockMvc.perform(
            multipart("/api/workspaces/1/files").file(mockFile)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.filename").value("data.csv"))
    }

    @Test
    fun `GET list files returns 200 with file list`() {
        val files = listOf(makeStoredFile(id = 1L), makeStoredFile(id = 2L, workspaceId = 1L))
        whenever(fileStorageService.listFiles(1L)).thenReturn(files)

        mockMvc.perform(get("/api/workspaces/1/files"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `GET download file returns 200 with content`() {
        val stored = makeStoredFile(id = 1L)
        val retrieved = RetrievedFile(
            filename = "data.csv", contentType = "text/csv",
            content = "col1,col2\n1,2".toByteArray(), isSource = true
        )
        whenever(fileStorageService.retrieveFile(1L)).thenReturn(retrieved)

        mockMvc.perform(get("/api/workspaces/1/files/1"))
            .andExpect(status().isOk)
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"data.csv\""))
            .andExpect(content().contentType(MediaType.valueOf("text/csv")))
    }

    @Test
    fun `DELETE file returns 204`() {
        mockMvc.perform(delete("/api/workspaces/1/files/1"))
            .andExpect(status().isNoContent)

        verify(fileStorageService).deleteFile(1L)
    }
}
