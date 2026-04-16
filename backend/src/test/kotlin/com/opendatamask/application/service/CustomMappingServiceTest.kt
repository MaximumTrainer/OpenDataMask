package com.opendatamask.application.service

import com.opendatamask.adapter.output.persistence.ColumnGeneratorRepository
import com.opendatamask.adapter.output.persistence.TableConfigurationRepository
import com.opendatamask.adapter.output.persistence.WorkspaceRepository
import com.opendatamask.domain.model.*
import com.opendatamask.domain.port.input.dto.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.Optional

class CustomMappingServiceTest {

    private val workspaceRepo = mock<WorkspaceRepository>()
    private val tableConfigRepo = mock<TableConfigurationRepository>()
    private val columnGeneratorRepo = mock<ColumnGeneratorRepository>()
    private val service = CustomMappingService(workspaceRepo, tableConfigRepo, columnGeneratorRepo)

    private fun workspace(id: Long = 1L) = Workspace(id = id, name = "ws", ownerId = 1L)

    // ── resolveGeneratorType ──────────────────────────────────────────────

    @Test
    fun `resolveGeneratorType maps NULL strategy to GeneratorType NULL`() {
        assertEquals(GeneratorType.NULL, service.resolveGeneratorType("NULL"))
    }

    @Test
    fun `resolveGeneratorType maps HASH strategy to GeneratorType HASH`() {
        assertEquals(GeneratorType.HASH, service.resolveGeneratorType("HASH"))
    }

    @Test
    fun `resolveGeneratorType maps SCRAMBLE strategy to GeneratorType SCRAMBLE`() {
        assertEquals(GeneratorType.SCRAMBLE, service.resolveGeneratorType("SCRAMBLE"))
    }

    @Test
    fun `resolveGeneratorType maps FAKE strategy to GeneratorType FULL_NAME`() {
        assertEquals(GeneratorType.FULL_NAME, service.resolveGeneratorType("FAKE"))
    }

    @Test
    fun `resolveGeneratorType maps direct GeneratorType name`() {
        assertEquals(GeneratorType.EMAIL, service.resolveGeneratorType("EMAIL"))
    }

    @Test
    fun `resolveGeneratorType falls back to FULL_NAME for unknown strategy`() {
        assertEquals(GeneratorType.FULL_NAME, service.resolveGeneratorType("UNKNOWN_STRATEGY"))
    }

    @Test
    fun `resolveGeneratorType falls back to FULL_NAME for null strategy`() {
        assertEquals(GeneratorType.FULL_NAME, service.resolveGeneratorType(null))
    }

    @Test
    fun `resolveGeneratorType is case-insensitive`() {
        assertEquals(GeneratorType.HASH, service.resolveGeneratorType("hash"))
        assertEquals(GeneratorType.NULL, service.resolveGeneratorType("null"))
        assertEquals(GeneratorType.SCRAMBLE, service.resolveGeneratorType("scramble"))
    }

    // ── applyCustomMapping ────────────────────────────────────────────────

    @Test
    fun `applyCustomMapping throws when workspace not found`() {
        whenever(workspaceRepo.findById(99L)).thenReturn(Optional.empty())
        assertThrows(NoSuchElementException::class.java) {
            service.applyCustomMapping(99L, CustomMappingDto())
        }
    }

    @Test
    fun `applyCustomMapping creates table with MASK mode when any attribute is MASK`() {
        whenever(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace()))
        whenever(tableConfigRepo.findByWorkspaceId(1L)).thenReturn(emptyList())
        val savedTable = TableConfiguration(id = 10L, workspaceId = 1L, tableName = "users", mode = TableMode.MASK)
        whenever(tableConfigRepo.save(any<TableConfiguration>())).thenReturn(savedTable)
        whenever(columnGeneratorRepo.findByTableConfigurationId(10L)).thenReturn(emptyList())
        whenever(columnGeneratorRepo.save(any<ColumnGenerator>())).thenAnswer { it.arguments[0] }

        val mapping = CustomMappingDto(
            project = "MyProject",
            tables = listOf(
                CustomMappingTableDto(
                    tableName = "users",
                    attributes = listOf(
                        CustomMappingAttributeDto(name = "id", action = CustomMappingAction.MIGRATE_AS_IS),
                        CustomMappingAttributeDto(name = "email", action = CustomMappingAction.MASK, strategy = "FAKE")
                    )
                )
            )
        )
        service.applyCustomMapping(1L, mapping)

