import { test, expect, apiCreateWorkspace } from './odm-fixtures'

test.describe('Workspaces', () => {
  test('workspaces list page loads after login', async ({ authedPage }) => {
    await expect(authedPage).toHaveURL(/\/workspaces/)
    // The page heading or a workspaces-specific element should be visible.
    await expect(
      authedPage.locator('h1, h2, [class*="workspace"]').first()
    ).toBeVisible({ timeout: 10_000 })
  })

  test('create workspace button is present', async ({ authedPage }) => {
    await authedPage.goto('/workspaces')
    const createBtn = authedPage.locator(
      'button:has-text("Create"), button:has-text("New"), button:has-text("Add")'
    ).first()
    await expect(createBtn).toBeVisible({ timeout: 10_000 })
  })

  test('can open create workspace dialog', async ({ authedPage }) => {
    await authedPage.goto('/workspaces')
    const createBtn = authedPage.locator(
      'button:has-text("Create"), button:has-text("New"), button:has-text("Add")'
    ).first()
    await createBtn.click()
    // A modal/dialog or form with a name field should appear.
    await expect(
      authedPage.locator('[role="dialog"] input, .modal input, form input[name="name"]').first()
    ).toBeVisible({ timeout: 8_000 })
  })

  test('workspace created via API appears in the list', async ({ authedPage, token, request }) => {
    const name = `E2E WS ${Date.now()}`
    await apiCreateWorkspace(request, token, name)
    await authedPage.goto('/workspaces')
    await authedPage.waitForLoadState('networkidle')
    await expect(authedPage.locator(`text=${name}`)).toBeVisible({ timeout: 10_000 })
  })

  test('clicking a workspace navigates to its detail page', async ({ authedPage, token, request }) => {
    const name = `E2E WS Nav ${Date.now()}`
    const wsId = await apiCreateWorkspace(request, token, name)
    await authedPage.goto('/workspaces')
    await authedPage.waitForLoadState('networkidle')
    await authedPage.locator(`text=${name}`).first().click()
    await authedPage.waitForURL(/\/workspaces\/\d+/, { timeout: 10_000 })
  })
})
