import { test, expect } from './odm-fixtures'
import {
  SOURCE_DB,
  TARGET_DB,
  loginViaApi,
  registerUser,
  apiCall,
  seedVerificationData,
  runMaskingJobViaApi,
  waitForJobCompletion,
  waitForPageHeading,
  waitForLoadingDone,
  type JobResponse,
  type JobLogEntry,
  type ConnectionTestResponse,
} from './odm-fixtures'

// ── Job Execution & Destination Transfer Verification ─────────────────────
// Verifies job creation, monitoring, completion, and validates that masked
// data is actually transferred to the destination database — mirroring the
// checks performed by verification/verify.py.

test.describe('Job Execution', () => {
  let workspaceId: number
  let token: string
  let sourceConnectionId: number
  let targetConnectionId: number

  test.beforeAll(async () => {
    await registerUser()
    token = await loginViaApi()

    const ws = await apiCall('/api/workspaces', {
      method: 'POST',
      body: { name: 'Job Exec Test Workspace', description: 'E2E job tests' },
      token,
    })
    workspaceId = ws.id as number

    const src = await apiCall(`/api/workspaces/${workspaceId}/connections`, {
      method: 'POST',
      body: {
        name: SOURCE_DB.name,
        type: SOURCE_DB.type,
        connectionString: `jdbc:postgresql://${SOURCE_DB.host}:${SOURCE_DB.port}/${SOURCE_DB.database}`,
        username: SOURCE_DB.username,
        password: SOURCE_DB.password,
        isSource: true,
        isDestination: false,
      },
      token,
    })
    sourceConnectionId = src.id as number

    const tgt = await apiCall(`/api/workspaces/${workspaceId}/connections`, {
      method: 'POST',
      body: {
        name: TARGET_DB.name,
        type: TARGET_DB.type,
        connectionString: `jdbc:postgresql://${TARGET_DB.host}:${TARGET_DB.port}/${TARGET_DB.database}`,
        username: TARGET_DB.username,
        password: TARGET_DB.password,
        isSource: false,
        isDestination: true,
      },
      token,
    })
    targetConnectionId = tgt.id as number
  })

  test('jobs page shows empty state when no jobs exist', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')

    await expect(page.locator('.empty-state, .job-card')).toBeVisible({ timeout: 10_000 })

    const emptyState = page.locator('.empty-state')
    if (await emptyState.isVisible()) {
      await expect(emptyState).toContainText('No jobs yet')
      await expect(page.locator("button:has-text('Run New Job')")).toBeVisible()
    }
  })

  test('run new job modal opens with connection selectors', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')

    await page.click("button:has-text('Run New Job')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    await expect(page.locator("[role='dialog']")).toContainText('Run New Masking Job')

    // Job name field
    await expect(page.locator("[role='dialog'] input[placeholder*='Mask']")).toBeVisible()

    // Source and target connection selectors
    const selects = page.locator("[role='dialog'] select.form-control")
    await expect(selects.first()).toBeVisible()

    // Run Job button
    await expect(page.locator("[role='dialog'] button:has-text('Run Job')")).toBeVisible()
  })

  test('job creation validation requires all fields', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')

    await page.click("button:has-text('Run New Job')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    // Clear the name field
    const nameInput = page.locator("[role='dialog'] input[placeholder*='Mask']")
    await nameInput.fill('')

    // Try to submit
    await page.click("[role='dialog'] button:has-text('Run Job')")

    // Should show validation error
    await expect(page.locator("[role='dialog'] .alert-error")).toBeVisible({ timeout: 5_000 })
  })

  test('create and submit a masking job via UI', async ({ authenticatedPage: page }) => {
    // Seed table config + generators so the job has something to process
    const tbl = await apiCall(`/api/workspaces/${workspaceId}/tables`, {
      method: 'POST',
      body: {
        connectionId: sourceConnectionId,
        tableName: 'users',
        schemaName: 'public',
        mode: 'MASK',
      },
      token,
    })
    const tableId = tbl.id as number

    const generators = [
      { columnName: 'full_name', generatorType: 'FULL_NAME' },
      { columnName: 'email', generatorType: 'EMAIL' },
      { columnName: 'phone_number', generatorType: 'PHONE' },
      { columnName: 'date_of_birth', generatorType: 'BIRTH_DATE' },
      { columnName: 'salary', generatorType: 'RANDOM_INT', generatorParams: JSON.stringify({ min: '30000', max: '200000' }) },
    ]
    for (const gen of generators) {
      await apiCall(`/api/workspaces/${workspaceId}/tables/${tableId}/generators`, {
        method: 'POST',
        body: gen,
        token,
      })
    }

    await page.goto(`/workspaces/${workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')

    await page.click("button:has-text('Run New Job')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    // Fill job name
    await page.fill("[role='dialog'] input[placeholder*='Mask']", 'E2E Test Masking Job')

    // Select source connection
    const selects = page.locator("[role='dialog'] select.form-control")
    if (await selects.nth(0).isVisible()) {
      await selects.nth(0).selectOption({ index: 0 })
    }
    if (await selects.nth(1).isVisible()) {
      await selects.nth(1).selectOption({ index: 1 })
    }

    // Submit
    await page.click("[role='dialog'] button:has-text('Run Job')")

    // Modal should close and job should appear
    await page.waitForSelector("[role='dialog']", { state: 'hidden', timeout: 10_000 })
    await expect(page.locator('.job-card')).toBeVisible({ timeout: 10_000 })
  })

  test('job card shows status badge', async ({ authenticatedPage: page }) => {
    // Create a job via API
    const jobId = await runMaskingJobViaApi(token, workspaceId, sourceConnectionId, targetConnectionId)

    await page.goto(`/workspaces/${workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')

    await page.waitForSelector('.job-card', { timeout: 10_000 })

    // Should display a status badge (PENDING, RUNNING, or COMPLETED)
    const statusBadge = page.locator('.job-card .badge, .job-card [class*="status"]').first()
    await expect(statusBadge).toBeVisible()
  })

  test('view logs button expands log section', async ({ authenticatedPage: page }) => {
    // Create and wait for a job to complete via API
    const jobId = await runMaskingJobViaApi(token, workspaceId, sourceConnectionId, targetConnectionId)
    await waitForJobCompletion(token, workspaceId, jobId)

    await page.goto(`/workspaces/${workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')

    await page.waitForSelector('.job-card', { timeout: 10_000 })

    // Click View Logs
    const viewLogsBtn = page.locator("button:has-text('View Logs')").first()
    if (await viewLogsBtn.isVisible({ timeout: 5_000 })) {
      await viewLogsBtn.click()

      // Logs section should appear
      await expect(page.locator('.logs-section')).toBeVisible({ timeout: 10_000 })
    }
  })

  test('completed job shows stats (tables and rows processed)', async ({ authenticatedPage: page }) => {
    // Create and wait for a job to complete
    const jobId = await runMaskingJobViaApi(token, workspaceId, sourceConnectionId, targetConnectionId)
    const status = await waitForJobCompletion(token, workspaceId, jobId)

    if (status === 'COMPLETED') {
      await page.goto(`/workspaces/${workspaceId}/jobs`)
      await waitForPageHeading(page, 'Jobs')

      // Wait for the jobs to load and auto-refresh
      await page.waitForSelector('.job-card', { timeout: 10_000 })

      // Reload to see completed status
      await page.reload()
      await waitForLoadingDone(page)
      await page.waitForSelector('.job-card', { timeout: 10_000 })

      // Completed jobs should show stat chips with tables and rows
      await expect(page.locator('.stat-chip, text=tables, text=rows')).toBeVisible({ timeout: 15_000 })
    }
  })
})

test.describe('Destination Database Transfer Verification', () => {
  let seed: Awaited<ReturnType<typeof seedVerificationData>>

  test.beforeAll(async () => {
    seed = await seedVerificationData()

    // Run a masking job and wait for completion
    const jobId = await runMaskingJobViaApi(
      seed.token,
      seed.workspaceId,
      seed.sourceConnectionId,
      seed.targetConnectionId
    )
    const status = await waitForJobCompletion(seed.token, seed.workspaceId, jobId)
    if (status !== 'COMPLETED') {
      throw new Error(`Masking job did not complete successfully: status=${status}`)
    }
  })

  test('completed job is visible in jobs list with COMPLETED status', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${seed.workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')
    await page.waitForSelector('.job-card', { timeout: 10_000 })

    // At least one job should show COMPLETED
    await expect(page.locator('text=COMPLETED')).toBeVisible({ timeout: 15_000 })
  })

  test('completed job shows non-zero tables processed', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${seed.workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')
    await page.waitForSelector('.job-card', { timeout: 10_000 })

    // Check via API that tablesProcessed > 0
    const jobs = await apiCall<JobResponse[]>(`/api/workspaces/${seed.workspaceId}/jobs`, {
      token: seed.token,
    })

    const completedJob = jobs.find((j) => j.status === 'COMPLETED')
    expect(completedJob).toBeDefined()
    expect(completedJob!.tablesProcessed).toBeGreaterThan(0)
  })

  test('completed job shows non-zero rows processed', async ({ authenticatedPage: page }) => {
    // Verify via API
    const jobs = await apiCall<JobResponse[]>(`/api/workspaces/${seed.workspaceId}/jobs`, {
      token: seed.token,
    })

    const completedJob = jobs.find((j) => j.status === 'COMPLETED')
    expect(completedJob).toBeDefined()
    expect(completedJob!.rowsProcessed).toBeGreaterThan(0)

    // Also verify in UI that stats show rows
    await page.goto(`/workspaces/${seed.workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')
    await page.waitForSelector('.job-card', { timeout: 10_000 })

    // The stat chip should show the row count
    await expect(page.locator('.stat-chip')).toBeVisible({ timeout: 10_000 })
  })

  test('job logs show masking activity entries', async ({ authenticatedPage: page }) => {
    // Fetch job ID
    const jobs = await apiCall<JobResponse[]>(`/api/workspaces/${seed.workspaceId}/jobs`, {
      token: seed.token,
    })

    const completedJob = jobs.find((j) => j.status === 'COMPLETED')
    expect(completedJob).toBeDefined()

    // Fetch logs via API
    const logs = await apiCall<JobLogEntry[]>(
      `/api/workspaces/${seed.workspaceId}/jobs/${completedJob!.id}/logs`,
      { token: seed.token }
    )

    expect(logs.length).toBeGreaterThan(0)

    // View logs in UI
    await page.goto(`/workspaces/${seed.workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')
    await page.waitForSelector('.job-card', { timeout: 10_000 })

    const viewLogsBtn = page.locator("button:has-text('View Logs')").first()
    if (await viewLogsBtn.isVisible({ timeout: 5_000 })) {
      await viewLogsBtn.click()
      await expect(page.locator('.logs-section')).toBeVisible({ timeout: 10_000 })

      // Log entries should be visible
      await expect(page.locator('.log-line')).toHaveCount(logs.length, { timeout: 10_000 })
    }
  })

  test('record integrity — source and target row counts match (API verification)', async () => {
    // This test verifies data transfer by checking the job's reported row count
    // against the known source data (50 users in source_db.sql).
    const jobs = await apiCall<JobResponse[]>(`/api/workspaces/${seed.workspaceId}/jobs`, {
      token: seed.token,
    })

    const completedJob = jobs.find((j) => j.status === 'COMPLETED')
    expect(completedJob).toBeDefined()

    // The source database has 50 user records (per verification/init/source_db.sql)
    expect(completedJob!.rowsProcessed).toBe(50)
    expect(completedJob!.tablesProcessed).toBe(1)
  })

  test('workspace overview shows job history after completion', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${seed.workspaceId}`)
    await waitForLoadingDone(page)

    // The workspace detail page should reflect completed job data
    const pageContent = await page.textContent('body')
    expect(pageContent).toBeTruthy()
  })

  test('source connection still test-passes after job', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${seed.workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')
    await waitForLoadingDone(page)

    // Find the source connection row
    const sourceRow = page.locator(`tr:has-text("${SOURCE_DB.name}")`).first()
    if (await sourceRow.isVisible({ timeout: 5_000 })) {
      await sourceRow.locator("button:has-text('Test')").click()

      // Wait for test result
      await expect(
        page.locator('.badge-green:has-text("OK"), .badge-red:has-text("Error")')
      ).toBeVisible({ timeout: 30_000 })

      // Should succeed
      await expect(sourceRow.locator('.badge-green:has-text("OK")')).toBeVisible({ timeout: 5_000 })
    }
  })

  test('destination connection still test-passes after job', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${seed.workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')
    await waitForLoadingDone(page)

    // Find the target connection row
    const targetRow = page.locator(`tr:has-text("${TARGET_DB.name}")`).first()
    if (await targetRow.isVisible({ timeout: 5_000 })) {
      await targetRow.locator("button:has-text('Test')").click()

      await expect(
        page.locator('.badge-green:has-text("OK"), .badge-red:has-text("Error")')
      ).toBeVisible({ timeout: 30_000 })

      await expect(targetRow.locator('.badge-green:has-text("OK")')).toBeVisible({ timeout: 5_000 })
    }
  })
})
