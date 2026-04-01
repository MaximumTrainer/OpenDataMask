package com.opendatamask.service

import com.opendatamask.model.*
import com.opendatamask.repository.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.util.Optional

class WorkspaceExportServiceTest {

    private val tableConfigRepo = mock<TableConfigurationRepository>()
    private val columnGeneratorRepo = mock<ColumnGeneratorRepository>()
    private val postJobActionRepo = mock<PostJobActionRepository>()
    private val workspaceRepo = mock<WorkspaceRepository>()
    private val service = WorkspaceExportService(tableConfigRepo, columnGeneratorRepo, postJobActionRepo, workspaceRepo)

    @Test
    fun `export returns workspace config with tables and generators`() {
        val table = TableConfiguration(id = 1L, workspaceId = 1L, tableName = "users", mode = TableMode.MASK)
        val generator = ColumnGenerator(tableConfigurationId = 1L, columnName = "email", generatorType = GeneratorType.EMAIL)
        whenever(tableConfigRepo.findByWorkspaceId(1L)).thenReturn(listOf(table))
        whenever(columnGeneratorRepo.findByTableConfigurationId(1L)).thenReturn(listOf(generator))
        whenever(postJobActionRepo.findByWorkspaceId(1L)).thenReturn(emptyList())

        val config = service.export(1L)

        assertEquals("1.0", config.version)
        assertEquals(1, config.tables.size)
        assertEquals("users", config.tables[0].tableName)
        assertEquals(TableMode.MASK, config.tables[0].mode)
        assertEquals(1, config.tables[0].columnGenerators.size)
        assertEquals("email", config.tables[0].columnGenerators[0].columnName)
    }

    @Test
    fun `import creates new table configurations`() {
        val workspace = Workspace(id = 1L, name = "test", ownerId = 1L)
        whenever(workspaceRepo.findById(1L)).thenReturn(Optional.of(workspace))
        whenever(tableConfigRepo.findByWorkspaceId(1L)).thenReturn(emptyList())
        whenever(tableConfigRepo.save(any<TableConfiguration>())).thenAnswer { it.arguments[0] }
        whenever(columnGeneratorRepo.findByTableConfigurationId(any())).thenReturn(emptyList())
        whenever(columnGeneratorRepo.save(any<ColumnGenerator>())).thenAnswer { it.arguments[0] }

        val config = com.opendatamask.dto.WorkspaceConfigDto(
            tables = listOf(com.opendatamask.dto.TableConfigExportDto(
                tableName = "orders", schemaName = null, mode = TableMode.MASK,
                rowLimit = null, whereClause = null,
                columnGenerators = listOf(com.opendatamask.dto.ColumnGeneratorExportDto("total", GeneratorType.CUSTOM, null))
            ))
        )
        service.import(1L, config)

        verify(tableConfigRepo).save(any())
        verify(columnGeneratorRepo).save(any())
    }

    @Test
    fun `import throws when workspace not found`() {
        whenever(workspaceRepo.findById(999L)).thenReturn(Optional.empty())
        assertThrows(NoSuchElementException::class.java) {
            service.import(999L, com.opendatamask.dto.WorkspaceConfigDto())
        }
    }
}
