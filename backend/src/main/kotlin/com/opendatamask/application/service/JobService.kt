package com.opendatamask.application.service

import com.opendatamask.domain.port.input.JobUseCase

import com.opendatamask.domain.port.output.EncryptionPort
import com.opendatamask.domain.port.output.ConnectorFactoryPort
import com.opendatamask.domain.port.output.JobPort
import com.opendatamask.domain.port.output.JobLogPort
import com.opendatamask.domain.port.output.WorkspacePort
import com.opendatamask.domain.port.output.DataConnectionPort
import com.opendatamask.domain.port.output.TableConfigurationPort
import com.opendatamask.domain.port.output.ColumnGeneratorPort
import com.opendatamask.domain.port.output.SubsetTableConfigPort
import com.opendatamask.domain.port.output.DatabaseConnector
import com.opendatamask.domain.port.input.dto.JobLogResponse
import com.opendatamask.domain.port.input.dto.JobResponse
import com.opendatamask.domain.model.*
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class JobService(
    private val jobRepository: JobPort,
    private val jobLogRepository: JobLogPort,
    private val workspaceRepository: WorkspacePort,
    private val dataConnectionRepository: DataConnectionPort,
    private val tableConfigurationRepository: TableConfigurationPort,
    private val columnGeneratorRepository: ColumnGeneratorPort,
    private val encryptionPort: EncryptionPort,
    private val connectorFactory: ConnectorFactoryPort,
    private val generatorService: GeneratorService,
    private val destinationSchemaService: DestinationSchemaService,
    private val postJobActionService: PostJobActionService,
    private val schemaChangeService: SchemaChangeService,
    private val webhookService: WebhookService
) : JobUseCase {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private var subsetExecutionService: SubsetExecutionService? = null

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private var subsetTableConfigPort: SubsetTableConfigPort? = null

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private var privacyReportService: PrivacyReportService? = null

    private val logger = LoggerFactory.getLogger(JobService::class.java)

    @Transactional
    override fun createJob(workspaceId: Long, createdBy: Long): JobResponse {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }

        val job = Job(
            workspaceId = workspaceId,
            status = JobStatus.PENDING,
            createdBy = createdBy
        )
        val saved = jobRepository.save(job)
        return saved.toResponse()
    }

    @Transactional
    fun createJob(workspaceId: Long): Job {
        workspaceRepository.findById(workspaceId)
            .orElseThrow { NoSuchElementException("Workspace not found: $workspaceId") }
        val job = Job(workspaceId = workspaceId, status = JobStatus.PENDING, createdBy = 0L)
        return jobRepository.save(job)
    }

    @Transactional(readOnly = true)
    override fun getJob(workspaceId: Long, jobId: Long): JobResponse {
        return findJob(workspaceId, jobId).toResponse()
    }

    @Transactional(readOnly = true)
    override fun listJobs(workspaceId: Long): List<JobResponse> {
        return jobRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    override fun getJobLogs(workspaceId: Long, jobId: Long): List<JobLogResponse> {
        findJob(workspaceId, jobId)
        return jobLogRepository.findByJobIdOrderByTimestamp(jobId).map { it.toLogResponse() }
    }

    @Transactional
    override fun cancelJob(workspaceId: Long, jobId: Long): JobResponse {
        val job = findJob(workspaceId, jobId)
        if (job.status !in listOf(JobStatus.PENDING, JobStatus.RUNNING)) {
            throw IllegalStateException("Cannot cancel job in status ${job.status}")
        }
        job.status = JobStatus.CANCELLED
        job.completedAt = LocalDateTime.now()
        return jobRepository.save(job).toResponse()
    }

    @Transactional
    override fun createAndRunJob(workspaceId: Long, createdBy: Long): JobResponse {
        val response = createJob(workspaceId, createdBy)
        runJob(response.id)
        return response
    }

    @Async
    fun runJob(jobId: Long) {
        val job = jobRepository.findById(jobId).orElse(null) ?: return

        try {
            updateJobStatus(job, JobStatus.RUNNING)
            addLog(job.id, "Job started", LogLevel.INFO)

            if (schemaChangeService.isBlockingJobRun(job.workspaceId)) {
                throw IllegalStateException("Job blocked: unresolved schema changes require attention before running")
            }

            val sourceConnections = dataConnectionRepository.findByWorkspaceId(job.workspaceId)
                .filter { it.isSource }
            val destConnections = dataConnectionRepository.findByWorkspaceId(job.workspaceId)
                .filter { it.isDestination }

            if (sourceConnections.isEmpty()) {
                throw IllegalStateException("No source connection configured for workspace ${job.workspaceId}")
            }
            if (destConnections.isEmpty()) {
                throw IllegalStateException("No destination connection configured for workspace ${job.workspaceId}")
            }

            val sourceConn = sourceConnections.first()
            val destConn = destConnections.first()

            addLog(job.id, "Connecting to source: ${sourceConn.name}", LogLevel.INFO)
            val sourceConnector = connectorFactory.createConnector(
                type = sourceConn.type,
                connectionString = encryptionPort.decrypt(sourceConn.connectionString),
                username = sourceConn.username,
                password = sourceConn.password?.let { encryptionPort.decrypt(it) },
                database = sourceConn.database
            )

            addLog(job.id, "Connecting to destination: ${destConn.name}", LogLevel.INFO)
            val destConnector = connectorFactory.createConnector(
                type = destConn.type,
                connectionString = encryptionPort.decrypt(destConn.connectionString),
                username = destConn.username,
                password = destConn.password?.let { encryptionPort.decrypt(it) },
                database = destConn.database
            )

            if (!sourceConnector.testConnection()) {
                throw IllegalStateException("Cannot connect to source database")
            }
            if (!destConnector.testConnection()) {
                throw IllegalStateException("Cannot connect to destination database")
            }

            destinationSchemaService.validateCompatibility(sourceConn.type, destConn.type)

            val tableConfigs = tableConfigurationRepository.findByWorkspaceId(job.workspaceId)
            addLog(job.id, "Processing ${tableConfigs.size} table(s)", LogLevel.INFO)

            val localSubsetRepo = subsetTableConfigPort
            val localSubsetExec = subsetExecutionService
            val subsetRows: Map<String, List<Map<String, Any?>>> =
                if (localSubsetRepo != null &&
                    localSubsetExec != null &&
                    localSubsetRepo.findByWorkspaceId(job.workspaceId).isNotEmpty()
                ) {
                    addLog(job.id, "Running referential integrity subset execution", LogLevel.INFO)
                    localSubsetExec.executeSubset(job.workspaceId, sourceConnector)
                } else {
                    emptyMap()
                }

            for (tableConfig in tableConfigs) {
                if (tableConfig.mode == TableMode.SKIP) {
                    addLog(job.id, "Skipping table: ${tableConfig.tableName}", LogLevel.INFO)
                    continue
                }
                addLog(job.id, "Mirroring schema for table: ${tableConfig.tableName}", LogLevel.INFO)
                val selectedAttrs = tableConfig.selectedAttributes
                    ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                destinationSchemaService.mirrorSchema(
                    sourceConnector, sourceConn.type,
                    destConnector, destConn.type,
                    tableConfig.tableName,
                    selectedAttrs
                )
                processTable(job.id, tableConfig, sourceConnector, destConnector, subsetRows)
            }

            updateJobStatus(job, JobStatus.COMPLETED)
            addLog(job.id, "Job completed successfully", LogLevel.INFO)
            val completedJob = jobRepository.findById(job.id).orElse(job)
            privacyReportService?.generateJobReport(job.id, job.workspaceId)
            postJobActionService.triggerActions(completedJob)
            webhookService.triggerForJob(completedJob, JobStatus.COMPLETED.name)

        } catch (e: Exception) {
            logger.error("Job ${job.id} failed", e)
            addLog(job.id, "Job failed: ${e.message}", LogLevel.ERROR)
            val failedJob = jobRepository.findById(job.id).orElse(job)
            failedJob.status = JobStatus.FAILED
            failedJob.completedAt = LocalDateTime.now()
            failedJob.errorMessage = e.message?.take(4096)
            jobRepository.save(failedJob)
            postJobActionService.triggerActions(failedJob)
            webhookService.triggerForJob(failedJob, JobStatus.FAILED.name)
        }
    }

    private fun processTable(
        jobId: Long,
        tableConfig: TableConfiguration,
        sourceConnector: DatabaseConnector,
        destConnector: DatabaseConnector,
        preComputedRows: Map<String, List<Map<String, Any?>>> = emptyMap()
    ) {
        addLog(jobId, "Processing table: ${tableConfig.tableName} (mode: ${tableConfig.mode})", LogLevel.INFO)

        val selectedAttrs = tableConfig.selectedAttributes
            ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }

        when (tableConfig.mode) {
            TableMode.PASSTHROUGH -> {
                val data = sourceConnector.fetchData(tableConfig.tableName, tableConfig.rowLimit?.toInt(), null, selectedAttrs)
                addLog(jobId, "Fetched ${data.size} rows from ${tableConfig.tableName}", LogLevel.INFO)
                val written = destConnector.writeData(tableConfig.tableName, data)
                addLog(jobId, "Wrote $written rows to destination ${tableConfig.tableName}", LogLevel.INFO)
            }
            TableMode.MASK -> {
                val generators = columnGeneratorRepository.findByTableConfigurationId(tableConfig.id)
                val data = sourceConnector.fetchData(tableConfig.tableName, tableConfig.rowLimit?.toInt(), null, selectedAttrs)
                addLog(
                    jobId,
                    "Masking ${data.size} rows in ${tableConfig.tableName} with ${generators.size} generator(s)",
                    LogLevel.INFO
                )
                val maskedData = data.map { row -> generatorService.applyGenerators(row, generators) }
                val written = destConnector.writeData(tableConfig.tableName, maskedData)
                addLog(jobId, "Wrote $written masked rows to destination ${tableConfig.tableName}", LogLevel.INFO)
            }
            TableMode.GENERATE -> {
                val generators = columnGeneratorRepository.findByTableConfigurationId(tableConfig.id)
                val rowCount = tableConfig.rowLimit?.toInt() ?: 100
                addLog(jobId, "Generating $rowCount rows for ${tableConfig.tableName} with ${generators.size} generator(s)", LogLevel.INFO)
                val generatedData = generatorService.generateRows(generators, rowCount)
                val written = destConnector.writeData(tableConfig.tableName, generatedData)
                addLog(jobId, "Wrote $written generated rows to destination ${tableConfig.tableName}", LogLevel.INFO)
            }
            TableMode.SUBSET -> {
                val data = preComputedRows[tableConfig.tableName]
                    ?: sourceConnector.fetchData(
                        tableConfig.tableName,
                        tableConfig.rowLimit?.toInt(),
                        tableConfig.whereClause,
                        selectedAttrs
                    )
                addLog(jobId, "Subsetting ${data.size} rows from ${tableConfig.tableName}", LogLevel.INFO)
                val written = destConnector.writeData(tableConfig.tableName, data)
                addLog(jobId, "Wrote $written rows to destination ${tableConfig.tableName}", LogLevel.INFO)
            }
            TableMode.UPSERT -> {
                val generators = columnGeneratorRepository.findByTableConfigurationId(tableConfig.id)
                val data = sourceConnector.fetchData(tableConfig.tableName, tableConfig.rowLimit?.toInt(), tableConfig.whereClause, selectedAttrs)
                addLog(jobId, "Upserting ${data.size} rows in ${tableConfig.tableName} with ${generators.size} generator(s)", LogLevel.INFO)
                val maskedData = if (generators.isNotEmpty()) data.map { row -> generatorService.applyGenerators(row, generators) } else data
                val written = destConnector.writeData(tableConfig.tableName, maskedData)
                addLog(jobId, "Wrote $written upserted rows to destination ${tableConfig.tableName}", LogLevel.INFO)
            }
            TableMode.SKIP -> {
                // unreachable: SKIP tables are short-circuited in the calling loop
            }
        }
    }

    private fun updateJobStatus(job: Job, status: JobStatus) {
        val freshJob = jobRepository.findById(job.id).orElse(job)
        freshJob.status = status
        if (status == JobStatus.RUNNING) freshJob.startedAt = LocalDateTime.now()
        if (status in listOf(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELLED)) {
            freshJob.completedAt = LocalDateTime.now()
        }
        jobRepository.save(freshJob)
    }

    private fun addLog(jobId: Long, message: String, level: LogLevel) {
        jobLogRepository.save(JobLog(jobId = jobId, message = message, level = level))
    }

    private fun findJob(workspaceId: Long, jobId: Long): Job {
        val job = jobRepository.findById(jobId)
            .orElseThrow { NoSuchElementException("Job not found: $jobId") }
        if (job.workspaceId != workspaceId) {
            throw NoSuchElementException("Job $jobId does not belong to workspace $workspaceId")
        }
        return job
    }

    private fun Job.toResponse() = JobResponse(
        id = id,
        workspaceId = workspaceId,
        status = status,
        startedAt = startedAt,
        completedAt = completedAt,
        createdAt = createdAt,
        errorMessage = errorMessage,
        createdBy = createdBy
    )

    private fun JobLog.toLogResponse() = JobLogResponse(
        id = id,
        jobId = jobId,
        message = message,
        level = level,
        timestamp = timestamp
    )
}



