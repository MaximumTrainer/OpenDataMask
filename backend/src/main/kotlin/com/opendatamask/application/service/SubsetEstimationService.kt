package com.opendatamask.application.service

import com.opendatamask.domain.port.output.ConnectorFactoryPort
import com.opendatamask.domain.model.SubsetLimitType
import com.opendatamask.domain.port.input.dto.SubsetEstimateResponse
import com.opendatamask.domain.port.input.dto.TableEstimate
import com.opendatamask.domain.port.output.DataConnectionPort
import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.domain.port.output.SubsetTableConfigPort
import org.springframework.stereotype.Service

@Service
class SubsetEstimationService(
    private val subsetTableConfigPort: SubsetTableConfigPort,
    private val dataConnectionPort: DataConnectionPort,
    private val encryptionPort: EncryptionPort,
    private val connectorFactory: ConnectorFactoryPort
) {
    fun estimate(workspaceId: Long): SubsetEstimateResponse {
        val connections = dataConnectionPort.findByWorkspaceId(workspaceId)
        val sourceConn = connections.firstOrNull { it.isSource }
            ?: return SubsetEstimateResponse(
                workspaceId = workspaceId,
                totalEstimatedRows = 0L,
                success = false,
                errorMessage = "No source connection configured for workspace $workspaceId",
                tableEstimates = emptyList()
            )

        val configs = subsetTableConfigPort.findByWorkspaceId(workspaceId)
        if (configs.isEmpty()) {
            return SubsetEstimateResponse(
                workspaceId = workspaceId,
                totalEstimatedRows = 0L,
                success = true,
                tableEstimates = emptyList()
            )
        }

        val connStr = encryptionPort.decrypt(sourceConn.connectionString)
        val connector = connectorFactory.createConnector(
            sourceConn.type, connStr, null, null, null
        )

        val estimates = configs.map { config ->
            val totalRows = connector.countRows(config.tableName, null)
            val estimatedRows = when (config.limitType) {
                SubsetLimitType.PERCENTAGE -> (totalRows * config.limitValue / 100.0).toLong().coerceAtMost(totalRows)
                SubsetLimitType.ROW_COUNT -> config.limitValue.toLong().coerceAtMost(totalRows)
                SubsetLimitType.ALL -> totalRows
            }
            TableEstimate(
                tableName = config.tableName,
                totalRows = totalRows,
                estimatedRows = estimatedRows,
                limitType = config.limitType,
                limitValue = config.limitValue
            )
        }

        return SubsetEstimateResponse(
            workspaceId = workspaceId,
            totalEstimatedRows = estimates.sumOf { it.estimatedRows },
            success = true,
            tableEstimates = estimates
        )
    }
}
