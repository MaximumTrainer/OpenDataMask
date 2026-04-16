package com.opendatamask.application.service

import com.opendatamask.domain.model.CustomDataMapping
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.MappingAction
import com.opendatamask.domain.model.MaskingStrategy
import com.opendatamask.domain.port.input.dto.BulkCustomDataMappingRequest
import com.opendatamask.domain.port.input.dto.CustomDataMappingRequest
import com.opendatamask.adapter.output.persistence.CustomDataMappingRepository
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
class CustomDataMappingServiceTest {

    @Mock private lateinit var customDataMappingRepository: CustomDataMappingRepository

    @InjectMocks
    private lateinit var service: CustomDataMappingService

    private fun makeMapping(
        id: Long = 1L,
        workspaceId: Long = 10L,
        connectionId: Long = 2L,
        tableName: String = "users",
        columnName: String = "email",
        action: MappingAction = MappingAction.MASK,
        maskingStrategy: MaskingStrategy? = MaskingStrategy.FAKE,
        fakeGeneratorType: GeneratorType? = GeneratorType.EMAIL
    ) = CustomDataMapping(
        id = id,
        workspaceId = workspaceId,
        connectionId = connectionId,
        tableName = tableName,
        columnName = columnName,
        action = action,
        maskingStrategy = maskingStrategy,
        fakeGeneratorType = fakeGeneratorType
    )

    // ── createMapping ──────────────────────────────────────────────────────

    @Test
    fun `createMapping saves and returns response`() {
        val saved = makeMapping(id = 1L)
        val request = CustomDataMappingRequest(
            connectionId = 2L,
            tableName = "users",
            columnName = "email",
            action = MappingAction.MASK,
            maskingStrategy = MaskingStrategy.FAKE,
            fakeGeneratorType = GeneratorType.EMAIL
        )
        whenever(customDataMappingRepository.save(any<CustomDataMapping>())).thenReturn(saved)

        val response = service.createMapping(10L, request)

        assertEquals(1L, response.id)
        assertEquals(10L, response.workspaceId)
        assertEquals("users", response.tableName)
        assertEquals("email", response.columnName)
        assertEquals(MappingAction.MASK, response.action)
        assertEquals(MaskingStrategy.FAKE, response.maskingStrategy)
        assertEquals(GeneratorType.EMAIL, response.fakeGeneratorType)
    }

    @Test
    fun `createMapping with MIGRATE_AS_IS action has null masking fields`() {
        val saved = makeMapping(
            id = 1L, action = MappingAction.MIGRATE_AS_IS,
            maskingStrategy = null, fakeGeneratorType = null
        )
        val request = CustomDataMappingRequest(
            connectionId = 2L,
            tableName = "users",
            columnName = "id",
            action = MappingAction.MIGRATE_AS_IS
        )
        whenever(customDataMappingRepository.save(any<CustomDataMapping>())).thenReturn(saved)

        val response = service.createMapping(10L, request)

        assertEquals(MappingAction.MIGRATE_AS_IS, response.action)
        assertNull(response.maskingStrategy)
        assertNull(response.fakeGeneratorType)
    }

    // ── getMapping ─────────────────────────────────────────────────────────

    @Test
    fun `getMapping returns mapping by id`() {
        val mapping = makeMapping(id = 1L, workspaceId = 10L)
        whenever(customDataMappingRepository.findById(1L)).thenReturn(Optional.of(mapping))

        val response = service.getMapping(10L, 1L)

        assertEquals(1L, response.id)
        assertEquals("email", response.columnName)
    }

    @Test
    fun `getMapping throws when mapping not found`() {
        whenever(customDataMappingRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.getMapping(10L, 99L) }
    }

    @Test
    fun `getMapping throws when mapping belongs to different workspace`() {
        val mapping = makeMapping(id = 1L, workspaceId = 20L)
        whenever(customDataMappingRepository.findById(1L)).thenReturn(Optional.of(mapping))

        assertThrows<NoSuchElementException> { service.getMapping(10L, 1L) }
    }

    // ── listMappings ───────────────────────────────────────────────────────

    @Test
    fun `listMappings returns all mappings for workspace`() {
        val mappings = listOf(makeMapping(id = 1L), makeMapping(id = 2L, columnName = "phone"))
        whenever(customDataMappingRepository.findByWorkspaceId(10L)).thenReturn(mappings)

        val result = service.listMappings(10L)

        assertEquals(2, result.size)
    }

    // ── listMappingsForTable ───────────────────────────────────────────────

    @Test
    fun `listMappingsForTable returns mappings for specified table`() {
        val mappings = listOf(
            makeMapping(id = 1L, tableName = "users", columnName = "email"),
            makeMapping(id = 2L, tableName = "users", columnName = "phone")
        )
        whenever(customDataMappingRepository.findByWorkspaceIdAndConnectionIdAndTableName(10L, 2L, "users"))
            .thenReturn(mappings)

        val result = service.listMappingsForTable(10L, 2L, "users")

        assertEquals(2, result.size)
    }

