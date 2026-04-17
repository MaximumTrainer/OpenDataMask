import { test, expect } from '@playwright/test'
import {
  ODM_USERNAME,
  ODM_PASSWORD,
  registerUser,
  waitForLoadingDone,
} from './odm-fixtures'

// ── Auth / Login Tests ────────────────────────────────────────────────────
// Verifies the login page renders correctly, rejects bad credentials, and
// allows a registered user to sign in and reach the workspaces page.

test.describe('Authentication', () => {
  test.beforeAll(async () => {
    await registerUser()
  })

  test('login page renders with expected form fields', async ({ page }) => {
    await page.goto('/login')

    await expect(page.locator('h1')).toContainText('OpenDataMask')
    await expect(page.locator('#username')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()
    await expect(page.locator("button[type='submit']")).toBeVisible()
    await expect(page.locator("button[type='submit']")).toContainText('Sign In')
  })

  test('register page renders with expected form fields', async ({ page }) => {
    await page.goto('/register')

    await expect(page.locator('h1')).toContainText('OpenDataMask')
    await expect(page.locator('#username')).toBeVisible()
    await expect(page.locator('#email')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()
    await expect(page.locator("button[type='submit']")).toBeVisible()
  })

  test('login with invalid credentials shows error', async ({ page }) => {
    await page.goto('/login')
    await page.fill('#username', 'nonexistent_user')
    await page.fill('#password', 'wrong_password')
    await page.click("button[type='submit']")

    await expect(page.locator('.alert-error')).toBeVisible({ timeout: 10_000 })
  })

  test('login with empty fields shows validation message', async ({ page }) => {
    await page.goto('/login')

    // Click submit without filling anything
    await page.click("button[type='submit']")

    // The form should show a validation error or not navigate away
    await expect(page).toHaveURL(/\/login/)
  })

  test('successful login redirects to workspaces', async ({ page }) => {
    await page.goto('/login')
    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")

    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })
    await waitForLoadingDone(page)

    await expect(page.locator('h1')).toContainText('Workspaces')
  })

  test('authenticated user accessing /login is redirected to /workspaces', async ({ page }) => {
    // First login
    await page.goto('/login')
    await page.fill('#username', ODM_USERNAME)
    await page.fill('#password', ODM_PASSWORD)
    await page.click("button[type='submit']")
    await page.waitForURL(/\/workspaces/, { timeout: 15_000 })

    // Now navigate to /login — should redirect back
    await page.goto('/login')
    await expect(page).toHaveURL(/\/workspaces/)
  })

  test('unauthenticated user accessing protected route is redirected to /login', async ({ page }) => {
    // Clear any stored credentials
    await page.goto('/login')
    await page.evaluate(() => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    })

    await page.goto('/workspaces')
    await expect(page).toHaveURL(/\/login/)
  })
})
