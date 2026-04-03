package com.opendatamask.application.service

import com.opendatamask.domain.model.SubsetLimitType
import com.opendatamask.domain.model.SubsetTableConfig
import com.opendatamask.domain.port.input.dto.SubsetTableConfigRequest
import com.opendatamask.domain.port.output.SubsetTableConfigPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.util.Optional

class SubsetConfigServiceTest {

    private val repo = mock<SubsetTableConfigPort>()
    private val service = SubsetConfigService(repo)

    private fun makeConfig(
        id: Long? = 1L,
        workspaceId: Long = 1L,
        tableName: String = "users"
    ) = SubsetTableConfig(
        id = id,
        workspaceId = workspaceId,
        tableName = tableName,
        limitType = SubsetLimitType.PERCENTAGE,
        limitValue = 10
    )

    @Test
    fun `listConfigs returns list for workspace`() {
        val configs = listOf(makeConfig(1L, tableName = "users"), makeConfig(2L, tableName = "orders"))
        whenever(repo.findByWorkspaceId(1L)).thenReturn(configs)

        val result = service.listConfigs(1L)

        assertEquals(2, result.size)
        assertEquals("users", result[0].tableName)
        assertEquals("orders", result[1].tableName)
    }

    @Test
    fun `createOrUpdateConfig creates new when not existing`() {
        val request = SubsetTableConfigRequest(tableName = "users")
        val savedConfig = makeConfig(1L)
        whenever(repo.findByWorkspaceIdAndTableName(1L, "users")).thenReturn(null)
        whenever(repo.save(any())).thenReturn(savedConfig)

        val result = service.createOrUpdateConfig(1L, request)

        assertEquals("users", result.tableName)
        verify(repo).save(any())
    }

    @Test
    fun `createOrUpdateConfig updates existing when found`() {
        val existing = makeConfig(1L)
        val request = SubsetTableConfigRequest(
            tableName = "users",
            limitType = SubsetLimitType.ROW_COUNT,
            limitValue = 100
        )
        whenever(repo.findByWorkspaceIdAndTableName(1L, "users")).thenReturn(existing)
        whenever(repo.save(any())).thenAnswer { it.arguments[0] }

        val result = service.createOrUpdateConfig(1L, request)

        assertEquals(SubsetLimitType.ROW_COUNT, result.limitType)
        assertEquals(100, result.limitValue)
    }

    @Test
    fun `updateConfig happy path`() {
        val config = makeConfig(1L)
        val request = SubsetTableConfigRequest(
            tableName = "users",
            limitType = SubsetLimitType.ROW_COUNT,
            limitValue = 50
        )
        whenever(repo.findById(1L)).thenReturn(Optional.of(config))
        whenever(repo.save(any())).thenAnswer { it.arguments[0] }

        val result = service.updateConfig(1L, 1L, request)

        assertEquals(SubsetLimitType.ROW_COUNT, result.limitType)
        assertEquals(50, result.limitValue)
    }

    @Test
    fun `updateConfig throws NoSuchElementException when not found`() {
        whenever(repo.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.updateConfig(1L, 99L, SubsetTableConfigRequest(tableName = "users"))
        }
    }

    @Test
    fun `updateConfig throws NoSuchElementException when wrong workspace`() {
        val config = makeConfig(1L, workspaceId = 2L)
        whenever(repo.findById(1L)).thenReturn(Optional.of(config))

        assertThrows<NoSuchElementException> {
            service.updateConfig(1L, 1L, SubsetTableConfigRequest(tableName = "users"))
        }
    }

    @Test
    fun `deleteConfig happy path`() {
        val config = makeConfig(1L)
        whenever(repo.findById(1L)).thenReturn(Optional.of(config))

        service.deleteConfig(1L, 1L)

        verify(repo).deleteById(1L)
    }

    @Test
    fun `deleteConfig throws NoSuchElementException when not found`() {
        whenever(repo.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.deleteConfig(1L, 99L)
        }
    }

    @Test
    fun `deleteConfig throws NoSuchElementException when wrong workspace`() {
        val config = makeConfig(1L, workspaceId = 2L)
        whenever(repo.findById(1L)).thenReturn(Optional.of(config))

        assertThrows<NoSuchElementException> {
            service.deleteConfig(1L, 1L)
        }
    }
}
