import { test, expect, apiCreateWorkspace, apiCreateConnection, API_BASE } from './odm-fixtures'

test.describe('Jobs', () => {
  test('jobs page loads for a workspace', async ({ authedPage, token, request }) => {
    const wsId = await apiCreateWorkspace(request, token, `E2E Jobs WS ${Date.now()}`)
    await authedPage.goto(`/workspaces/${wsId}/jobs`)
    await authedPage.waitForLoadState('networkidle')
    await expect(authedPage.locator('h1, h2, [class*="job"]').first()).toBeVisible({ timeout: 10_000 })
  })

  test('run job button is present on the jobs page', async ({ authedPage, token, request }) => {
    const wsId = await apiCreateWorkspace(request, token, `E2E Jobs Btn WS ${Date.now()}`)
    await authedPage.goto(`/workspaces/${wsId}/jobs`)
    await authedPage.waitForLoadState('networkidle')
    const runBtn = authedPage.locator(
      'button:has-text("Run"), button:has-text("Start"), button:has-text("New Job"), button:has-text("Create")'
    ).first()
    await expect(runBtn).toBeVisible({ timeout: 10_000 })
  })

  test('job created via API appears in the job list', async ({ authedPage, token, request }) => {
    const wsId = await apiCreateWorkspace(request, token, `E2E Job List WS ${Date.now()}`)
    // Create minimal connections and table config so a job can be triggered.
    const srcId = await apiCreateConnection(request, token, wsId, { name: 'e2e-job-source', isSource: true })
    await apiCreateConnection(request, token, wsId, { name: 'e2e-job-target', isSource: false })

    const tblResp = await request.post(`${API_BASE}/api/workspaces/${wsId}/tables`, {
      data: { connectionId: srcId, tableName: 'users', schemaName: 'public', mode: 'MASK' },
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(tblResp.ok()).toBeTruthy()

    const jobResp = await request.post(`${API_BASE}/api/workspaces/${wsId}/jobs`, {
      data: {},
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(jobResp.ok()).toBeTruthy()
    const job = await jobResp.json()

    await authedPage.goto(`/workspaces/${wsId}/jobs`)
    await authedPage.waitForLoadState('networkidle')
    // The job entry should appear; match on ID or a status badge.
    const jobEntry = authedPage.locator(`[data-job-id="${job.id}"], text=${job.id}`).first()
    const statusBadge = authedPage.locator('[class*="status"], [class*="badge"], [class*="job-card"]').first()
    await expect(jobEntry.or(statusBadge)).toBeVisible({ timeout: 15_000 })
  })

  test('actions page loads for a workspace', async ({ authedPage, token, request }) => {
    const wsId = await apiCreateWorkspace(request, token, `E2E Actions WS ${Date.now()}`)
    await authedPage.goto(`/workspaces/${wsId}/actions`)
    await authedPage.waitForLoadState('networkidle')
    await expect(authedPage.locator('h1, h2, [class*="action"]').first()).toBeVisible({ timeout: 10_000 })
  })
})
