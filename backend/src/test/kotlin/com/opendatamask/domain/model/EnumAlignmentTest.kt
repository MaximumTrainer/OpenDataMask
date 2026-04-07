package com.opendatamask.domain.model

import com.opendatamask.domain.model.ConnectionType
import com.opendatamask.domain.model.GeneratorType
import com.opendatamask.domain.model.JobStatus
import com.opendatamask.domain.model.LogLevel
import com.opendatamask.domain.model.TableMode
import com.opendatamask.domain.model.UserRole
import com.opendatamask.domain.model.WorkspaceRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnumAlignmentTest {

    @Test
    fun `ConnectionType contains canonical set`() {
        val values = ConnectionType.values().map { it.name }.toSet()
        val expected = setOf("POSTGRESQL", "MONGODB", "AZURE_SQL", "MONGODB_COSMOS", "FILE", "MYSQL")
        assertEquals(expected, values, "ConnectionType values do not match canonical set")
    }

    @Test
    fun `GeneratorType contains canonical set`() {
        val values = GeneratorType.values().map { it.name }.toSet()
        val expected = setOf(
            "NAME", "EMAIL", "PHONE", "ADDRESS", "SSN", "CREDIT_CARD", "DATE", "UUID", "CONSTANT", "NULL", "CUSTOM",
            "FIRST_NAME", "LAST_NAME", "FULL_NAME",
            "STREET_ADDRESS", "CITY", "STATE", "ZIP_CODE", "COUNTRY", "POSTAL_CODE", "GPS_COORDINATES",
            "USERNAME", "PASSWORD",
            "IBAN", "SWIFT_CODE", "MONEY_AMOUNT", "BTC_ADDRESS",
            "PASSPORT_NUMBER", "DRIVERS_LICENSE", "BIRTH_DATE", "GENDER",
            "ICD_CODE", "MEDICAL_RECORD_NUMBER", "HEALTH_PLAN_NUMBER",
            "IP_ADDRESS", "IPV6_ADDRESS", "MAC_ADDRESS", "URL",
            "VIN", "LICENSE_PLATE",
            "ORGANIZATION", "ACCOUNT_NUMBER",
            "TITLE", "JOB_TITLE", "NATIONALITY",
            "COMPANY_NAME", "DEPARTMENT",
            "CURRENCY_CODE",
            "DOMAIN_NAME", "USER_AGENT",
            "LATITUDE", "LONGITUDE", "TIME_ZONE",
            "BOOLEAN", "LOREM", "TIMESTAMP",
            "CONDITIONAL", "PARTIAL_MASK", "FORMAT_PRESERVING", "SEQUENTIAL", "RANDOM_INT"
        )
        assertEquals(expected, values, "GeneratorType values do not match canonical set")
    }

    @Test
    fun `UserRole contains canonical set`() {
        val values = UserRole.values().map { it.name }.toSet()
        val expected = setOf("ADMIN", "USER", "VIEWER")
        assertEquals(expected, values, "UserRole values do not match canonical set")
    }

    @Test
    fun `WorkspaceRole contains canonical set`() {
        val values = WorkspaceRole.values().map { it.name }.toSet()
        val expected = setOf("ADMIN", "USER", "VIEWER")
        assertEquals(expected, values, "WorkspaceRole values do not match canonical set")
    }

    @Test
    fun `TableMode contains canonical set`() {
        val values = TableMode.values().map { it.name }.toSet()
        val expected = setOf("PASSTHROUGH", "MASK", "GENERATE", "SUBSET", "UPSERT", "SKIP")
        assertEquals(expected, values, "TableMode values do not match canonical set")
    }

    @Test
    fun `LogLevel contains canonical set`() {
        val values = LogLevel.values().map { it.name }.toSet()
        val expected = setOf("DEBUG", "INFO", "WARN", "ERROR")
        assertEquals(expected, values, "LogLevel values do not match canonical set")
    }

    @Test
    fun `JobStatus contains canonical set`() {
        val values = JobStatus.values().map { it.name }.toSet()
        val expected = setOf("PENDING", "RUNNING", "COMPLETED", "FAILED", "CANCELLED")
        assertEquals(expected, values, "JobStatus values do not match canonical set")
    }
}
