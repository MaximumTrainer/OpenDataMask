package com.opendatamask.service

import com.opendatamask.connector.ColumnInfo
import com.opendatamask.connector.DatabaseConnector
import com.opendatamask.model.ConnectionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DestinationSchemaService {
    private val logger = LoggerFactory.getLogger(DestinationSchemaService::class.java)

    private val postgresTypeMap = mapOf(
        "int4" to "INTEGER", "int8" to "BIGINT", "int2" to "SMALLINT",
        "float4" to "FLOAT", "float8" to "DOUBLE PRECISION",
        "bool" to "BOOLEAN", "text" to "TEXT",
        "timestamptz" to "TIMESTAMP", "timestamp" to "TIMESTAMP",
        "date" to "DATE", "uuid" to "VARCHAR(36)",
        "jsonb" to "TEXT", "json" to "TEXT", "bytea" to "TEXT"
    )

    fun mapColumnType(sourceType: String, sourceDb: ConnectionType, destDb: ConnectionType): String {
        val normalized = postgresTypeMap[sourceType.lowercase()] ?: sourceType.uppercase()
        return when (destDb) {
            ConnectionType.MONGODB, ConnectionType.MONGODB_COSMOS -> "mixed"
            else -> normalized
        }
    }

    fun mirrorSchema(
        sourceConnector: DatabaseConnector,
        sourceType: ConnectionType,
        destConnector: DatabaseConnector,
        destType: ConnectionType,
        tableName: String
    ) {
        logger.info("Mirroring schema for table: $tableName ($sourceType -> $destType)")
        val sourceColumns = sourceConnector.listColumns(tableName)
        val destColumns = sourceColumns.map { col ->
            ColumnInfo(
                name = col.name,
                type = mapColumnType(col.type, sourceType, destType),
                nullable = col.nullable
            )
        }
        destConnector.createTable(tableName, destColumns)
    }

    fun validateCompatibility(sourceType: ConnectionType, destType: ConnectionType) {
        val unsupportedPairs = setOf<Pair<ConnectionType, ConnectionType>>(
            // All pairs are currently supported; add restrictions here if needed
        )
        if (Pair(sourceType, destType) in unsupportedPairs) {
            throw IllegalArgumentException(
                "Unsupported connection type pair: $sourceType -> $destType"
            )
        }
    }
}
