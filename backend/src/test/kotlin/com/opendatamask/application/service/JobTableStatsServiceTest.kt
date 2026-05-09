package com.opendatamask.application.service

import com.opendatamask.domain.model.JobTableStats
import com.opendatamask.domain.port.output.JobTableStatsPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class JobTableStatsServiceTest {

    @Mock private lateinit var jobTableStatsPort: JobTableStatsPort

    @Test
    fun `should save stats with correct table name and jobId`() {
        val stats = JobTableStats(
            jobId = 1L,
            tableName = "users",
            rowsRead = 100,
            rowsWritten = 100,
            rowsSkipped = 0,
            startedAt = LocalDateTime.now(),
            completedAt = LocalDateTime.now(),
            elapsedMs = 250
        )
        whenever(jobTableStatsPort.save(any())).thenAnswer { it.arguments[0] as JobTableStats }

        val saved = jobTableStatsPort.save(stats)

        assertEquals("users", saved.tableName)
        assertEquals(1L, saved.jobId)
        verify(jobTableStatsPort).save(stats)
    }

    @Test
    fun `should compute rowsPerSecond correctly`() {
        val stats = JobTableStats(
            jobId = 2L,
            tableName = "orders",
            rowsRead = 1000,
            rowsWritten = 1000,
            rowsSkipped = 0,
            startedAt = LocalDateTime.now(),
            completedAt = LocalDateTime.now(),
            elapsedMs = 2000,
            rowsPerSecond = 500.0
        )
        whenever(jobTableStatsPort.save(any())).thenAnswer { it.arguments[0] as JobTableStats }

        val saved = jobTableStatsPort.save(stats)

        assertEquals(500.0, saved.rowsPerSecond)
    }

    @Test
    fun `should return empty list when no stats exist for job`() {
        whenever(jobTableStatsPort.findByJobId(999L)).thenReturn(emptyList())

        val result = jobTableStatsPort.findByJobId(999L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should return stats for all tables in a job`() {
        val stats = listOf(
            JobTableStats(jobId = 5L, tableName = "users", rowsRead = 50, rowsWritten = 50, rowsSkipped = 0,
                startedAt = LocalDateTime.now(), elapsedMs = 100),
            JobTableStats(jobId = 5L, tableName = "orders", rowsRead = 200, rowsWritten = 200, rowsSkipped = 0,
                startedAt = LocalDateTime.now(), elapsedMs = 300)
        )
        whenever(jobTableStatsPort.findByJobId(5L)).thenReturn(stats)

        val result = jobTableStatsPort.findByJobId(5L)

        assertEquals(2, result.size)
        assertEquals("users", result[0].tableName)
        assertEquals("orders", result[1].tableName)
    }

    @Test
    fun `should track rows skipped separately from rows written`() {
        val stats = JobTableStats(
            jobId = 3L,
            tableName = "audit_log",
            rowsRead = 500,
            rowsWritten = 400,
            rowsSkipped = 100,
            startedAt = LocalDateTime.now(),
            elapsedMs = 150
        )
        whenever(jobTableStatsPort.save(any())).thenAnswer { it.arguments[0] as JobTableStats }

        val saved = jobTableStatsPort.save(stats)

        assertEquals(500L, saved.rowsRead)
        assertEquals(400L, saved.rowsWritten)
        assertEquals(100L, saved.rowsSkipped)
    }
}
