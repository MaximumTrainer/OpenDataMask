package com.opendatamask.application.service

import com.opendatamask.domain.model.GeneratorPreset
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.ConsistencyMode
import com.opendatamask.domain.model.TableConfiguration
import com.opendatamask.domain.model.TableMode
import com.opendatamask.domain.port.input.dto.GeneratorPresetRequest
import com.opendatamask.adapter.output.persistence.ColumnGeneratorRepository
import com.opendatamask.adapter.output.persistence.GeneratorPresetRepository
import com.opendatamask.adapter.output.persistence.TableConfigurationRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class GeneratorPresetServiceTest {

    @Autowired
    private lateinit var generatorPresetService: GeneratorPresetService

    @Autowired
    private lateinit var generatorPresetRepository: GeneratorPresetRepository

    @Autowired
    private lateinit var columnGeneratorRepository: ColumnGeneratorRepository

    @Autowired
    private lateinit var tableConfigurationRepository: TableConfigurationRepository

    @Test
    fun `system presets are seeded at startup`() {
        val systemPresets = generatorPresetService.listSystemPresets()
        assertTrue(systemPresets.isNotEmpty(), "System presets should be seeded at startup")
        assertTrue(systemPresets.all { it.isSystem }, "All system presets should have isSystem=true")
        assertTrue(systemPresets.any { it.generatorType == GeneratorType.EMAIL })
        assertTrue(systemPresets.any { it.generatorType == GeneratorType.FIRST_NAME })
        assertTrue(systemPresets.any { it.generatorType == GeneratorType.SSN })
    }

    @Test
    fun `system presets are not duplicated when seeded twice`() {
        val countBefore = generatorPresetService.listSystemPresets().size
        // Manually invoke seeder logic (DataSeeder @PostConstruct already ran)
        val dataSeeder = DataSeeder(generatorPresetRepository)
        dataSeeder.seedSystemPresets()
        val countAfter = generatorPresetService.listSystemPresets().size
        assertEquals(countBefore, countAfter, "Seeding twice should not duplicate system presets")
    }

    @Test
    fun `createPreset saves a workspace preset`() {
        val request = GeneratorPresetRequest(name = "My Email Preset", generatorType = GeneratorType.EMAIL)
        val response = generatorPresetService.createPreset(workspaceId = 1L, request = request)

        assertNotNull(response.id)
        assertEquals("My Email Preset", response.name)
        assertEquals(GeneratorType.EMAIL, response.generatorType)
        assertEquals(1L, response.workspaceId)
        assertFalse(response.isSystem)
    }

    @Test
    fun `listWorkspacePresets returns only presets for given workspace`() {
        generatorPresetService.createPreset(1L, GeneratorPresetRequest("WS1 Preset", GeneratorType.PHONE))
        generatorPresetService.createPreset(2L, GeneratorPresetRequest("WS2 Preset", GeneratorType.NAME))

        val ws1Presets = generatorPresetService.listWorkspacePresets(1L)
        assertTrue(ws1Presets.any { it.name == "WS1 Preset" })
        assertFalse(ws1Presets.any { it.name == "WS2 Preset" })
    }

    @Test
    fun `updatePreset changes preset name and type`() {
        val created = generatorPresetService.createPreset(
            1L, GeneratorPresetRequest("Original Name", GeneratorType.PHONE)
        )
        val updated = generatorPresetService.updatePreset(
            1L, created.id, GeneratorPresetRequest("Updated Name", GeneratorType.EMAIL)
        )
        assertEquals("Updated Name", updated.name)
        assertEquals(GeneratorType.EMAIL, updated.generatorType)
    }

    @Test
    fun `updatePreset throws when preset belongs to different workspace`() {
        val created = generatorPresetService.createPreset(
            1L, GeneratorPresetRequest("WS1 Preset", GeneratorType.PHONE)
        )
        assertThrows<NoSuchElementException> {
            generatorPresetService.updatePreset(2L, created.id, GeneratorPresetRequest("Hacked", GeneratorType.EMAIL))
        }
    }

    @Test
    fun `deletePreset removes the preset`() {
        val created = generatorPresetService.createPreset(
            1L, GeneratorPresetRequest("To Delete", GeneratorType.NULL)
        )
        generatorPresetService.deletePreset(1L, created.id)
        assertFalse(generatorPresetRepository.existsById(created.id))
    }

    @Test
    fun `applyPreset updates column generator type from preset`() {
        val tableConfig = tableConfigurationRepository.save(
            TableConfiguration(workspaceId = 1L, tableName = "users", mode = TableMode.MASK)
        )
        val generator = columnGeneratorRepository.save(
            ColumnGenerator(tableConfigurationId = tableConfig.id, columnName = "email", generatorType = GeneratorType.NULL)
        )
        val preset = generatorPresetRepository.save(
            GeneratorPreset(name = "Email Preset", generatorType = GeneratorType.EMAIL, workspaceId = 1L)
        )

        val result = generatorPresetService.applyPreset(generator.id, preset.id!!)

        assertEquals(GeneratorType.EMAIL, result.generatorType)
        assertEquals(preset.id, result.presetId)
    }

    @Test
    fun `applyPresetToColumn looks up generator by workspace and table name`() {
        val tableConfig = tableConfigurationRepository.save(
            TableConfiguration(workspaceId = 10L, tableName = "orders", mode = TableMode.MASK)
        )
        val generator = columnGeneratorRepository.save(
            ColumnGenerator(tableConfigurationId = tableConfig.id, columnName = "phone", generatorType = GeneratorType.NULL)
        )
        val preset = generatorPresetRepository.save(
            GeneratorPreset(name = "Phone Preset", generatorType = GeneratorType.PHONE, workspaceId = 10L)
        )

        val result = generatorPresetService.applyPresetToColumn(10L, "orders", "phone", preset.id!!)

        assertEquals(GeneratorType.PHONE, result.generatorType)
        assertEquals(preset.id, result.presetId)
    }
}

