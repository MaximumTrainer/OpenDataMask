import { test, expect } from './odm-fixtures'

test.describe('Sensitivity Rules', () => {
  test('sensitivity rules page loads', async ({ authedPage }) => {
    await authedPage.goto('/settings/sensitivity-rules')
    await authedPage.waitForLoadState('networkidle')
    await expect(
      authedPage.locator('h1, h2, [class*="rule"], [class*="sensitivity"]').first()
    ).toBeVisible({ timeout: 10_000 })
  })

  test('new rule button or add button is present', async ({ authedPage }) => {
    await authedPage.goto('/settings/sensitivity-rules')
    await authedPage.waitForLoadState('networkidle')
    const newRuleBtn = authedPage.locator(
      'button:has-text("New"), button:has-text("Add"), button:has-text("Create")'
    ).first()
    await expect(newRuleBtn).toBeVisible({ timeout: 10_000 })
  })

  test('new rule drawer or dialog opens when triggered', async ({ authedPage }) => {
    await authedPage.goto('/settings/sensitivity-rules')
    await authedPage.waitForLoadState('networkidle')
    const newRuleBtn = authedPage.locator(
      'button:has-text("New"), button:has-text("Add"), button:has-text("Create")'
    ).first()
    await newRuleBtn.click()
    // A drawer or modal with at least an input field should appear.
    await expect(
      authedPage.locator('.drawer input, [role="dialog"] input, .modal input').first()
    ).toBeVisible({ timeout: 8_000 })
  })

  test('rule name input accepts text in the new-rule drawer', async ({ authedPage }) => {
    await authedPage.goto('/settings/sensitivity-rules')
    await authedPage.waitForLoadState('networkidle')
    const newRuleBtn = authedPage.locator(
      'button:has-text("New"), button:has-text("Add"), button:has-text("Create")'
    ).first()
    await newRuleBtn.click()
    const nameInput = authedPage.locator(
      '.drawer input, [role="dialog"] input, .modal input'
    ).first()
    await expect(nameInput).toBeVisible({ timeout: 8_000 })
    await nameInput.fill('TEST_RULE')
    await expect(nameInput).toHaveValue('TEST_RULE')
  })
})
