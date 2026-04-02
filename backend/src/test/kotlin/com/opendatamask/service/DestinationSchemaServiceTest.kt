package com.opendatamask.service

import com.opendatamask.adapter.output.connector.ColumnInfo
import com.opendatamask.adapter.output.connector.DatabaseConnector
import com.opendatamask.domain.model.ConnectionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*

class DestinationSchemaServiceTest {

    private val service = DestinationSchemaService()

    // ── mapColumnType – PostgreSQL source ─────────────────────────────────

    @Test
    fun `mapColumnType maps PostgreSQL INT4 to mixed for MongoDB destination`() {
        val result = service.mapColumnType("INT4", ConnectionType.POSTGRESQL, ConnectionType.MONGODB)
        assertEquals("mixed", result)
    }

    @Test
    fun `mapColumnType maps PostgreSQL INT4 to INTEGER for PostgreSQL destination`() {
        val result = service.mapColumnType("int4", ConnectionType.POSTGRESQL, ConnectionType.POSTGRESQL)
        assertEquals("INTEGER", result)
    }

    @Test
    fun `mapColumnType maps VARCHAR to string type for PostgreSQL destination`() {
        val result = service.mapColumnType("VARCHAR", ConnectionType.POSTGRESQL, ConnectionType.POSTGRESQL)
        assertEquals("VARCHAR", result)
    }

    @Test
    fun `mapColumnType maps bare PostgreSQL VARCHAR to NVARCHAR MAX for Azure SQL destination`() {
        val result = service.mapColumnType("VARCHAR", ConnectionType.POSTGRESQL, ConnectionType.AZURE_SQL)
        assertEquals("NVARCHAR(MAX)", result)
    }

    @Test
    fun `mapColumnType maps PostgreSQL bool to BIT for Azure SQL destination`() {
        val result = service.mapColumnType("bool", ConnectionType.POSTGRESQL, ConnectionType.AZURE_SQL)
        assertEquals("BIT", result)
    }

    @Test
    fun `mapColumnType maps PostgreSQL text to NVARCHAR MAX for Azure SQL destination`() {
        val result = service.mapColumnType("text", ConnectionType.POSTGRESQL, ConnectionType.AZURE_SQL)
        assertEquals("NVARCHAR(MAX)", result)
    }

    @Test
    fun `mapColumnType maps PostgreSQL timestamptz to DATETIME2 for Azure SQL destination`() {
        val result = service.mapColumnType("timestamptz", ConnectionType.POSTGRESQL, ConnectionType.AZURE_SQL)
        assertEquals("DATETIME2", result)
    }

    @Test
    fun `mapColumnType maps PostgreSQL uuid to NVARCHAR 36 for Azure SQL destination`() {
        val result = service.mapColumnType("uuid", ConnectionType.POSTGRESQL, ConnectionType.AZURE_SQL)
        assertEquals("NVARCHAR(36)", result)
    }

    // ── mapColumnType – Azure SQL source ──────────────────────────────────

    @Test
    fun `mapColumnType maps Azure SQL int to INTEGER for PostgreSQL destination`() {
        val result = service.mapColumnType("int", ConnectionType.AZURE_SQL, ConnectionType.POSTGRESQL)
        assertEquals("INTEGER", result)
    }

    @Test
    fun `mapColumnType maps Azure SQL bigint to BIGINT for PostgreSQL destination`() {
        val result = service.mapColumnType("bigint", ConnectionType.AZURE_SQL, ConnectionType.POSTGRESQL)
        assertEquals("BIGINT", result)
    }

    @Test
    fun `mapColumnType maps Azure SQL nvarchar to TEXT for PostgreSQL destination`() {
        val result = service.mapColumnType("nvarchar", ConnectionType.AZURE_SQL, ConnectionType.POSTGRESQL)
        assertEquals("TEXT", result)
    }

    @Test
    fun `mapColumnType maps Azure SQL bit to BOOLEAN for PostgreSQL destination`() {
        val result = service.mapColumnType("bit", ConnectionType.AZURE_SQL, ConnectionType.POSTGRESQL)
        assertEquals("BOOLEAN", result)
    }

    @Test
    fun `mapColumnType maps Azure SQL int to INT for Azure SQL destination`() {
        val result = service.mapColumnType("int", ConnectionType.AZURE_SQL, ConnectionType.AZURE_SQL)
        assertEquals("INT", result)
    }

    @Test
    fun `mapColumnType maps Azure SQL bit to BIT for Azure SQL destination`() {
        val result = service.mapColumnType("bit", ConnectionType.AZURE_SQL, ConnectionType.AZURE_SQL)
        assertEquals("BIT", result)
    }

    @Test
    fun `mapColumnType maps Azure SQL datetime2 to mixed for MongoDB destination`() {
        val result = service.mapColumnType("datetime2", ConnectionType.AZURE_SQL, ConnectionType.MONGODB)
        assertEquals("mixed", result)
    }

    // ── mapColumnType – MongoDB source ────────────────────────────────────

