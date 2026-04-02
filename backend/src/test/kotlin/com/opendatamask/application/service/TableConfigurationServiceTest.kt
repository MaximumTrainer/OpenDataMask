package com.opendatamask.application.service

import com.opendatamask.domain.port.input.dto.ColumnGeneratorRequest
import com.opendatamask.domain.port.input.dto.TableConfigurationRequest
import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.TableConfiguration
import com.opendatamask.domain.model.TableMode
import com.opendatamask.adapter.output.persistence.ColumnGeneratorRepository
import com.opendatamask.adapter.output.persistence.TableConfigurationRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class TableConfigurationServiceTest {

    @Mock private lateinit var tableConfigurationRepository: TableConfigurationRepository
    @Mock private lateinit var columnGeneratorRepository: ColumnGeneratorRepository

    @InjectMocks
    private lateinit var service: TableConfigurationService

    private fun makeTableConfig(
        id: Long = 1L,
        workspaceId: Long = 10L,
        tableName: String = "users",
        mode: TableMode = TableMode.PASSTHROUGH
    ) = TableConfiguration(id = id, workspaceId = workspaceId, tableName = tableName, mode = mode)

    private fun makeGenerator(
        id: Long = 1L,
        tableConfigurationId: Long = 1L,
        columnName: String = "email",
        type: GeneratorType = GeneratorType.EMAIL
    ) = ColumnGenerator(id = id, tableConfigurationId = tableConfigurationId, columnName = columnName, generatorType = type)

    private fun makeConfigRequest(
        tableName: String = "users",
        mode: TableMode = TableMode.PASSTHROUGH
    ) = TableConfigurationRequest(tableName = tableName, mode = mode)

    // ── createTableConfiguration ───────────────────────────────────────────

    @Test
    fun `createTableConfiguration saves and returns response`() {
        val saved = makeTableConfig(id = 1L)
        whenever(tableConfigurationRepository.save(any<TableConfiguration>())).thenReturn(saved)

        val response = service.createTableConfiguration(10L, makeConfigRequest())

        assertEquals(1L, response.id)
        assertEquals(10L, response.workspaceId)
        assertEquals("users", response.tableName)
        assertEquals(TableMode.PASSTHROUGH, response.mode)
    }

    @Test
    fun `createTableConfiguration saves with all optional fields`() {
        val saved = makeTableConfig(id = 1L, mode = TableMode.SUBSET).also {
            it.rowLimit = 100L
            it.whereClause = "active = true"
        }
        val request = TableConfigurationRequest(
            tableName = "users", mode = TableMode.SUBSET,
            rowLimit = 100L, whereClause = "active = true"
        )
        whenever(tableConfigurationRepository.save(any<TableConfiguration>())).thenReturn(saved)

        val response = service.createTableConfiguration(10L, request)

        assertEquals(100L, response.rowLimit)
        assertEquals("active = true", response.whereClause)
    }

    // ── getTableConfiguration ──────────────────────────────────────────────

    @Test
    fun `getTableConfiguration returns config by id`() {
        val config = makeTableConfig(id = 1L, workspaceId = 10L)
        whenever(tableConfigurationRepository.findById(1L)).thenReturn(Optional.of(config))

        val response = service.getTableConfiguration(10L, 1L)

        assertEquals(1L, response.id)
        assertEquals("users", response.tableName)
    }

    @Test
    fun `getTableConfiguration throws when config not found`() {
        whenever(tableConfigurationRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.getTableConfiguration(10L, 99L) }
    }

    @Test
    fun `getTableConfiguration throws when config belongs to different workspace`() {
        val config = makeTableConfig(id = 1L, workspaceId = 20L)
        whenever(tableConfigurationRepository.findById(1L)).thenReturn(Optional.of(config))

        assertThrows<NoSuchElementException> { service.getTableConfiguration(10L, 1L) }
    }

    // ── listTableConfigurations ────────────────────────────────────────────

    @Test
    fun `listTableConfigurations returns all configs for workspace`() {
        val configs = listOf(makeTableConfig(id = 1L), makeTableConfig(id = 2L, tableName = "orders"))
        whenever(tableConfigurationRepository.findByWorkspaceId(10L)).thenReturn(configs)

        val result = service.listTableConfigurations(10L)

        assertEquals(2, result.size)
    }

    // ── updateTableConfiguration ───────────────────────────────────────────

    @Test
    fun `updateTableConfiguration updates fields`() {
        val config = makeTableConfig(id = 1L, workspaceId = 10L)
        val request = TableConfigurationRequest(tableName = "accounts", mode = TableMode.MASK, rowLimit = 50L)
        whenever(tableConfigurationRepository.findById(1L)).thenReturn(Optional.of(config))
        whenever(tableConfigurationRepository.save(any<TableConfiguration>())).thenAnswer { it.arguments[0] as TableConfiguration }

        val response = service.updateTableConfiguration(10L, 1L, request)

        assertEquals("accounts", response.tableName)
        assertEquals(TableMode.MASK, response.mode)
        assertEquals(50L, response.rowLimit)
    }

    @Test
    fun `updateTableConfiguration throws when config not found`() {
        whenever(tableConfigurationRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.updateTableConfiguration(10L, 99L, makeConfigRequest())
        }
    }

    // ── deleteTableConfiguration ───────────────────────────────────────────

    @Test
    fun `deleteTableConfiguration removes config and its generators`() {
        val config = makeTableConfig(id = 1L, workspaceId = 10L)
        whenever(tableConfigurationRepository.findById(1L)).thenReturn(Optional.of(config))

        service.deleteTableConfiguration(10L, 1L)

        verify(columnGeneratorRepository).deleteByTableConfigurationId(1L)
        verify(tableConfigurationRepository).delete(config)
    }

    @Test
    fun `deleteTableConfiguration throws when config not found`() {
        whenever(tableConfigurationRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.deleteTableConfiguration(10L, 99L) }
    }

    // ── addColumnGenerator ─────────────────────────────────────────────────

    @Test
    fun `addColumnGenerator saves and returns generator response`() {
        val config = makeTableConfig(id = 1L)
        val saved = makeGenerator(id = 5L)
        val request = ColumnGeneratorRequest(columnName = "email", generatorType = GeneratorType.EMAIL)
        whenever(tableConfigurationRepository.findById(1L)).thenReturn(Optional.of(config))
        whenever(columnGeneratorRepository.save(any<ColumnGenerator>())).thenReturn(saved)

        val response = service.addColumnGenerator(1L, request)

        assertEquals(5L, response.id)
        assertEquals("email", response.columnName)
        assertEquals(GeneratorType.EMAIL, response.generatorType)
    }

    @Test
    fun `addColumnGenerator throws when table config not found`() {
        whenever(tableConfigurationRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.addColumnGenerator(99L, ColumnGeneratorRequest(columnName = "x", generatorType = GeneratorType.NULL))
        }
    }

    // ── listColumnGenerators ───────────────────────────────────────────────

    @Test
    fun `listColumnGenerators returns generators for table`() {
        val config = makeTableConfig(id = 1L)
        val generators = listOf(makeGenerator(id = 1L), makeGenerator(id = 2L, columnName = "phone", type = GeneratorType.PHONE))
        whenever(tableConfigurationRepository.findById(1L)).thenReturn(Optional.of(config))
        whenever(columnGeneratorRepository.findByTableConfigurationId(1L)).thenReturn(generators)

        val result = service.listColumnGenerators(1L)

        assertEquals(2, result.size)
    }

    @Test
    fun `listColumnGenerators throws when table config not found`() {
        whenever(tableConfigurationRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.listColumnGenerators(99L) }
    }

    // ── updateColumnGenerator ──────────────────────────────────────────────

    @Test
    fun `updateColumnGenerator updates generator fields`() {
        val generator = makeGenerator(id = 1L, tableConfigurationId = 1L)
        val request = ColumnGeneratorRequest(columnName = "phone", generatorType = GeneratorType.PHONE)
        whenever(columnGeneratorRepository.findById(1L)).thenReturn(Optional.of(generator))
        whenever(columnGeneratorRepository.save(any<ColumnGenerator>())).thenAnswer { it.arguments[0] as ColumnGenerator }

        val response = service.updateColumnGenerator(1L, 1L, request)

        assertEquals("phone", response.columnName)
        assertEquals(GeneratorType.PHONE, response.generatorType)
    }

    @Test
    fun `updateColumnGenerator throws when generator not found`() {
        whenever(columnGeneratorRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.updateColumnGenerator(1L, 99L, ColumnGeneratorRequest(columnName = "x", generatorType = GeneratorType.NULL))
        }
    }

    @Test
    fun `updateColumnGenerator throws when generator belongs to different table`() {
        val generator = makeGenerator(id = 1L, tableConfigurationId = 2L)
        whenever(columnGeneratorRepository.findById(1L)).thenReturn(Optional.of(generator))

        assertThrows<NoSuchElementException> {
            service.updateColumnGenerator(1L, 1L, ColumnGeneratorRequest(columnName = "x", generatorType = GeneratorType.NULL))
        }
    }

    // ── deleteColumnGenerator ──────────────────────────────────────────────

    @Test
    fun `deleteColumnGenerator removes generator`() {
        val generator = makeGenerator(id = 1L, tableConfigurationId = 1L)
        whenever(columnGeneratorRepository.findById(1L)).thenReturn(Optional.of(generator))

        service.deleteColumnGenerator(1L, 1L)

        verify(columnGeneratorRepository).delete(generator)
    }

    @Test
    fun `deleteColumnGenerator throws when generator not found`() {
        whenever(columnGeneratorRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.deleteColumnGenerator(1L, 99L) }
    }

    @Test
    fun `deleteColumnGenerator throws when generator belongs to different table`() {
        val generator = makeGenerator(id = 1L, tableConfigurationId = 2L)
        whenever(columnGeneratorRepository.findById(1L)).thenReturn(Optional.of(generator))

        assertThrows<NoSuchElementException> { service.deleteColumnGenerator(1L, 1L) }
    }
}

