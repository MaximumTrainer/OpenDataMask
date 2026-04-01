package com.opendatamask.service

import com.opendatamask.config.EncryptionService
import com.opendatamask.connector.ConnectorFactory
import com.opendatamask.connector.DatabaseConnector
import com.opendatamask.model.*
import com.opendatamask.repository.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class JobServiceTest {

    @Mock private lateinit var jobRepository: JobRepository
    @Mock private lateinit var jobLogRepository: JobLogRepository
    @Mock private lateinit var workspaceRepository: WorkspaceRepository
    @Mock private lateinit var dataConnectionRepository: DataConnectionRepository
    @Mock private lateinit var tableConfigurationRepository: TableConfigurationRepository
    @Mock private lateinit var columnGeneratorRepository: ColumnGeneratorRepository
    @Mock private lateinit var encryptionService: EncryptionService
    @Mock private lateinit var connectorFactory: ConnectorFactory
    @Mock private lateinit var generatorService: GeneratorService
    @Mock private lateinit var destinationSchemaService: DestinationSchemaService
    @Mock private lateinit var postJobActionService: PostJobActionService
    @Mock private lateinit var schemaChangeService: SchemaChangeService
    @Mock private lateinit var webhookService: WebhookService

    @InjectMocks
    private lateinit var jobService: JobService

    private fun makeWorkspace(id: Long = 1L) = Workspace(
        id = id, name = "WS", ownerId = 1L,
        createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()
    )

    private fun makeJob(
        id: Long = 1L,
        workspaceId: Long = 1L,
        status: JobStatus = JobStatus.PENDING
    ) = Job(id = id, workspaceId = workspaceId, status = status, createdBy = 1L)

    private fun makeDataConnection(
        id: Long = 1L,
        workspaceId: Long = 1L,
        isSource: Boolean = false,
        isDestination: Boolean = false
    ) = DataConnection(
        id = id, workspaceId = workspaceId, name = "conn",
        type = ConnectionType.POSTGRESQL, connectionString = "enc_conn",
        isSource = isSource, isDestination = isDestination
    )

    private fun makeTableConfig(
        id: Long = 1L,
        workspaceId: Long = 1L,
        mode: TableMode = TableMode.PASSTHROUGH,
        rowLimit: Long? = null,
        whereClause: String? = null
    ) = TableConfiguration(
        id = id, workspaceId = workspaceId, tableName = "users",
        mode = mode, rowLimit = rowLimit, whereClause = whereClause
    )

    private fun stubJobSaveAndLog(job: Job) {
        whenever(jobRepository.findById(job.id)).thenReturn(Optional.of(job))
        whenever(jobRepository.save(any<Job>())).thenReturn(job)
        whenever(jobLogRepository.save(any<JobLog>())).thenAnswer { it.arguments[0] as JobLog }
    }

    // ── createJob ──────────────────────────────────────────────────────────

    @Test
    fun `createJob saves job with PENDING status`() {
        val savedJob = makeJob(id = 1L)
        whenever(workspaceRepository.findById(1L)).thenReturn(Optional.of(makeWorkspace()))
        whenever(jobRepository.save(any<Job>())).thenReturn(savedJob)

        val response = jobService.createJob(1L, 1L)

        assertEquals(1L, response.id)
        assertEquals(JobStatus.PENDING, response.status)
        verify(jobRepository).save(argThat { status == JobStatus.PENDING && workspaceId == 1L })
    }

    @Test
    fun `createJob throws when workspace not found`() {
        whenever(workspaceRepository.findById(999L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { jobService.createJob(999L, 1L) }
    }

    // ── getJob ─────────────────────────────────────────────────────────────

    @Test
    fun `getJob returns job for matching workspace`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        whenever(jobRepository.findById(1L)).thenReturn(Optional.of(job))

        val response = jobService.getJob(1L, 1L)

        assertEquals(1L, response.id)
    }

    @Test
    fun `getJob throws when job belongs to different workspace`() {
        val job = makeJob(id = 1L, workspaceId = 2L)
        whenever(jobRepository.findById(1L)).thenReturn(Optional.of(job))

        assertThrows<NoSuchElementException> { jobService.getJob(1L, 1L) }
    }

    @Test
    fun `getJob throws when job not found`() {
        whenever(jobRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<NoSuchElementException> { jobService.getJob(1L, 99L) }
    }

    // ── listJobs ───────────────────────────────────────────────────────────

    @Test
    fun `listJobs returns jobs for workspace`() {
        whenever(jobRepository.findByWorkspaceIdOrderByCreatedAtDesc(1L))
            .thenReturn(listOf(makeJob(id = 1L), makeJob(id = 2L)))

        val result = jobService.listJobs(1L)

        assertEquals(2, result.size)
    }

    // ── getJobLogs ─────────────────────────────────────────────────────────

    @Test
    fun `getJobLogs returns logs for job`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        val log = JobLog(id = 1L, jobId = 1L, message = "started", level = LogLevel.INFO)
        whenever(jobRepository.findById(1L)).thenReturn(Optional.of(job))
        whenever(jobLogRepository.findByJobIdOrderByTimestamp(1L)).thenReturn(listOf(log))

        val result = jobService.getJobLogs(1L, 1L)

        assertEquals(1, result.size)
        assertEquals("started", result[0].message)
    }

    // ── cancelJob ──────────────────────────────────────────────────────────

    @Test
    fun `cancelJob sets status to CANCELLED`() {
        val job = makeJob(id = 1L, status = JobStatus.PENDING)
        whenever(jobRepository.findById(1L)).thenReturn(Optional.of(job))
        whenever(jobRepository.save(any<Job>())).thenReturn(job)

        val response = jobService.cancelJob(1L, 1L)

        assertEquals(JobStatus.CANCELLED, response.status)
    }

    @Test
    fun `cancelJob also works for RUNNING status`() {
        val job = makeJob(id = 1L, status = JobStatus.RUNNING)
        whenever(jobRepository.findById(1L)).thenReturn(Optional.of(job))
        whenever(jobRepository.save(any<Job>())).thenReturn(job)

        val response = jobService.cancelJob(1L, 1L)

        assertEquals(JobStatus.CANCELLED, response.status)
    }

    @Test
    fun `cancelJob throws when job is already COMPLETED`() {
        val job = makeJob(id = 1L, status = JobStatus.COMPLETED)
        whenever(jobRepository.findById(1L)).thenReturn(Optional.of(job))

        assertThrows<IllegalStateException> { jobService.cancelJob(1L, 1L) }
    }

    @Test
    fun `cancelJob throws when job is already FAILED`() {
        val job = makeJob(id = 1L, status = JobStatus.FAILED)
        whenever(jobRepository.findById(1L)).thenReturn(Optional.of(job))

        assertThrows<IllegalStateException> { jobService.cancelJob(1L, 1L) }
    }

    // ── runJob failure paths ───────────────────────────────────────────────

    @Test
    fun `runJob sets status to FAILED when no source connection configured`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        stubJobSaveAndLog(job)
        whenever(dataConnectionRepository.findByWorkspaceId(1L)).thenReturn(emptyList())

        jobService.runJob(1L)

        verify(jobRepository, atLeastOnce()).save(argThat { status == JobStatus.FAILED })
        verify(postJobActionService).triggerActions(any())
    }

    @Test
    fun `runJob sets status to FAILED when no destination connection configured`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        val sourceConn = makeDataConnection(id = 1L, isSource = true)
        stubJobSaveAndLog(job)
        whenever(dataConnectionRepository.findByWorkspaceId(1L)).thenReturn(listOf(sourceConn))

        jobService.runJob(1L)

        verify(jobRepository, atLeastOnce()).save(argThat { status == JobStatus.FAILED })
    }

    @Test
    fun `runJob returns early when job not found`() {
        whenever(jobRepository.findById(99L)).thenReturn(Optional.empty())

        jobService.runJob(99L)

        verify(jobRepository, never()).save(any())
    }

    // ── runJob success — PASSTHROUGH ───────────────────────────────────────

    @Test
    fun `runJob completes successfully with PASSTHROUGH table`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        val sourceConn = makeDataConnection(id = 1L, workspaceId = 1L, isSource = true)
        val destConn = makeDataConnection(id = 2L, workspaceId = 1L, isDestination = true)
        val tableConfig = makeTableConfig(mode = TableMode.PASSTHROUGH)
        val mockSrc = mock<DatabaseConnector>()
        val mockDst = mock<DatabaseConnector>()
        val rows = listOf(mapOf("id" to 1, "name" to "Alice"))

        stubJobSaveAndLog(job)
        whenever(dataConnectionRepository.findByWorkspaceId(1L)).thenReturn(listOf(sourceConn, destConn))
        whenever(encryptionService.decrypt(any())).thenReturn("decrypted")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockSrc, mockDst)
        whenever(mockSrc.testConnection()).thenReturn(true)
        whenever(mockDst.testConnection()).thenReturn(true)
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tableConfig))
        whenever(mockSrc.fetchData(eq("users"), anyOrNull(), anyOrNull())).thenReturn(rows)
        whenever(mockDst.writeData(eq("users"), eq(rows))).thenReturn(1)

        jobService.runJob(1L)

        verify(mockDst).writeData("users", rows)
        verify(jobRepository, atLeastOnce()).save(argThat { status == JobStatus.COMPLETED })
        verify(postJobActionService).triggerActions(any())
    }

    // ── runJob success — MASK ──────────────────────────────────────────────

    @Test
    fun `runJob processes MASK mode and applies generators`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        val sourceConn = makeDataConnection(id = 1L, workspaceId = 1L, isSource = true)
        val destConn = makeDataConnection(id = 2L, workspaceId = 1L, isDestination = true)
        val tableConfig = makeTableConfig(mode = TableMode.MASK)
        val generator = ColumnGenerator(
            id = 1L, tableConfigurationId = 1L,
            columnName = "name", generatorType = GeneratorType.NAME
        )
        val mockSrc = mock<DatabaseConnector>()
        val mockDst = mock<DatabaseConnector>()
        val original = mapOf<String, Any?>("id" to 1, "name" to "Alice")
        val masked = mapOf<String, Any?>("id" to 1, "name" to "Bob")

        stubJobSaveAndLog(job)
        whenever(dataConnectionRepository.findByWorkspaceId(1L)).thenReturn(listOf(sourceConn, destConn))
        whenever(encryptionService.decrypt(any())).thenReturn("decrypted")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockSrc, mockDst)
        whenever(mockSrc.testConnection()).thenReturn(true)
        whenever(mockDst.testConnection()).thenReturn(true)
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tableConfig))
        whenever(columnGeneratorRepository.findByTableConfigurationId(1L)).thenReturn(listOf(generator))
        whenever(mockSrc.fetchData(eq("users"), anyOrNull(), anyOrNull())).thenReturn(listOf(original))
        whenever(generatorService.applyGenerators(original, listOf(generator))).thenReturn(masked)
        whenever(mockDst.writeData("users", listOf(masked))).thenReturn(1)

        jobService.runJob(1L)

        verify(generatorService).applyGenerators(original, listOf(generator))
        verify(mockDst).writeData("users", listOf(masked))
    }

    // ── runJob success — GENERATE ──────────────────────────────────────────

    @Test
    fun `runJob processes GENERATE mode with explicit rowLimit`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        val sourceConn = makeDataConnection(id = 1L, workspaceId = 1L, isSource = true)
        val destConn = makeDataConnection(id = 2L, workspaceId = 1L, isDestination = true)
        val tableConfig = makeTableConfig(mode = TableMode.GENERATE, rowLimit = 5L)
        val generator = ColumnGenerator(
            id = 1L, tableConfigurationId = 1L,
            columnName = "name", generatorType = GeneratorType.NAME
        )
        val generated = listOf(mapOf<String, Any?>("name" to "Generated"))
        val mockSrc = mock<DatabaseConnector>()
        val mockDst = mock<DatabaseConnector>()

        stubJobSaveAndLog(job)
        whenever(dataConnectionRepository.findByWorkspaceId(1L)).thenReturn(listOf(sourceConn, destConn))
        whenever(encryptionService.decrypt(any())).thenReturn("decrypted")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockSrc, mockDst)
        whenever(mockSrc.testConnection()).thenReturn(true)
        whenever(mockDst.testConnection()).thenReturn(true)
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tableConfig))
        whenever(columnGeneratorRepository.findByTableConfigurationId(1L)).thenReturn(listOf(generator))
        whenever(generatorService.generateRows(listOf(generator), 5)).thenReturn(generated)
        whenever(mockDst.writeData("users", generated)).thenReturn(1)

        jobService.runJob(1L)

        verify(generatorService).generateRows(listOf(generator), 5)
        verify(mockDst).writeData("users", generated)
    }

    @Test
    fun `runJob processes GENERATE mode defaults to 100 rows when no rowLimit`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        val sourceConn = makeDataConnection(id = 1L, workspaceId = 1L, isSource = true)
        val destConn = makeDataConnection(id = 2L, workspaceId = 1L, isDestination = true)
        val tableConfig = makeTableConfig(mode = TableMode.GENERATE, rowLimit = null)
        val mockSrc = mock<DatabaseConnector>()
        val mockDst = mock<DatabaseConnector>()

        stubJobSaveAndLog(job)
        whenever(dataConnectionRepository.findByWorkspaceId(1L)).thenReturn(listOf(sourceConn, destConn))
        whenever(encryptionService.decrypt(any())).thenReturn("decrypted")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockSrc, mockDst)
        whenever(mockSrc.testConnection()).thenReturn(true)
        whenever(mockDst.testConnection()).thenReturn(true)
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tableConfig))
        whenever(columnGeneratorRepository.findByTableConfigurationId(1L)).thenReturn(emptyList())
        whenever(generatorService.generateRows(emptyList(), 100)).thenReturn(emptyList())
        whenever(mockDst.writeData(any(), any())).thenReturn(0)

        jobService.runJob(1L)

        verify(generatorService).generateRows(emptyList(), 100)
    }

    // ── runJob success — SUBSET ────────────────────────────────────────────

    @Test
    fun `runJob processes SUBSET mode with whereClause`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        val sourceConn = makeDataConnection(id = 1L, workspaceId = 1L, isSource = true)
        val destConn = makeDataConnection(id = 2L, workspaceId = 1L, isDestination = true)
        val tableConfig = makeTableConfig(mode = TableMode.SUBSET, whereClause = "age > 18")
        val mockSrc = mock<DatabaseConnector>()
        val mockDst = mock<DatabaseConnector>()
        val filtered = listOf(mapOf<String, Any?>("id" to 1, "age" to 25))

        stubJobSaveAndLog(job)
        whenever(dataConnectionRepository.findByWorkspaceId(1L)).thenReturn(listOf(sourceConn, destConn))
        whenever(encryptionService.decrypt(any())).thenReturn("decrypted")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockSrc, mockDst)
        whenever(mockSrc.testConnection()).thenReturn(true)
        whenever(mockDst.testConnection()).thenReturn(true)
        whenever(tableConfigurationRepository.findByWorkspaceId(1L)).thenReturn(listOf(tableConfig))
        whenever(mockSrc.fetchData(eq("users"), anyOrNull(), eq("age > 18"))).thenReturn(filtered)
        whenever(mockDst.writeData("users", filtered)).thenReturn(1)

        jobService.runJob(1L)

        verify(mockSrc).fetchData("users", null, "age > 18")
        verify(mockDst).writeData("users", filtered)
    }

    @Test
    fun `runJob sets FAILED and triggers actions when source cannot connect`() {
        val job = makeJob(id = 1L, workspaceId = 1L)
        val sourceConn = makeDataConnection(id = 1L, workspaceId = 1L, isSource = true)
        val destConn = makeDataConnection(id = 2L, workspaceId = 1L, isDestination = true)
        val mockSrc = mock<DatabaseConnector>()
        val mockDst = mock<DatabaseConnector>()

        stubJobSaveAndLog(job)
        whenever(dataConnectionRepository.findByWorkspaceId(1L)).thenReturn(listOf(sourceConn, destConn))
        whenever(encryptionService.decrypt(any())).thenReturn("decrypted")
        whenever(connectorFactory.createConnector(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(mockSrc, mockDst)
        whenever(mockSrc.testConnection()).thenReturn(false)

        jobService.runJob(1L)

        verify(jobRepository, atLeastOnce()).save(argThat { status == JobStatus.FAILED })
        verify(postJobActionService).triggerActions(any())
    }
}

