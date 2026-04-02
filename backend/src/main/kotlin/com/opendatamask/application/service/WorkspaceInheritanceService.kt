package com.opendatamask.application.service

import com.opendatamask.domain.port.input.WorkspaceInheritanceUseCase

import com.opendatamask.domain.model.*
import com.opendatamask.adapter.output.persistence.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WorkspaceInheritanceService(
    private val workspaceRepository: WorkspaceRepository,
    private val tableConfigurationRepository: TableConfigurationRepository,
    private val columnGeneratorRepository: ColumnGeneratorRepository,
    private val inheritedConfigRepository: InheritedConfigRepository
) : WorkspaceInheritanceUseCase {

    /**
     * Copies all TableConfiguration and ColumnGenerator records from the parent workspace
     * into the child workspace, skipping any tables/columns the child already has.
     * Tracks each copied record in InheritedConfig.
     */
    @Transactional
    override fun inheritFromParent(childWorkspaceId: Long, parentWorkspaceId: Long) {
        val parentTableConfigs = tableConfigurationRepository.findByWorkspaceId(parentWorkspaceId)

        for (parentConfig in parentTableConfigs) {
            val existingChildConfigOpt = tableConfigurationRepository
                .findByWorkspaceIdAndTableName(childWorkspaceId, parentConfig.tableName)

            val childConfig = if (existingChildConfigOpt.isEmpty) {
                val newConfig = tableConfigurationRepository.save(
                    TableConfiguration(
                        workspaceId = childWorkspaceId,
                        tableName = parentConfig.tableName,
                        schemaName = parentConfig.schemaName,
                        mode = parentConfig.mode,
                        rowLimit = parentConfig.rowLimit,
                        whereClause = parentConfig.whereClause
                    )
                )
                inheritedConfigRepository.save(
                    InheritedConfig(
                        childWorkspaceId = childWorkspaceId,
                        parentWorkspaceId = parentWorkspaceId,
                        configType = "TABLE_CONFIG",
                        tableName = parentConfig.tableName,
                        inheritedEntityId = newConfig.id
                    )
                )
                newConfig
            } else {
                existingChildConfigOpt.get()
            }

            val parentGenerators = columnGeneratorRepository.findByTableConfigurationId(parentConfig.id)
            val existingChildColumns = columnGeneratorRepository
                .findByTableConfigurationId(childConfig.id)
                .map { it.columnName }
                .toSet()

            for (parentGen in parentGenerators) {
                if (parentGen.columnName !in existingChildColumns) {
                    val childGen = columnGeneratorRepository.save(
                        ColumnGenerator(
                            tableConfigurationId = childConfig.id,
                            columnName = parentGen.columnName,
                            generatorType = parentGen.generatorType,
                            generatorParams = parentGen.generatorParams
                        )
                    )
                    inheritedConfigRepository.save(
                        InheritedConfig(
                            childWorkspaceId = childWorkspaceId,
                            parentWorkspaceId = parentWorkspaceId,
                            configType = "COLUMN_GENERATOR",
                            tableName = parentConfig.tableName,
                            columnName = parentGen.columnName,
                            inheritedEntityId = childGen.id
                        )
                    )
                }
            }
        }
        // TODO: Copy ColumnSensitivity records from parent when ColumnSensitivityRepository is available (R1)
    }

    /**
     * Re-syncs the child workspace with its configured parent, adding any new parent configs
     * that the child doesn't yet have. Overridden configs (removed from InheritedConfig) are untouched.
     */
    @Transactional
    override fun syncWithParent(childWorkspaceId: Long) {
        val child = workspaceRepository.findById(childWorkspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $childWorkspaceId") }
        val parentWorkspaceId = child.parentWorkspaceId
            ?: throw IllegalStateException("Workspace $childWorkspaceId has no parent workspace configured")
        inheritFromParent(childWorkspaceId, parentWorkspaceId)
    }

    /**
     * Returns true if the given table/column combination in the child workspace is still
     * tracked as inherited (i.e., not locally overridden).
     */
    @Transactional(readOnly = true)
    fun isInherited(childWorkspaceId: Long, tableName: String, columnName: String?): Boolean {
        val records = inheritedConfigRepository
            .findByChildWorkspaceIdAndTableName(childWorkspaceId, tableName)
        return records.any { it.columnName == columnName }
    }

    /**
     * Removes the InheritedConfig record without deleting the underlying entity,
     * marking the config as locally overridden.
     */
    @Transactional
    override fun markAsOverridden(inheritedConfigId: Long) {
        if (!inheritedConfigRepository.existsById(inheritedConfigId)) {
            throw NoSuchElementException("InheritedConfig not found: $inheritedConfigId")
        }
        inheritedConfigRepository.deleteById(inheritedConfigId)
    }

    /** Lists all direct child workspaces of the given parent. */
    @Transactional(readOnly = true)
    override fun listChildWorkspaces(parentWorkspaceId: Long): List<Workspace> {
        workspaceRepository.findById(parentWorkspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $parentWorkspaceId") }
        return workspaceRepository.findByParentWorkspaceId(parentWorkspaceId)
    }

    /** Lists all InheritedConfig records for the given child workspace. */
    @Transactional(readOnly = true)
    override fun listInheritedConfigs(childWorkspaceId: Long): List<InheritedConfig> {
        workspaceRepository.findById(childWorkspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $childWorkspaceId") }
        return inheritedConfigRepository.findByChildWorkspaceId(childWorkspaceId)
    }
}