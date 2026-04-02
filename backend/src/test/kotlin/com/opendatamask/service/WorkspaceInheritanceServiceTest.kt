package com.opendatamask.service

import com.opendatamask.domain.model.*
import com.opendatamask.adapter.output.persistence.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.util.Optional

class WorkspaceInheritanceServiceTest {

    private val workspaceRepository = mock<WorkspaceRepository>()
    private val tableConfigRepository = mock<TableConfigurationRepository>()
    private val columnGeneratorRepository = mock<ColumnGeneratorRepository>()
    private val inheritedConfigRepository = mock<InheritedConfigRepository>()

    private val service = WorkspaceInheritanceService(
        workspaceRepository,
        tableConfigRepository,
        columnGeneratorRepository,
        inheritedConfigRepository
    )

    private fun makeWorkspace(id: Long, parentId: Long? = null) =
        Workspace(id = id, name = "ws-$id", ownerId = 1L, parentWorkspaceId = parentId)

    private fun makeTableConfig(id: Long, workspaceId: Long, tableName: String) =
        TableConfiguration(id = id, workspaceId = workspaceId, tableName = tableName, mode = TableMode.MASK)

    private fun makeGenerator(id: Long, tableConfigId: Long, columnName: String) =
        ColumnGenerator(id = id, tableConfigurationId = tableConfigId, columnName = columnName, generatorType = GeneratorType.EMAIL)

    @Test
    fun `inheritFromParent copies 3 table configs and 5 generators to child`() {
        val parentWsId = 1L
        val childWsId = 2L

        val parentConfigs = listOf(
            makeTableConfig(10L, parentWsId, "users"),
            makeTableConfig(11L, parentWsId, "orders"),
            makeTableConfig(12L, parentWsId, "products")
        )
        whenever(tableConfigRepository.findByWorkspaceId(parentWsId)).thenReturn(parentConfigs)

        // Child has no existing configs
        whenever(tableConfigRepository.findByWorkspaceIdAndTableName(eq(childWsId), any())).thenReturn(Optional.empty())

        // Save returns entities with new IDs (20, 21, 22)
        var savedConfigIdCounter = 20L
        whenever(tableConfigRepository.save(any<TableConfiguration>())).thenAnswer { inv ->
            val tc = inv.arguments[0] as TableConfiguration
            TableConfiguration(id = savedConfigIdCounter++, workspaceId = tc.workspaceId, tableName = tc.tableName, mode = tc.mode)
        }

        // Generators: users→2, orders→2, products→1
        whenever(columnGeneratorRepository.findByTableConfigurationId(10L)).thenReturn(
            listOf(makeGenerator(1L, 10L, "email"), makeGenerator(2L, 10L, "name"))
        )
        whenever(columnGeneratorRepository.findByTableConfigurationId(11L)).thenReturn(
            listOf(makeGenerator(3L, 11L, "total"), makeGenerator(4L, 11L, "status"))
        )
        whenever(columnGeneratorRepository.findByTableConfigurationId(12L)).thenReturn(
            listOf(makeGenerator(5L, 12L, "price"))
        )

        // Child has no existing generators
        whenever(columnGeneratorRepository.findByTableConfigurationId(any())).thenAnswer { inv ->
            val id = inv.arguments[0] as Long
            when (id) {
                10L -> listOf(makeGenerator(1L, 10L, "email"), makeGenerator(2L, 10L, "name"))
                11L -> listOf(makeGenerator(3L, 11L, "total"), makeGenerator(4L, 11L, "status"))
                12L -> listOf(makeGenerator(5L, 12L, "price"))
                else -> emptyList()
            }
        }

        var savedGenIdCounter = 100L
        whenever(columnGeneratorRepository.save(any<ColumnGenerator>())).thenAnswer { inv ->
            val gen = inv.arguments[0] as ColumnGenerator
            ColumnGenerator(id = savedGenIdCounter++, tableConfigurationId = gen.tableConfigurationId, columnName = gen.columnName, generatorType = gen.generatorType)
        }
        whenever(inheritedConfigRepository.save(any<InheritedConfig>())).thenAnswer { it.arguments[0] }

        service.inheritFromParent(childWsId, parentWsId)

        // 3 table configs saved
        verify(tableConfigRepository, times(3)).save(any<TableConfiguration>())
        // 5 generators saved
        verify(columnGeneratorRepository, times(5)).save(any<ColumnGenerator>())
        // 3 table + 5 generator = 8 InheritedConfig records
        verify(inheritedConfigRepository, times(8)).save(any<InheritedConfig>())
    }

    @Test
    fun `inheritFromParent skips tables already in child`() {
        val parentWsId = 1L
        val childWsId = 2L

        val parentConfig = makeTableConfig(10L, parentWsId, "users")
        whenever(tableConfigRepository.findByWorkspaceId(parentWsId)).thenReturn(listOf(parentConfig))

        val existingChildConfig = makeTableConfig(20L, childWsId, "users")
        whenever(tableConfigRepository.findByWorkspaceIdAndTableName(childWsId, "users"))
            .thenReturn(Optional.of(existingChildConfig))

        whenever(columnGeneratorRepository.findByTableConfigurationId(10L)).thenReturn(emptyList())
        whenever(columnGeneratorRepository.findByTableConfigurationId(20L)).thenReturn(emptyList())

        service.inheritFromParent(childWsId, parentWsId)

        verify(tableConfigRepository, never()).save(any<TableConfiguration>())
        verify(inheritedConfigRepository, never()).save(any<InheritedConfig>())
    }

