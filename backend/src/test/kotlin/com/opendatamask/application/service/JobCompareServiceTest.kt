package com.opendatamask.application.service

import com.opendatamask.domain.model.Job
import com.opendatamask.domain.model.JobStatus
import com.opendatamask.domain.model.JobTableStats
import com.opendatamask.domain.port.output.JobPort
import com.opendatamask.domain.port.output.JobTableStatsPort
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

class JobCompareServiceTest {

    private val jobPort: JobPort = mock()
    private val jobTableStatsPort: JobTableStatsPort = mock()
    private val service = JobCompareService(jobPort, jobTableStatsPort)

    private fun makeJob(id: Long) = Job(
        id = id,
        workspaceId = 1L,
        status = JobStatus.COMPLETED,
        createdBy = 1L,
        createdAt = LocalDateTime.now()
    )

    private fun makeStats(jobId: Long, tableName: String, rowsRead: Long) = JobTableStats(
        jobId = jobId,
        tableName = tableName,
        rowsRead = rowsRead,
        startedAt = LocalDateTime.now()
    )

    @Test
    fun `compareJobs returns rowsDelta as difference in rows read`() {
        whenever(jobPort.findById(1L)).thenReturn(Optional.of(makeJob(1L)))
        whenever(jobPort.findById(2L)).thenReturn(Optional.of(makeJob(2L)))
        whenever(jobTableStatsPort.findByJobId(1L)).thenReturn(listOf(
            makeStats(1L, "users", 100L),
            makeStats(1L, "orders", 200L)
        ))
        whenever(jobTableStatsPort.findByJobId(2L)).thenReturn(listOf(
            makeStats(2L, "users", 150L),
            makeStats(2L, "orders", 180L)
        ))

        val result = service.compareJobs(1L, 1L, 2L)

        assertEquals(1L, result.jobAId)
        assertEquals(2L, result.jobBId)
        assertEquals(30L, result.rowsDelta) // (150+180) - (100+200)
    }

    @Test
    fun `compareJobs identifies tables added in job B`() {
        whenever(jobPort.findById(1L)).thenReturn(Optional.of(makeJob(1L)))
        whenever(jobPort.findById(2L)).thenReturn(Optional.of(makeJob(2L)))
        whenever(jobTableStatsPort.findByJobId(1L)).thenReturn(listOf(
            makeStats(1L, "users", 100L)
        ))
        whenever(jobTableStatsPort.findByJobId(2L)).thenReturn(listOf(
            makeStats(2L, "users", 100L),
            makeStats(2L, "payments", 50L)
        ))

        val result = service.compareJobs(1L, 1L, 2L)

        assertEquals(listOf("payments"), result.tablesAddedInB)
        assertTrue(result.tablesRemovedInB.isEmpty())
        assertEquals(listOf("users"), result.tablesInCommon)
    }

    @Test
    fun `compareJobs identifies tables removed in job B`() {
        whenever(jobPort.findById(1L)).thenReturn(Optional.of(makeJob(1L)))
        whenever(jobPort.findById(2L)).thenReturn(Optional.of(makeJob(2L)))
        whenever(jobTableStatsPort.findByJobId(1L)).thenReturn(listOf(
            makeStats(1L, "users", 100L),
            makeStats(1L, "legacy_table", 500L)
        ))
        whenever(jobTableStatsPort.findByJobId(2L)).thenReturn(listOf(
            makeStats(2L, "users", 110L)
        ))

        val result = service.compareJobs(1L, 1L, 2L)

        assertEquals(listOf("legacy_table"), result.tablesRemovedInB)
        assertTrue(result.tablesAddedInB.isEmpty())
    }

    @Test
    fun `compareJobs throws when jobA is not found`() {
        whenever(jobPort.findById(99L)).thenReturn(Optional.empty())
        whenever(jobPort.findById(2L)).thenReturn(Optional.of(makeJob(2L)))

        assertThrows<IllegalStateException> {
            service.compareJobs(1L, 99L, 2L)
        }
    }

    @Test
    fun `compareJobs handles both jobs with no table stats`() {
        whenever(jobPort.findById(1L)).thenReturn(Optional.of(makeJob(1L)))
        whenever(jobPort.findById(2L)).thenReturn(Optional.of(makeJob(2L)))
        whenever(jobTableStatsPort.findByJobId(1L)).thenReturn(emptyList())
        whenever(jobTableStatsPort.findByJobId(2L)).thenReturn(emptyList())

        val result = service.compareJobs(1L, 1L, 2L)

        assertEquals(0L, result.rowsDelta)
        assertTrue(result.tablesAddedInB.isEmpty())
        assertTrue(result.tablesRemovedInB.isEmpty())
        assertTrue(result.tablesInCommon.isEmpty())
    }
}
