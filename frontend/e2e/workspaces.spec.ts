import { test, expect, registerUser, loginViaApi, apiCall, type IdResponse } from './odm-fixtures'

test.describe('Workspaces', () => {
  test('workspaces list page loads after login', async ({ authenticatedPage: page }) => {
    await expect(page).toHaveURL(/\/workspaces/)
    await expect(
      page.locator('h1, h2, [class*="workspace"]').first()
    ).toBeVisible({ timeout: 10_000 })
  })

  test('create workspace button is present', async ({ authenticatedPage: page }) => {
    await page.goto('/workspaces')
    const createBtn = page.locator(
      'button:has-text("Create"), button:has-text("New"), button:has-text("Add")'
    ).first()
    await expect(createBtn).toBeVisible({ timeout: 10_000 })
  })

  test('can open create workspace dialog', async ({ authenticatedPage: page }) => {
    await page.goto('/workspaces')
    const createBtn = page.locator(
      'button:has-text("Create"), button:has-text("New"), button:has-text("Add")'
    ).first()
    await createBtn.click()
    await expect(
      page.locator('[role="dialog"] input, .modal input, form input[name="name"]').first()
    ).toBeVisible({ timeout: 8_000 })
  })

  test('workspace created via API appears in the list', async ({ authenticatedPage: page }) => {
    await registerUser()
    const token = await loginViaApi()
    const name = `E2E WS ${Date.now()}`
    await apiCall<IdResponse>('/api/workspaces', {
      method: 'POST',
      body: { name, description: '' },
      token,
    })
    await page.goto('/workspaces')
    await page.waitForLoadState('networkidle')
    await expect(page.locator(`text=${name}`)).toBeVisible({ timeout: 10_000 })
  })

  test('clicking a workspace navigates to its detail page', async ({ authenticatedPage: page }) => {
    await registerUser()
    const token = await loginViaApi()
    const name = `E2E WS Nav ${Date.now()}`
    await apiCall<IdResponse>('/api/workspaces', {
      method: 'POST',
      body: { name, description: '' },
      token,
    })
    await page.goto('/workspaces')
    await page.waitForLoadState('networkidle')
    await page.locator(`text=${name}`).first().click()
    await page.waitForURL(/\/workspaces\/\d+/, { timeout: 10_000 })
  })
})

