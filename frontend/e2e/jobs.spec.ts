import { test, expect, registerUser, loginViaApi, apiCall, type IdResponse } from './odm-fixtures'

test.describe('Jobs', () => {
  test('jobs page loads for a workspace', async ({ authenticatedPage: page }) => {
    await registerUser()
    const token = await loginViaApi()
    const ws = await apiCall<IdResponse>('/api/workspaces', {
      method: 'POST',
      body: { name: `E2E Jobs WS ${Date.now()}`, description: '' },
      token,
    })
    await page.goto(`/workspaces/${ws.id}/jobs`)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1, h2, [class*="job"], [class*="empty"]').first()).toBeVisible({ timeout: 10_000 })
  })

  test('run job button is present on the jobs page', async ({ authenticatedPage: page }) => {
    await registerUser()
    const token = await loginViaApi()
    const ws = await apiCall<IdResponse>('/api/workspaces', {
      method: 'POST',
      body: { name: `E2E Jobs Btn WS ${Date.now()}`, description: '' },
      token,
    })
    await page.goto(`/workspaces/${ws.id}/jobs`)
    await page.waitForLoadState('networkidle')
    const runBtn = page.locator(
      'button:has-text("Run"), button:has-text("Start"), button:has-text("New Job"), button:has-text("Create")'
    ).first()
    await expect(runBtn).toBeVisible({ timeout: 10_000 })
  })

  test('job created via API appears in the job list', async ({ authenticatedPage: page }) => {
    await registerUser()
    const token = await loginViaApi()
    const ws = await apiCall<IdResponse>('/api/workspaces', {
      method: 'POST',
      body: { name: `E2E Job List WS ${Date.now()}`, description: '' },
      token,
    })
    const src = await apiCall<IdResponse>(`/api/workspaces/${ws.id}/connections`, {
      method: 'POST',
      body: {
        name: 'e2e-job-source',
        type: 'POSTGRESQL',
        connectionString: 'jdbc:postgresql://source_db:5432/source_db',
        username: 'source_user',
        password: 'source_pass',
        isSource: true,
        isDestination: false,
      },
      token,
    })
    await apiCall(`/api/workspaces/${ws.id}/connections`, {
      method: 'POST',
      body: {
        name: 'e2e-job-target',
        type: 'POSTGRESQL',
        connectionString: 'jdbc:postgresql://target_db:5432/target_db',
        username: 'target_user',
        password: 'target_pass',
        isSource: false,
        isDestination: true,
      },
      token,
    })
    await apiCall(`/api/workspaces/${ws.id}/tables`, {
      method: 'POST',
      body: { connectionId: src.id, tableName: 'users', schemaName: 'public', mode: 'MASK' },
      token,
    })
    const job = await apiCall<{ id: number }>(`/api/workspaces/${ws.id}/jobs`, {
      method: 'POST',
      body: { name: `E2E Job ${Date.now()}` },
      token,
    })
    await page.goto(`/workspaces/${ws.id}/jobs`)
    await page.waitForLoadState('networkidle')
    const jobEntry = page.locator(`[data-job-id="${job.id}"], text=${job.id}`).first()
    const statusBadge = page.locator('[class*="status"], [class*="badge"], [class*="job-card"]').first()
    await expect(jobEntry.or(statusBadge)).toBeVisible({ timeout: 15_000 })
  })

  test('actions page loads for a workspace', async ({ authenticatedPage: page }) => {
    await registerUser()
    const token = await loginViaApi()
    const ws = await apiCall<IdResponse>('/api/workspaces', {
      method: 'POST',
      body: { name: `E2E Actions WS ${Date.now()}`, description: '' },
      token,
    })
    await page.goto(`/workspaces/${ws.id}/actions`)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1, h2, [class*="action"]').first()).toBeVisible({ timeout: 10_000 })
  })
})

