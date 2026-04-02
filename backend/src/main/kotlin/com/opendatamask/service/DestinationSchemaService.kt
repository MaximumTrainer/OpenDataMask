package com.opendatamask.service

import com.opendatamask.connector.ColumnInfo
import com.opendatamask.connector.DatabaseConnector
import com.opendatamask.domain.model.ConnectionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DestinationSchemaService {
    private val logger = LoggerFactory.getLogger(DestinationSchemaService::class.java)

    // Normalize PostgreSQL internal type names to portable SQL types
    private val postgresTypeMap = mapOf(
        "int4" to "INTEGER", "int8" to "BIGINT", "int2" to "SMALLINT",
        "float4" to "FLOAT", "float8" to "DOUBLE PRECISION",
        "bool" to "BOOLEAN", "text" to "TEXT",
        "timestamptz" to "TIMESTAMP", "timestamp" to "TIMESTAMP",
        "date" to "DATE", "uuid" to "VARCHAR(36)",
        "jsonb" to "TEXT", "json" to "TEXT", "bytea" to "TEXT"
    )

    // Normalize Azure SQL / SQL Server type names to portable SQL types
    private val azureSqlTypeMap = mapOf(
        "int" to "INTEGER", "bigint" to "BIGINT", "smallint" to "SMALLINT", "tinyint" to "SMALLINT",
        "float" to "FLOAT", "real" to "FLOAT", "double precision" to "DOUBLE PRECISION",
        "bit" to "BOOLEAN",
        "nvarchar" to "TEXT", "varchar" to "TEXT", "nchar" to "TEXT", "char" to "TEXT",
        "ntext" to "TEXT", "text" to "TEXT",
        "datetime" to "TIMESTAMP", "datetime2" to "TIMESTAMP", "smalldatetime" to "TIMESTAMP",
        "date" to "DATE", "time" to "TIME",
        "uniqueidentifier" to "VARCHAR(36)",
        "varbinary" to "TEXT", "binary" to "TEXT", "image" to "TEXT",
        "money" to "DECIMAL(19,4)", "smallmoney" to "DECIMAL(10,4)",
        "decimal" to "DECIMAL", "numeric" to "DECIMAL",
        "xml" to "TEXT"
    )

    // Re-map portable SQL types to Azure SQL / SQL Server equivalents
    private val toAzureSqlTypeMap = mapOf(
        "INTEGER" to "INT", "BIGINT" to "BIGINT", "SMALLINT" to "SMALLINT",
        "FLOAT" to "FLOAT", "DOUBLE PRECISION" to "FLOAT",
        "BOOLEAN" to "BIT", "TEXT" to "NVARCHAR(MAX)",
        "TIMESTAMP" to "DATETIME2", "DATE" to "DATE", "TIME" to "TIME",
        "VARCHAR(36)" to "NVARCHAR(36)",
        // Catch bare string type names that may come through the fallback path (e.g. VARCHAR,
        // CHAR, NVARCHAR without a length) — SQL Server requires a length, so map to NVARCHAR(MAX).
        "VARCHAR" to "NVARCHAR(MAX)", "CHAR" to "NVARCHAR(MAX)",
        "NVARCHAR" to "NVARCHAR(MAX)", "NCHAR" to "NVARCHAR(MAX)",
        "DECIMAL(19,4)" to "DECIMAL(19,4)", "DECIMAL(10,4)" to "DECIMAL(10,4)", "DECIMAL" to "DECIMAL"
    )

    fun mapColumnType(sourceType: String, sourceDb: ConnectionType, destDb: ConnectionType): String {
        // Step 1: normalize the source type to a portable representation
        val normalized = when (sourceDb) {
            ConnectionType.POSTGRESQL ->
                postgresTypeMap[sourceType.lowercase()] ?: sourceType.uppercase()
            ConnectionType.AZURE_SQL ->
                azureSqlTypeMap[sourceType.lowercase()] ?: sourceType.uppercase()
            ConnectionType.MONGODB, ConnectionType.MONGODB_COSMOS -> "mixed"
            ConnectionType.FILE ->
                if (sourceType.equals("mixed", ignoreCase = true)) "TEXT" else sourceType.uppercase()
            ConnectionType.MYSQL ->
                postgresTypeMap[sourceType.lowercase()] ?: sourceType.uppercase()
        }

        // Step 2: translate the portable type to the destination dialect
        return when (destDb) {
            ConnectionType.MONGODB, ConnectionType.MONGODB_COSMOS -> "mixed"
            ConnectionType.AZURE_SQL -> toAzureSqlTypeMap[normalized] ?: normalized
            ConnectionType.POSTGRESQL, ConnectionType.FILE, ConnectionType.MYSQL -> normalized
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
        // FILE connectors derive schema from the file format at runtime; schema mirroring
        // to a FILE destination is not meaningful (createTable is a no-op there).
        if (destType == ConnectionType.FILE) {
            throw IllegalArgumentException(
                "FILE is not a valid destination for schema-mirrored jobs. " +
                    "Use a database connection type as the destination."
            )
        }

        // MongoDB sources expose only a sampled 'mixed' schema, so writing to a strict SQL
        // destination may lose type fidelity — emit a warning so operators are aware.
        if (sourceType in setOf(ConnectionType.MONGODB, ConnectionType.MONGODB_COSMOS) &&
            destType in setOf(ConnectionType.POSTGRESQL, ConnectionType.AZURE_SQL)
        ) {
            logger.warn(
                "Source is a document store ($sourceType) and destination is a relational database " +
                    "($destType). Column types will be inferred from a single document sample and " +
                    "may not reflect the full collection schema."
            )
        }
    }
}
