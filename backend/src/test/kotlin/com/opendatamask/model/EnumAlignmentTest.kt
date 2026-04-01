package com.opendatamask.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnumAlignmentTest {

    @Test
    fun `ConnectionType contains canonical set`() {
        val values = ConnectionType.values().map { it.name }.toSet()
        val expected = setOf("POSTGRESQL", "MONGODB", "AZURE_SQL", "MONGODB_COSMOS")
        assertEquals(expected, values, "ConnectionType values do not match canonical set")
    }

    @Test
    fun `GeneratorType contains canonical set`() {
        val values = GeneratorType.values().map { it.name }.toSet()
        val expected = setOf("NAME", "EMAIL", "PHONE", "ADDRESS", "SSN", "CREDIT_CARD", "DATE", "UUID", "CONSTANT", "NULL", "CUSTOM")
        assertEquals(expected, values, "GeneratorType values do not match canonical set")
    }

    @Test
    fun `WorkspaceRole contains VIEWER`() {
        val values = WorkspaceRole.values().map { it.name }.toSet()
        assertTrue(values.contains("VIEWER"), "WorkspaceRole must contain VIEWER")
    }
}
