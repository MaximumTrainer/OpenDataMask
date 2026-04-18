import { test, expect } from './odm-fixtures'
import {
  ODM_USERNAME,
  ODM_PASSWORD,
  SOURCE_DB,
  TARGET_DB,
  registerUser,
  loginViaApi,
  apiCall,
  waitForPageHeading,
  waitForLoadingDone,
  waitForJobCompletion,
  type IdResponse,
  type WorkspaceResponse,
  type ConnectionResponse,
  type JobResponse,
  type JobLogEntry,
  type ConnectionTestResponse,
} from './odm-fixtures'

// ── Full E2E Workflow Test ────────────────────────────────────────────────
// This spec mirrors the exact sequence performed by
// verification/run_verification.sh and verification/verify.py:
//
//   1. Register user → Login
//   2. Create workspace
//   3. Create source connection (PostgreSQL → source_db)
//   4. Create destination connection (PostgreSQL → target_db)
//   5. Configure table masking (users table, MASK mode)
//   6. Add column generators (full_name, email, phone, dob, salary)
//   7. Run masking job
//   8. Verify job completes successfully
//   9. Verify record integrity (row count = 50)
//  10. Verify masking effectiveness (all 50 rows processed)
//
// The test uses the UI for the interactive steps and the API for seeding
// and verification, ensuring the frontend correctly drives the backend.