    @Test
    fun `mapColumnType maps MongoDB mixed type to mixed for PostgreSQL destination`() {
        val result = service.mapColumnType("mixed", ConnectionType.MONGODB, ConnectionType.POSTGRESQL)
        assertEquals("mixed", result)
    }

    @Test
    fun `mapColumnType maps MongoDB mixed type to mixed for Azure SQL destination`() {
        val result = service.mapColumnType("mixed", ConnectionType.MONGODB, ConnectionType.AZURE_SQL)
        assertEquals("mixed", result)
    }

    // ── mapColumnType – FILE source ───────────────────────────────────────

    @Test
    fun `mapColumnType maps FILE mixed type to TEXT for PostgreSQL destination`() {
        val result = service.mapColumnType("mixed", ConnectionType.FILE, ConnectionType.POSTGRESQL)
        assertEquals("TEXT", result)
    }

    @Test
    fun `mapColumnType maps FILE mixed type to NVARCHAR MAX for Azure SQL destination`() {
        val result = service.mapColumnType("mixed", ConnectionType.FILE, ConnectionType.AZURE_SQL)
        assertEquals("NVARCHAR(MAX)", result)
    }

    @Test
    fun `mapColumnType maps FILE VARCHAR to NVARCHAR MAX for Azure SQL destination`() {
        val result = service.mapColumnType("VARCHAR", ConnectionType.FILE, ConnectionType.AZURE_SQL)
        assertEquals("NVARCHAR(MAX)", result)
    }

    // ── mirrorSchema ──────────────────────────────────────────────────────

    @Test
    fun `mirrorSchema calls createTable on destination with mapped columns`() {
        val sourceConnector = mock<DatabaseConnector>()
        val destConnector = mock<DatabaseConnector>()
        val columns = listOf(
            ColumnInfo("id", "INT4", false),
            ColumnInfo("name", "VARCHAR", true)
        )
        whenever(sourceConnector.listColumns("users")).thenReturn(columns)

        service.mirrorSchema(
            sourceConnector, ConnectionType.POSTGRESQL,
            destConnector, ConnectionType.POSTGRESQL,
            "users"
        )

        verify(destConnector).createTable(eq("users"), any())
    }

    @Test
    fun `mirrorSchema translates PostgreSQL columns to Azure SQL types`() {
        val sourceConnector = mock<DatabaseConnector>()
        val destConnector = mock<DatabaseConnector>()
        val columns = listOf(
            ColumnInfo("id", "int4", false),
            ColumnInfo("active", "bool", true)
        )
        whenever(sourceConnector.listColumns("users")).thenReturn(columns)

        service.mirrorSchema(
            sourceConnector, ConnectionType.POSTGRESQL,
            destConnector, ConnectionType.AZURE_SQL,
            "users"
        )

        verify(destConnector).createTable(
            eq("users"),
            argThat { cols ->
                cols.any { it.name == "id" && it.type == "INT" } &&
                    cols.any { it.name == "active" && it.type == "BIT" }
            }
        )
    }

    // ── validateCompatibility ─────────────────────────────────────────────

    @Test
    fun `validateCompatibility passes for homogeneous SQL pairs`() {
        service.validateCompatibility(ConnectionType.POSTGRESQL, ConnectionType.POSTGRESQL)
        service.validateCompatibility(ConnectionType.AZURE_SQL, ConnectionType.AZURE_SQL)
    }

    @Test
    fun `validateCompatibility passes for heterogeneous SQL pairs`() {
        service.validateCompatibility(ConnectionType.POSTGRESQL, ConnectionType.AZURE_SQL)
    }

    @Test
    fun `validateCompatibility passes for SQL to MongoDB destination`() {
        service.validateCompatibility(ConnectionType.POSTGRESQL, ConnectionType.MONGODB)
        service.validateCompatibility(ConnectionType.AZURE_SQL, ConnectionType.MONGODB_COSMOS)
    }

    @Test
    fun `validateCompatibility passes for MongoDB to MongoDB`() {
        service.validateCompatibility(ConnectionType.MONGODB, ConnectionType.MONGODB)
        service.validateCompatibility(ConnectionType.MONGODB_COSMOS, ConnectionType.MONGODB_COSMOS)
    }

    @Test
    fun `validateCompatibility passes for MongoDB source to SQL destination with warning`() {
        // Should not throw — a warning is logged instead
        service.validateCompatibility(ConnectionType.MONGODB, ConnectionType.POSTGRESQL)
        service.validateCompatibility(ConnectionType.MONGODB_COSMOS, ConnectionType.AZURE_SQL)
    }

    @Test
    fun `validateCompatibility throws for FILE destination`() {
        assertThrows<IllegalArgumentException> {
            service.validateCompatibility(ConnectionType.POSTGRESQL, ConnectionType.FILE)
        }
    }

    @Test
    fun `validateCompatibility throws for FILE destination regardless of source`() {
        ConnectionType.values().filter { it != ConnectionType.FILE }.forEach { sourceType ->
            assertThrows<IllegalArgumentException>(
                "Expected exception for source=$sourceType -> dest=FILE"
            ) {
                service.validateCompatibility(sourceType, ConnectionType.FILE)
            }
        }
    }
}
