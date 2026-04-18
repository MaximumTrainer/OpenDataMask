import { test, expect } from './odm-fixtures'

test.describe('Sensitivity Rules', () => {
  test('sensitivity rules page loads', async ({ authenticatedPage: page }) => {
    await page.goto('/settings/sensitivity-rules')
    await page.waitForLoadState('networkidle')
    await expect(
      page.locator('h1, h2, [class*="rule"], [class*="sensitivity"]').first()
    ).toBeVisible({ timeout: 10_000 })
  })

  test('new rule button or add button is present', async ({ authenticatedPage: page }) => {
    await page.goto('/settings/sensitivity-rules')
    await page.waitForLoadState('networkidle')
    const newRuleBtn = page.locator(
      'button:has-text("New"), button:has-text("Add"), button:has-text("Create")'
    ).first()
    await expect(newRuleBtn).toBeVisible({ timeout: 10_000 })
  })

  test('new rule drawer or dialog opens when triggered', async ({ authenticatedPage: page }) => {
    await page.goto('/settings/sensitivity-rules')
    await page.waitForLoadState('networkidle')
    const newRuleBtn = page.locator(
      'button:has-text("New"), button:has-text("Add"), button:has-text("Create")'
    ).first()
    await newRuleBtn.click()
    await expect(
      page.locator('.drawer input, [role="dialog"] input, .modal input').first()
    ).toBeVisible({ timeout: 8_000 })
  })

  test('rule name input accepts text in the new-rule drawer', async ({ authenticatedPage: page }) => {
    await page.goto('/settings/sensitivity-rules')
    await page.waitForLoadState('networkidle')
    const newRuleBtn = page.locator(
      'button:has-text("New"), button:has-text("Add"), button:has-text("Create")'
    ).first()
    await newRuleBtn.click()
    const nameInput = page.locator(
      '.drawer input, [role="dialog"] input, .modal input'
    ).first()
    await expect(nameInput).toBeVisible({ timeout: 8_000 })
    await nameInput.fill('TEST_RULE')
    await expect(nameInput).toHaveValue('TEST_RULE')
  })
})

