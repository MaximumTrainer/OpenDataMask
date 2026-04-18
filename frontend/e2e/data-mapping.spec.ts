import { test, expect, apiCreateWorkspace, API_BASE } from './odm-fixtures'

test.describe('Data Mapping', () => {
  test('data mapping page loads for a workspace', async ({ authedPage, token, request }) => {
    const wsId = await apiCreateWorkspace(request, token, `E2E Mapping WS ${Date.now()}`)
    await authedPage.goto(`/workspaces/${wsId}/mappings`)
    await authedPage.waitForLoadState('networkidle')
    await expect(authedPage.locator('h1, h2, [class*="mapping"], [class*="data"]').first()).toBeVisible({ timeout: 10_000 })
  })

  test('table configuration section is accessible', async ({ authedPage, token, request }) => {
    const wsId = await apiCreateWorkspace(request, token, `E2E Table WS ${Date.now()}`)

    // Create a table configuration via the API so something is present to display.
    const srcResp = await request.post(`${API_BASE}/api/workspaces/${wsId}/connections`, {
      data: {
        name: 'e2e-map-source',
        type: 'POSTGRESQL',
        connectionString: 'jdbc:postgresql://source_db:5432/source_db',
        username: 'source_user',
        password: 'source_pass',
        isSource: true,
        isDestination: false,
      },
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(srcResp.ok()).toBeTruthy()

    await authedPage.goto(`/workspaces/${wsId}/mappings`)
    await authedPage.waitForLoadState('networkidle')
    // The page should render without errors — heading or empty-state is acceptable.
    await expect(authedPage.locator('h1, h2, [class*="empty"], [class*="mapping"]').first()).toBeVisible({ timeout: 10_000 })
  })

  test('tables configuration page loads', async ({ authedPage, token, request }) => {
    const wsId = await apiCreateWorkspace(request, token, `E2E Tables WS ${Date.now()}`)
    await authedPage.goto(`/workspaces/${wsId}/tables`)
    await authedPage.waitForLoadState('networkidle')
    await expect(authedPage.locator('h1, h2, [class*="table"]').first()).toBeVisible({ timeout: 10_000 })
  })
})