    @Test
    fun `isInherited returns true for inherited table config`() {
        val childWsId = 2L
        val inherited = InheritedConfig(
            id = 1L, childWorkspaceId = childWsId, parentWorkspaceId = 1L,
            configType = "TABLE_CONFIG", tableName = "users", columnName = null, inheritedEntityId = 20L
        )
        whenever(inheritedConfigRepository.findByChildWorkspaceIdAndTableName(childWsId, "users"))
            .thenReturn(listOf(inherited))

        assertTrue(service.isInherited(childWsId, "users", null))
        assertFalse(service.isInherited(childWsId, "users", "email"))
    }

    @Test
    fun `isInherited returns true for inherited column generator`() {
        val childWsId = 2L
        val inherited = InheritedConfig(
            id = 2L, childWorkspaceId = childWsId, parentWorkspaceId = 1L,
            configType = "COLUMN_GENERATOR", tableName = "users", columnName = "email", inheritedEntityId = 30L
        )
        whenever(inheritedConfigRepository.findByChildWorkspaceIdAndTableName(childWsId, "users"))
            .thenReturn(listOf(inherited))

        assertTrue(service.isInherited(childWsId, "users", "email"))
        assertFalse(service.isInherited(childWsId, "users", "phone"))
    }

    @Test
    fun `markAsOverridden removes InheritedConfig without deleting the entity`() {
        val inheritedId = 5L
        whenever(inheritedConfigRepository.existsById(inheritedId)).thenReturn(true)

        service.markAsOverridden(inheritedId)

        verify(inheritedConfigRepository).deleteById(inheritedId)
        // The underlying TableConfiguration or ColumnGenerator is NOT deleted
        verify(tableConfigRepository, never()).deleteById(any())
        verify(columnGeneratorRepository, never()).deleteById(any())
    }

    @Test
    fun `markAsOverridden throws when InheritedConfig not found`() {
        whenever(inheritedConfigRepository.existsById(99L)).thenReturn(false)

        assertThrows(NoSuchElementException::class.java) {
            service.markAsOverridden(99L)
        }
        verify(inheritedConfigRepository, never()).deleteById(any())
    }

    @Test
    fun `syncWithParent adds new parent configs not in child`() {
        val parentWsId = 1L
        val childWsId = 2L
        val child = makeWorkspace(childWsId, parentId = parentWsId)
        whenever(workspaceRepository.findById(childWsId)).thenReturn(Optional.of(child))

        val parentConfig = makeTableConfig(10L, parentWsId, "new_table")
        whenever(tableConfigRepository.findByWorkspaceId(parentWsId)).thenReturn(listOf(parentConfig))
        whenever(tableConfigRepository.findByWorkspaceIdAndTableName(childWsId, "new_table"))
            .thenReturn(Optional.empty())

        val savedConfig = makeTableConfig(20L, childWsId, "new_table")
        whenever(tableConfigRepository.save(any<TableConfiguration>())).thenReturn(savedConfig)
        whenever(columnGeneratorRepository.findByTableConfigurationId(10L)).thenReturn(emptyList())
        whenever(columnGeneratorRepository.findByTableConfigurationId(20L)).thenReturn(emptyList())
        whenever(inheritedConfigRepository.save(any<InheritedConfig>())).thenAnswer { it.arguments[0] }

        service.syncWithParent(childWsId)

        verify(tableConfigRepository).save(any<TableConfiguration>())
        verify(inheritedConfigRepository).save(any<InheritedConfig>())
    }

    @Test
    fun `syncWithParent throws when workspace has no parent`() {
        val childWsId = 2L
        val child = makeWorkspace(childWsId, parentId = null)
        whenever(workspaceRepository.findById(childWsId)).thenReturn(Optional.of(child))

        assertThrows(IllegalStateException::class.java) {
            service.syncWithParent(childWsId)
        }
    }

    @Test
    fun `listChildWorkspaces returns child workspaces`() {
        val parentWsId = 1L
        val parent = makeWorkspace(parentWsId)
        whenever(workspaceRepository.findById(parentWsId)).thenReturn(Optional.of(parent))
        val children = listOf(makeWorkspace(2L, parentId = parentWsId), makeWorkspace(3L, parentId = parentWsId))
        whenever(workspaceRepository.findByParentWorkspaceId(parentWsId)).thenReturn(children)

        val result = service.listChildWorkspaces(parentWsId)

        assertEquals(2, result.size)
        assertTrue(result.all { it.parentWorkspaceId == parentWsId })
    }

    @Test
    fun `syncWithParent does not overwrite already-existing overridden config`() {
        val parentWsId = 1L
        val childWsId = 2L
        val child = makeWorkspace(childWsId, parentId = parentWsId)
        whenever(workspaceRepository.findById(childWsId)).thenReturn(Optional.of(child))

        val parentConfig = makeTableConfig(10L, parentWsId, "overridden_table")
        whenever(tableConfigRepository.findByWorkspaceId(parentWsId)).thenReturn(listOf(parentConfig))

        // Child already has a config for this table (overridden locally)
        val existingChildConfig = makeTableConfig(20L, childWsId, "overridden_table")
        whenever(tableConfigRepository.findByWorkspaceIdAndTableName(childWsId, "overridden_table"))
            .thenReturn(Optional.of(existingChildConfig))
        whenever(columnGeneratorRepository.findByTableConfigurationId(10L)).thenReturn(emptyList())
        whenever(columnGeneratorRepository.findByTableConfigurationId(20L)).thenReturn(emptyList())

        service.syncWithParent(childWsId)

        // The existing config must NOT be overwritten
        verify(tableConfigRepository, never()).save(any<TableConfiguration>())
        verify(inheritedConfigRepository, never()).save(any<InheritedConfig>())
    }
}
