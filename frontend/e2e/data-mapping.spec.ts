import { test, expect, registerUser, loginViaApi, apiCall, type IdResponse } from './odm-fixtures'

test.describe('Data Mapping', () => {
  test('data mapping page loads for a workspace', async ({ authenticatedPage: page }) => {
    await registerUser()
    const token = await loginViaApi()
    const ws = await apiCall<IdResponse>('/api/workspaces', {
      method: 'POST',
      body: { name: `E2E Mapping WS ${Date.now()}`, description: '' },
      token,
    })
    await page.goto(`/workspaces/${ws.id}/mappings`)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1, h2, [class*="mapping"], [class*="data"]').first()).toBeVisible({ timeout: 10_000 })
  })

  test('table configuration section is accessible', async ({ authenticatedPage: page }) => {
    await registerUser()
    const token = await loginViaApi()
    const ws = await apiCall<IdResponse>('/api/workspaces', {
      method: 'POST',
      body: { name: `E2E Table WS ${Date.now()}`, description: '' },
      token,
    })
    await apiCall(`/api/workspaces/${ws.id}/connections`, {
      method: 'POST',
      body: {
        name: 'e2e-map-source',
        type: 'POSTGRESQL',
        connectionString: 'jdbc:postgresql://source_db:5432/source_db',
        username: 'source_user',
        password: 'source_pass',
        isSource: true,
        isDestination: false,
      },
      token,
    })
    await page.goto(`/workspaces/${ws.id}/mappings`)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1, h2, [class*="empty"], [class*="mapping"]').first()).toBeVisible({ timeout: 10_000 })
  })

  test('tables configuration page loads', async ({ authenticatedPage: page }) => {
    await registerUser()
    const token = await loginViaApi()
    const ws = await apiCall<IdResponse>('/api/workspaces', {
      method: 'POST',
      body: { name: `E2E Tables WS ${Date.now()}`, description: '' },
      token,
    })
    await page.goto(`/workspaces/${ws.id}/tables`)
    await page.waitForLoadState('networkidle')
    await expect(page.locator('h1, h2, [class*="table"]').first()).toBeVisible({ timeout: 10_000 })
  })
})