        val captor = argumentCaptor<TableConfiguration>()
        verify(tableConfigRepo).save(captor.capture())
        assertEquals(TableMode.MASK, captor.firstValue.mode)
        assertEquals("users", captor.firstValue.tableName)
    }

    @Test
    fun `applyCustomMapping creates table with PASSTHROUGH mode when all attributes are MIGRATE_AS_IS`() {
        whenever(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace()))
        whenever(tableConfigRepo.findByWorkspaceId(1L)).thenReturn(emptyList())
        val savedTable = TableConfiguration(id = 10L, workspaceId = 1L, tableName = "audit_log", mode = TableMode.PASSTHROUGH)
        whenever(tableConfigRepo.save(any<TableConfiguration>())).thenReturn(savedTable)
        whenever(columnGeneratorRepo.findByTableConfigurationId(10L)).thenReturn(emptyList())

        val mapping = CustomMappingDto(
            tables = listOf(
                CustomMappingTableDto(
                    tableName = "audit_log",
                    attributes = listOf(
                        CustomMappingAttributeDto(name = "id", action = CustomMappingAction.MIGRATE_AS_IS),
                        CustomMappingAttributeDto(name = "action", action = CustomMappingAction.MIGRATE_AS_IS)
                    )
                )
            )
        )
        service.applyCustomMapping(1L, mapping)

        val captor = argumentCaptor<TableConfiguration>()
        verify(tableConfigRepo).save(captor.capture())
        assertEquals(TableMode.PASSTHROUGH, captor.firstValue.mode)
        verify(columnGeneratorRepo, never()).save(any<ColumnGenerator>())
    }

    @Test
    fun `applyCustomMapping creates generators only for MASK attributes`() {
        whenever(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace()))
        whenever(tableConfigRepo.findByWorkspaceId(1L)).thenReturn(emptyList())
        val savedTable = TableConfiguration(id = 10L, workspaceId = 1L, tableName = "users", mode = TableMode.MASK)
        whenever(tableConfigRepo.save(any<TableConfiguration>())).thenReturn(savedTable)
        whenever(columnGeneratorRepo.findByTableConfigurationId(10L)).thenReturn(emptyList())
        whenever(columnGeneratorRepo.save(any<ColumnGenerator>())).thenAnswer { it.arguments[0] }

        val mapping = CustomMappingDto(
            tables = listOf(
                CustomMappingTableDto(
                    tableName = "users",
                    attributes = listOf(
                        CustomMappingAttributeDto(name = "id", action = CustomMappingAction.MIGRATE_AS_IS),
                        CustomMappingAttributeDto(name = "email", action = CustomMappingAction.MASK, strategy = "FAKE"),
                        CustomMappingAttributeDto(name = "salary", action = CustomMappingAction.MASK, strategy = "NULL"),
                        CustomMappingAttributeDto(name = "name", action = CustomMappingAction.MASK, strategy = "HASH")
                    )
                )
            )
        )
        service.applyCustomMapping(1L, mapping)

        val genCaptor = argumentCaptor<ColumnGenerator>()
        verify(columnGeneratorRepo, times(3)).save(genCaptor.capture())
        val savedGenerators = genCaptor.allValues
        assertEquals(GeneratorType.FULL_NAME, savedGenerators.find { it.columnName == "email" }?.generatorType)
        assertEquals(GeneratorType.NULL, savedGenerators.find { it.columnName == "salary" }?.generatorType)
        assertEquals(GeneratorType.HASH, savedGenerators.find { it.columnName == "name" }?.generatorType)
    }

    @Test
    fun `applyCustomMapping updates existing table configuration`() {
        val existingTable = TableConfiguration(id = 5L, workspaceId = 1L, tableName = "orders", mode = TableMode.PASSTHROUGH)
        whenever(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace()))
        whenever(tableConfigRepo.findByWorkspaceId(1L)).thenReturn(listOf(existingTable))
        whenever(tableConfigRepo.save(any<TableConfiguration>())).thenAnswer { it.arguments[0] }
        whenever(columnGeneratorRepo.findByTableConfigurationId(5L)).thenReturn(emptyList())
        whenever(columnGeneratorRepo.save(any<ColumnGenerator>())).thenAnswer { it.arguments[0] }

        val mapping = CustomMappingDto(
            tables = listOf(
                CustomMappingTableDto(
                    tableName = "orders",
                    attributes = listOf(
                        CustomMappingAttributeDto(name = "customer_id", action = CustomMappingAction.MASK, strategy = "HASH")
                    )
                )
            )
        )
        service.applyCustomMapping(1L, mapping)

        val captor = argumentCaptor<TableConfiguration>()
        verify(tableConfigRepo).save(captor.capture())
        // Must reuse the same entity (same id)
        assertEquals(5L, captor.firstValue.id)
        assertEquals(TableMode.MASK, captor.firstValue.mode)
    }
}
