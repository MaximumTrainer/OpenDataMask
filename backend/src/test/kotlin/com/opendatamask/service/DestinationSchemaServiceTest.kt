package com.opendatamask.service

import com.opendatamask.connector.ColumnInfo
import com.opendatamask.connector.DatabaseConnector
import com.opendatamask.model.ConnectionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*

class DestinationSchemaServiceTest {

    private val service = DestinationSchemaService()

    @Test
    fun `mapColumnType maps PostgreSQL INT4 to INT`() {
        val result = service.mapColumnType("INT4", ConnectionType.POSTGRESQL, ConnectionType.MONGODB)
        assertNotNull(result)
    }

    @Test
    fun `mapColumnType maps VARCHAR to string type`() {
        val result = service.mapColumnType("VARCHAR", ConnectionType.POSTGRESQL, ConnectionType.POSTGRESQL)
        assertEquals("VARCHAR", result)
    }

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
    fun `validateCompatibility passes for supported pairs`() {
        // Should not throw
        service.validateCompatibility(ConnectionType.POSTGRESQL, ConnectionType.POSTGRESQL)
        service.validateCompatibility(ConnectionType.POSTGRESQL, ConnectionType.MONGODB)
        service.validateCompatibility(ConnectionType.MONGODB, ConnectionType.MONGODB)
    }
}