test.describe('Full Verification Workflow', () => {
  test.describe.configure({ mode: 'serial' })

  const workspaceName = `Full Verification Workspace ${Date.now()}`
  let workspaceId: number
  let token: string
  let sourceConnectionId: number
  let targetConnectionId: number
  let tableConfigId: number
  let jobId: number

  test('step 1 — register and login via UI', async ({ page }) => {
    await registerUser()

    await page.goto('/login')
    await expect(page.locator('h1')).toContainText('OpenDataMask')

    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")

    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })
    await waitForLoadingDone(page)

    await expect(page.locator('h1')).toContainText('Workspaces')

    // Store token for API calls in subsequent steps
    token = await loginViaApi()
  })

  test('step 2 — create workspace via UI', async ({ page }) => {
    // Login first
    await page.goto('/login')
    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")
    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })
    await waitForLoadingDone(page)

    // Create workspace
    await page.click("button:has-text('New Workspace')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    const nameInput = page.locator("[role='dialog'] input.form-control").first()
    await nameInput.fill(workspaceName)

    // Look for description input if exists
    const inputs = page.locator("[role='dialog'] input.form-control, [role='dialog'] textarea.form-control")
    if ((await inputs.count()) > 1) {
      await inputs.nth(1).fill('End-to-end verification test')
    }

    const createBtn = page.locator("[role='dialog'] button:has-text('Create'), [role='dialog'] button:has-text('Save')")
    await createBtn.first().click()

    await page.waitForSelector("[role='dialog']", { state: 'hidden', timeout: 10_000 })
    await page.reload()
    await waitForLoadingDone(page)

    // Verify workspace appears
    await expect(page.locator(`text=${workspaceName}`).first()).toBeVisible({ timeout: 10_000 })

    // Get workspace ID via API for subsequent steps
    const workspaces = await apiCall<WorkspaceResponse[]>('/api/workspaces', { token })
    const ws = workspaces.find((w) => w.name === workspaceName)
    expect(ws).toBeDefined()
    workspaceId = ws!.id
  })

  test('step 3 — create source database connection', async ({ page }) => {
    // Login and navigate
    await page.goto('/login')
    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")
    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })

    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')

    await page.click("button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    await page.fill("[role='dialog'] input[placeholder='Production DB']", 'verification-source')
    await page.fill("[role='dialog'] input[placeholder='localhost']", SOURCE_DB.host)
    await page.fill("[role='dialog'] input[placeholder='mydb']", SOURCE_DB.database)
    await page.fill("[role='dialog'] input[placeholder='admin']", SOURCE_DB.username)
    await page.fill("[role='dialog'] input[type='password']", SOURCE_DB.password)

    const sourceCheckbox = page.locator("label:has-text('Source') input[type='checkbox']")
    if (!(await sourceCheckbox.isChecked())) {
      await sourceCheckbox.click()
    }

    await page.click("[role='dialog'] button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { state: 'hidden', timeout: 10_000 })
    await waitForLoadingDone(page)

    await expect(page.locator('td:has-text("verification-source")')).toBeVisible()

    // Get connection ID via API
    const conns = await apiCall<ConnectionResponse[]>(`/api/workspaces/${workspaceId}/connections`, {
      token,
    })
    const srcConn = conns.find((c) => c.name === 'verification-source')
    expect(srcConn).toBeDefined()
    sourceConnectionId = srcConn!.id
  })

  test('step 4 — create destination database connection', async ({ page }) => {
    await page.goto('/login')
    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")
    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })

    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')

    await page.click("button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    await page.fill("[role='dialog'] input[placeholder='Production DB']", 'verification-target')
    await page.fill("[role='dialog'] input[placeholder='localhost']", TARGET_DB.host)
    await page.fill("[role='dialog'] input[placeholder='mydb']", TARGET_DB.database)
    await page.fill("[role='dialog'] input[placeholder='admin']", TARGET_DB.username)
    await page.fill("[role='dialog'] input[type='password']", TARGET_DB.password)

    const destCheckbox = page.locator("label:has-text('Destination') input[type='checkbox']")
    if (!(await destCheckbox.isChecked())) {
      await destCheckbox.click()
    }

    await page.click("[role='dialog'] button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { state: 'hidden', timeout: 10_000 })
    await waitForLoadingDone(page)

    await expect(page.locator('td:has-text("verification-target")')).toBeVisible()

    // Get connection ID via API
    const conns = await apiCall<ConnectionResponse[]>(`/api/workspaces/${workspaceId}/connections`, {
      token,
    })
    const tgtConn = conns.find((c) => c.name === 'verification-target')
    expect(tgtConn).toBeDefined()
    targetConnectionId = tgtConn!.id
  })

  test('step 5 — configure table masking for users table', async ({ page }) => {
    // Use API for table config (matching verification script's approach)
    const tbl = await apiCall<IdResponse>(`/api/workspaces/${workspaceId}/tables`, {
      method: 'POST',
      body: {
        connectionId: sourceConnectionId,
        tableName: 'users',
        schemaName: 'public',
        mode: 'MASK',
      },
      token,
    })
    tableConfigId = tbl.id

    // Verify via UI
    await page.goto('/login')
    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")
    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })

    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')
    await waitForLoadingDone(page)

    await expect(page.locator('text=users')).toBeVisible()
    await expect(page.locator('text=MASK').first()).toBeVisible()
  })

  test('step 6 — add column generators matching verification script', async ({ page }) => {
    // Add generators via API (matching run_verification.sh exactly)
    const generators = [
      { columnName: 'full_name', generatorType: 'FULL_NAME' },
      { columnName: 'email', generatorType: 'EMAIL' },
      { columnName: 'phone_number', generatorType: 'PHONE' },
      { columnName: 'date_of_birth', generatorType: 'BIRTH_DATE' },
      { columnName: 'salary', generatorType: 'RANDOM_INT', generatorParams: JSON.stringify({ min: '30000', max: '200000' }) },
    ]

    for (const gen of generators) {
      await apiCall(`/api/workspaces/${workspaceId}/tables/${tableConfigId}/generators`, {
        method: 'POST',
        body: gen,
        token,
      })
    }

    // Verify via UI
    await page.goto('/login')
    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")
    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })

    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')
    await waitForLoadingDone(page)

    // Expand to see generators
    const expandBtn = page.locator("button:has-text('Columns')").first()
    if (await expandBtn.isVisible()) {
      await expandBtn.click()

      // Wait for async generator fetch
      await page.waitForSelector('.font-mono', { timeout: 10_000 }).catch(() => {})

      await expect(page.locator('text=full_name').first()).toBeVisible()
      await expect(page.locator('text=email').first()).toBeVisible()
      await expect(page.locator('text=phone_number').first()).toBeVisible()
      await expect(page.locator('text=date_of_birth').first()).toBeVisible()
      await expect(page.locator('text=salary').first()).toBeVisible()
    }
  })

  test('step 7 — run masking job and wait for completion', async ({ page }) => {
    test.setTimeout(300_000)
    // Trigger job via API
    jobId = (
      await apiCall<IdResponse>(`/api/workspaces/${workspaceId}/jobs`, {
        method: 'POST',
        body: {
          name: 'Full Verification Masking Job',
          sourceConnectionId,
          targetConnectionId,
        },
        token,
      })
    ).id

    // Wait for completion
    const status = await waitForJobCompletion(token, workspaceId, jobId, 180_000)
    expect(status).toBe('COMPLETED')

    // Verify in UI
    await page.goto('/login')
    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")
    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })

    await page.goto(`/workspaces/${workspaceId}/jobs`)
    await waitForPageHeading(page, 'Jobs')
    await page.waitForSelector('.job-card', { timeout: 10_000 })

    await expect(page.locator('text=COMPLETED')).toBeVisible({ timeout: 15_000 })
  })

  test('step 8 — verify record integrity (50 rows processed)', async () => {
    // Mirrors verification/verify.py check_record_integrity
    const job = await apiCall<JobResponse>(`/api/workspaces/${workspaceId}/jobs/${jobId}`, {
      token,
    })

    expect(job.status).toBe('COMPLETED')
    expect(job.rowsProcessed).toBe(50)
    expect(job.tablesProcessed).toBe(1)
  })

  test('step 9 — verify job logs contain masking activity', async () => {
    // Mirrors verification/verify.py's log inspection
    const logs = await apiCall<JobLogEntry[]>(`/api/workspaces/${workspaceId}/jobs/${jobId}/logs`, {
      token,
    })

    expect(logs.length).toBeGreaterThan(0)

    // Logs should contain INFO-level entries about the masking process
    const infoLogs = logs.filter((l) => l.level === 'INFO')
    expect(infoLogs.length).toBeGreaterThan(0)
  })

  test('step 10 — verify connections remain healthy after job', async ({ page }) => {
    // Test both connections via API
    const srcTest = await apiCall<ConnectionTestResponse>(
      `/api/workspaces/${workspaceId}/connections/${sourceConnectionId}/test`,
      { method: 'POST', token }
    )
    expect(srcTest.success).toBe(true)

    const tgtTest = await apiCall<ConnectionTestResponse>(
      `/api/workspaces/${workspaceId}/connections/${targetConnectionId}/test`,
      { method: 'POST', token }
    )
    expect(tgtTest.success).toBe(true)

    // Also verify in UI
    await page.goto('/login')
    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")
    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })

    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')
    await waitForLoadingDone(page)

    // Both connections should be listed
    await expect(page.locator('td:has-text("verification-source")')).toBeVisible()
    await expect(page.locator('td:has-text("verification-target")')).toBeVisible()
  })
})
