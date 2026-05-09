package com.opendatamask.application.service

import com.opendatamask.domain.port.input.dto.JobCompareResponse
import com.opendatamask.domain.port.output.JobPort
import com.opendatamask.domain.port.output.JobTableStatsPort
import org.springframework.stereotype.Service

@Service
class JobCompareService(
    private val jobPort: JobPort,
    private val jobTableStatsPort: JobTableStatsPort
) {

    fun compareJobs(workspaceId: Long, jobAId: Long, jobBId: Long): JobCompareResponse {
        jobPort.findById(jobAId).orElse(null)
            ?: error("Job $jobAId not found")
        jobPort.findById(jobBId).orElse(null)
            ?: error("Job $jobBId not found")

        val tablesA = jobTableStatsPort.findByJobId(jobAId).map { it.tableName }.toSet()
        val tablesB = jobTableStatsPort.findByJobId(jobBId).map { it.tableName }.toSet()

        val statsA = jobTableStatsPort.findByJobId(jobAId)
        val statsB = jobTableStatsPort.findByJobId(jobBId)

        val rowsA = statsA.sumOf { it.rowsRead }
        val rowsB = statsB.sumOf { it.rowsRead }

        return JobCompareResponse(
            jobAId = jobAId,
            jobBId = jobBId,
            rowsDelta = rowsB - rowsA,
            tablesAddedInB = (tablesB - tablesA).sorted(),
            tablesRemovedInB = (tablesA - tablesB).sorted(),
            tablesInCommon = tablesA.intersect(tablesB).sorted()
        )
    }
}
