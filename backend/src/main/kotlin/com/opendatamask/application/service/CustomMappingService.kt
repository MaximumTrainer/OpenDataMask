package com.opendatamask.application.service

import com.opendatamask.domain.model.ColumnGenerator
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.TableConfiguration
import com.opendatamask.domain.model.TableMode
import com.opendatamask.domain.port.input.CustomMappingUseCase
import com.opendatamask.domain.port.input.dto.CustomMappingAction
import com.opendatamask.domain.port.input.dto.CustomMappingDto
import com.opendatamask.domain.port.output.ColumnGeneratorPort
import com.opendatamask.domain.port.output.TableConfigurationPort
import com.opendatamask.domain.port.output.WorkspacePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomMappingService(
    private val workspaceRepository: WorkspacePort,
    private val tableConfigurationRepository: TableConfigurationPort,
    private val columnGeneratorRepository: ColumnGeneratorPort
) : CustomMappingUseCase {

    @Transactional
    override fun applyCustomMapping(workspaceId: Long, mapping: CustomMappingDto) {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }

        val existingTables = tableConfigurationRepository.findByWorkspaceId(workspaceId)
            .associateBy { it.tableName }

        for (tableDto in mapping.tables) {
            val hasMasked = tableDto.attributes.any { it.action == CustomMappingAction.MASK }
            val mode = if (hasMasked) TableMode.MASK else TableMode.PASSTHROUGH

            val table = existingTables[tableDto.tableName] ?: TableConfiguration(
                workspaceId = workspaceId,
                tableName = tableDto.tableName
            )
            table.mode = mode
            val savedTable = tableConfigurationRepository.save(table)

            val existingGenerators = columnGeneratorRepository.findByTableConfigurationId(savedTable.id)
                .associateBy { it.columnName }

            for (attr in tableDto.attributes) {
                if (attr.action != CustomMappingAction.MASK) continue

                val generatorType = resolveGeneratorType(attr.strategy)
                val generator = existingGenerators[attr.name] ?: ColumnGenerator(
                    tableConfigurationId = savedTable.id,
                    columnName = attr.name,
                    generatorType = generatorType
                )
                generator.generatorType = generatorType
                columnGeneratorRepository.save(generator)
            }
        }
    }

    // Maps a strategy string from the custom mapping format to a GeneratorType.
    // High-level strategies: NULL, HASH, SCRAMBLE, FAKE.
    // Any other value is treated as a direct GeneratorType name; falls back to FULL_NAME.
    internal fun resolveGeneratorType(strategy: String?): GeneratorType {
        if (strategy == null) return GeneratorType.FULL_NAME
        return when (strategy.uppercase()) {
            "NULL" -> GeneratorType.NULL
            "HASH" -> GeneratorType.HASH
            "SCRAMBLE" -> GeneratorType.SCRAMBLE
            "FAKE" -> GeneratorType.FULL_NAME
            else -> runCatching { GeneratorType.valueOf(strategy.uppercase()) }.getOrDefault(GeneratorType.FULL_NAME)
        }
    }
}