    // ── updateMapping ──────────────────────────────────────────────────────

    @Test
    fun `updateMapping updates and returns updated response`() {
        val existing = makeMapping(id = 1L, workspaceId = 10L, action = MappingAction.MIGRATE_AS_IS)
        val request = CustomDataMappingRequest(
            connectionId = 2L,
            tableName = "users",
            columnName = "email",
            action = MappingAction.MASK,
            maskingStrategy = MaskingStrategy.NULL
        )
        whenever(customDataMappingRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(customDataMappingRepository.save(any<CustomDataMapping>())).thenAnswer { it.arguments[0] as CustomDataMapping }

        val response = service.updateMapping(10L, 1L, request)

        assertEquals(MappingAction.MASK, response.action)
        assertEquals(MaskingStrategy.NULL, response.maskingStrategy)
    }

    @Test
    fun `updateMapping throws when mapping not found`() {
        whenever(customDataMappingRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> {
            service.updateMapping(10L, 99L, CustomDataMappingRequest(
                connectionId = 2L, tableName = "users", columnName = "email",
                action = MappingAction.MIGRATE_AS_IS
            ))
        }
    }

    // ── deleteMapping ──────────────────────────────────────────────────────

    @Test
    fun `deleteMapping removes mapping`() {
        val mapping = makeMapping(id = 1L, workspaceId = 10L)
        whenever(customDataMappingRepository.findById(1L)).thenReturn(Optional.of(mapping))

        service.deleteMapping(10L, 1L)

        verify(customDataMappingRepository).deleteById(1L)
    }

    @Test
    fun `deleteMapping throws when mapping not found`() {
        whenever(customDataMappingRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { service.deleteMapping(10L, 99L) }
    }

    // ── saveBulkMappings ───────────────────────────────────────────────────

    @Test
    fun `saveBulkMappings replaces existing mappings for table and returns all`() {
        val request = BulkCustomDataMappingRequest(
            connectionId = 2L,
            tableName = "users",
            columnMappings = listOf(
                BulkCustomDataMappingRequest.ColumnMappingEntry("id", MappingAction.MIGRATE_AS_IS),
                BulkCustomDataMappingRequest.ColumnMappingEntry("email", MappingAction.MASK, MaskingStrategy.FAKE, GeneratorType.EMAIL),
                BulkCustomDataMappingRequest.ColumnMappingEntry("ssn", MappingAction.MASK, MaskingStrategy.NULL)
            )
        )
        val savedMappings = request.columnMappings.mapIndexed { i, entry ->
            CustomDataMapping(
                id = (i + 1).toLong(),
                workspaceId = 10L,
                connectionId = 2L,
                tableName = "users",
                columnName = entry.columnName,
                action = entry.action,
                maskingStrategy = entry.maskingStrategy,
                fakeGeneratorType = entry.fakeGeneratorType
            )
        }
        whenever(customDataMappingRepository.bulkSave(any<List<CustomDataMapping>>())).thenReturn(savedMappings)

        val result = service.saveBulkMappings(10L, request)

        verify(customDataMappingRepository).deleteByWorkspaceIdAndConnectionIdAndTableName(10L, 2L, "users")
        verify(customDataMappingRepository).bulkSave(any<List<CustomDataMapping>>())
        assertEquals(3, result.size)
        assertEquals("id", result[0].columnName)
        assertEquals(MappingAction.MIGRATE_AS_IS, result[0].action)
        assertEquals(MappingAction.MASK, result[1].action)
        assertEquals(MaskingStrategy.FAKE, result[1].maskingStrategy)
        assertEquals(GeneratorType.EMAIL, result[1].fakeGeneratorType)
    }

    @Test
    fun `saveBulkMappings with HASH strategy sets correct masking strategy`() {
        val request = BulkCustomDataMappingRequest(
            connectionId = 2L,
            tableName = "users",
            columnMappings = listOf(
                BulkCustomDataMappingRequest.ColumnMappingEntry("user_ref", MappingAction.MASK, MaskingStrategy.HASH)
            )
        )
        val savedMapping = CustomDataMapping(
            id = 1L, workspaceId = 10L, connectionId = 2L, tableName = "users",
            columnName = "user_ref", action = MappingAction.MASK, maskingStrategy = MaskingStrategy.HASH
        )
        whenever(customDataMappingRepository.bulkSave(any<List<CustomDataMapping>>())).thenReturn(listOf(savedMapping))

        val result = service.saveBulkMappings(10L, request)

        assertEquals(1, result.size)
        assertEquals(MaskingStrategy.HASH, result[0].maskingStrategy)
        assertNull(result[0].fakeGeneratorType)
    }
}
