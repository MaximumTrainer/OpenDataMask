package com.opendatamask.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.opendatamask.config.EncryptionService
import com.opendatamask.adapter.output.connector.ConnectorFactory
import com.opendatamask.dto.ColumnPreviewResult
import com.opendatamask.dto.PreviewSample
import com.opendatamask.adapter.output.persistence.ColumnGeneratorRepository
import com.opendatamask.adapter.output.persistence.DataConnectionRepository
import com.opendatamask.adapter.output.persistence.TableConfigurationRepository
import org.springframework.stereotype.Service

@Service
class DataPreviewService(
    private val connectorFactory: ConnectorFactory,
    private val dataConnectionRepository: DataConnectionRepository,
    private val tableConfigurationRepository: TableConfigurationRepository,
    private val columnGeneratorRepository: ColumnGeneratorRepository,
    private val generatorService: GeneratorService,
    private val encryptionService: EncryptionService
) {
    private val mapper = jacksonObjectMapper()

    fun previewColumn(
        workspaceId: Long,
        tableName: String,
        columnName: String,
        sampleSize: Int = 5
    ): ColumnPreviewResult {
        val connections = dataConnectionRepository.findByWorkspaceIdAndIsSource(workspaceId, true)
        if (connections.isEmpty()) {
            return ColumnPreviewResult(tableName, columnName, null, emptyList())
        }

        val tableConfig = tableConfigurationRepository
            .findByWorkspaceIdAndTableName(workspaceId, tableName)
            .orElse(null)
        val generator = tableConfig?.let {
            columnGeneratorRepository.findByTableConfigurationId(it.id)
                .find { gen -> gen.columnName == columnName }
        }

        val samples = try {
            val conn = connections.first()
            val decryptedConnectionString = encryptionService.decrypt(conn.connectionString)
            val decryptedPassword = conn.password?.let { encryptionService.decrypt(it) }
            val connector = connectorFactory.createConnector(
                type = conn.type,
                connectionString = decryptedConnectionString,
                username = conn.username,
                password = decryptedPassword,
                database = conn.database
            )
            val rows = connector.fetchData(tableName, limit = sampleSize)
            rows.take(sampleSize).map { row ->
                val originalValue = row[columnName]?.toString()
                val maskedValue = if (generator != null) {
                    val params: Map<String, String>? = generator.generatorParams?.let {
                        try { mapper.readValue(it) } catch (e: Exception) { null }
                    }
                    generatorService.generateValue(
                        generator.generatorType,
                        originalValue,
                        params,
                        rawParams = generator.generatorParams
                    )?.toString()
                } else {
                    originalValue
                }
                PreviewSample(originalValue, maskedValue)
            }
        } catch (e: Exception) {
            emptyList()
        }

        return ColumnPreviewResult(tableName, columnName, generator?.generatorType, samples)
    }
}
