import { test, expect } from './odm-fixtures'
import {
  SOURCE_DB,
  TARGET_DB,
  loginViaApi,
  registerUser,
  apiCall,
  waitForPageHeading,
  waitForLoadingDone,
} from './odm-fixtures'

// ── Source & Destination Connection Tests ──────────────────────────────────
// Verifies that database connections can be created, displayed, tested, and
// managed through the frontend UI. Mirrors the source/target connection
// setup performed by verification/run_verification.sh.

test.describe('Database Connections', () => {
  let workspaceId: number
  let token: string

  test.beforeAll(async () => {
    await registerUser()
    token = await loginViaApi()

    const ws = await apiCall('/api/workspaces', {
      method: 'POST',
      body: { name: 'Connections Test Workspace', description: 'E2E connection tests' },
      token,
    })
    workspaceId = ws.id as number
  })

  test('connections page shows empty state when no connections exist', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')

    await expect(page.locator('.empty-state')).toBeVisible()
    await expect(page.locator('h3')).toContainText('No connections yet')
    await expect(page.locator("button:has-text('Add Connection')")).toBeVisible()
  })

  test('add connection modal opens and contains expected fields', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')

    await page.click("button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    // Verify modal title
    await expect(page.locator("[role='dialog']")).toContainText('Add Connection')

    // Verify essential form fields exist
    await expect(page.locator("[role='dialog'] input[placeholder='Production DB']")).toBeVisible()
    await expect(page.locator("[role='dialog'] select.form-control")).toBeVisible()
    await expect(page.locator("[role='dialog'] input[placeholder='localhost']")).toBeVisible()
    await expect(page.locator("[role='dialog'] input[placeholder='mydb']")).toBeVisible()
    await expect(page.locator("[role='dialog'] input[placeholder='admin']")).toBeVisible()

    // Verify Source and Destination checkboxes
    await expect(page.locator("label:has-text('Source')")).toBeVisible()
    await expect(page.locator("label:has-text('Destination')")).toBeVisible()

    // Verify action buttons
    await expect(page.locator("[role='dialog'] button:has-text('Add Connection')")).toBeVisible()
    await expect(page.locator("[role='dialog'] button:has-text('Cancel')")).toBeVisible()
  })

  test('form validation rejects empty required fields', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')

    await page.click("button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    // Clear any default values and try to submit
    await page.fill("[role='dialog'] input[placeholder='Production DB']", '')
    await page.click("[role='dialog'] button:has-text('Add Connection')")

    // Should show validation error
    await expect(page.locator("[role='dialog'] .alert-error")).toBeVisible({ timeout: 5_000 })
  })

  test('form validation requires at least one role (Source or Destination)', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')

    await page.click("button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    // Fill all fields but leave both checkboxes unchecked
    await page.fill("[role='dialog'] input[placeholder='Production DB']", 'test-conn')
    await page.fill("[role='dialog'] input[placeholder='localhost']", 'db-host')
    await page.fill("[role='dialog'] input[placeholder='mydb']", 'test_db')
    await page.fill("[role='dialog'] input[placeholder='admin']", 'test_user')
    await page.fill("[role='dialog'] input[type='password']", 'test_pass')

    await page.click("[role='dialog'] button:has-text('Add Connection')")

    await expect(page.locator("[role='dialog'] .alert-error")).toContainText('role', { ignoreCase: true })
  })

  test('create source database connection via UI', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')

    await page.click("button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    await page.fill("[role='dialog'] input[placeholder='Production DB']", SOURCE_DB.name)
    await page.fill("[role='dialog'] input[placeholder='localhost']", SOURCE_DB.host)
    await page.fill("[role='dialog'] input[placeholder='mydb']", SOURCE_DB.database)
    await page.fill("[role='dialog'] input[placeholder='admin']", SOURCE_DB.username)
    await page.fill("[role='dialog'] input[type='password']", SOURCE_DB.password)

    // Check Source role
    const sourceCheckbox = page.locator("label:has-text('Source') input[type='checkbox']")
    if (!(await sourceCheckbox.isChecked())) {
      await sourceCheckbox.click()
    }

    await page.click("[role='dialog'] button:has-text('Add Connection')")

    // Modal should close and connection should appear in the table
    await page.waitForSelector("[role='dialog']", { state: 'hidden', timeout: 10_000 })
    await waitForLoadingDone(page)

    // Verify connection appears in the list
    await expect(page.locator(`td:has-text("${SOURCE_DB.name}")`)).toBeVisible()
    await expect(page.locator('td .badge-blue')).toContainText('Source')
  })

  test('create destination database connection via UI', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')

    await page.click("button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    await page.fill("[role='dialog'] input[placeholder='Production DB']", TARGET_DB.name)
    await page.fill("[role='dialog'] input[placeholder='localhost']", TARGET_DB.host)
    await page.fill("[role='dialog'] input[placeholder='mydb']", TARGET_DB.database)
    await page.fill("[role='dialog'] input[placeholder='admin']", TARGET_DB.username)
    await page.fill("[role='dialog'] input[type='password']", TARGET_DB.password)

    // Check Destination role
    const destCheckbox = page.locator("label:has-text('Destination') input[type='checkbox']")
    if (!(await destCheckbox.isChecked())) {
      await destCheckbox.click()
    }

    await page.click("[role='dialog'] button:has-text('Add Connection')")

    // Modal should close
    await page.waitForSelector("[role='dialog']", { state: 'hidden', timeout: 10_000 })
    await waitForLoadingDone(page)

    // Verify both connections now exist
    await expect(page.locator(`td:has-text("${TARGET_DB.name}")`)).toBeVisible()
    await expect(page.locator('td .badge-green')).toContainText('Destination')
  })

  test('connections list shows correct columns', async ({ authenticatedPage: page }) => {
    // Ensure connections exist via API first
    try {
      await apiCall(`/api/workspaces/${workspaceId}/connections`, {
        method: 'POST',
        body: {
          name: 'list-test-source',
          type: 'POSTGRESQL',
          connectionString: `jdbc:postgresql://${SOURCE_DB.host}:${SOURCE_DB.port}/${SOURCE_DB.database}`,
          username: SOURCE_DB.username,
          password: SOURCE_DB.password,
          isSource: true,
          isDestination: false,
        },
        token,
      })
    } catch {
      // May already exist
    }

    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')
    await waitForLoadingDone(page)

    // Verify table headers
    const headers = page.locator('thead th')
    await expect(headers.nth(0)).toContainText('Name')
    await expect(headers.nth(1)).toContainText('Type')
    await expect(headers.nth(2)).toContainText('Host')
    await expect(headers.nth(3)).toContainText('Database')
    await expect(headers.nth(4)).toContainText('Roles')
    await expect(headers.nth(5)).toContainText('Status')
    await expect(headers.nth(6)).toContainText('Actions')
  })

  test('test connection button triggers connection test', async ({ authenticatedPage: page }) => {
    // Seed a connection via API
    let connId: number
    try {
      const conn = await apiCall(`/api/workspaces/${workspaceId}/connections`, {
        method: 'POST',
        body: {
          name: 'test-btn-conn',
          type: 'POSTGRESQL',
          connectionString: `jdbc:postgresql://${SOURCE_DB.host}:${SOURCE_DB.port}/${SOURCE_DB.database}`,
          username: SOURCE_DB.username,
          password: SOURCE_DB.password,
          isSource: true,
          isDestination: false,
        },
        token,
      })
      connId = conn.id as number
    } catch {
      // May already exist — just view the page
    }

    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')
    await waitForLoadingDone(page)

    // Click the first Test button
    const testBtn = page.locator("button:has-text('Test')").first()
    await expect(testBtn).toBeVisible()
    await testBtn.click()

    // Wait for test result badge to appear
    await expect(
      page.locator('.badge-green:has-text("OK"), .badge-red:has-text("Error")')
    ).toBeVisible({ timeout: 30_000 })
  })

  test('edit connection modal pre-fills existing data', async ({ authenticatedPage: page }) => {
    // Seed a connection via API
    const conn = await apiCall(`/api/workspaces/${workspaceId}/connections`, {
      method: 'POST',
      body: {
        name: 'edit-test-conn',
        type: 'POSTGRESQL',
        connectionString: `jdbc:postgresql://${SOURCE_DB.host}:${SOURCE_DB.port}/${SOURCE_DB.database}`,
        username: SOURCE_DB.username,
        password: SOURCE_DB.password,
        isSource: true,
        isDestination: false,
      },
      token,
    })

    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')
    await waitForLoadingDone(page)

    // Find the row for our connection and click Edit
    const row = page.locator(`tr:has-text("edit-test-conn")`)
    await row.locator("button:has-text('Edit')").click()

    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    // Modal should say "Edit Connection"
    await expect(page.locator("[role='dialog']")).toContainText('Edit Connection')

    // Name should be pre-filled
    const nameInput = page.locator("[role='dialog'] input[placeholder='Production DB']")
    await expect(nameInput).toHaveValue('edit-test-conn')
  })

  test('delete connection removes it from the list', async ({ authenticatedPage: page }) => {
    // Seed a connection to delete
    await apiCall(`/api/workspaces/${workspaceId}/connections`, {
      method: 'POST',
      body: {
        name: 'delete-me-conn',
        type: 'POSTGRESQL',
        connectionString: `jdbc:postgresql://${SOURCE_DB.host}:${SOURCE_DB.port}/${SOURCE_DB.database}`,
        username: SOURCE_DB.username,
        password: SOURCE_DB.password,
        isSource: true,
        isDestination: false,
      },
      token,
    })

    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')
    await waitForLoadingDone(page)

    // Confirm the connection exists
    await expect(page.locator('td:has-text("delete-me-conn")')).toBeVisible()

    // Set up dialog handler to accept the confirmation
    page.once('dialog', (dialog) => dialog.accept())

    // Click Delete
    const row = page.locator('tr:has-text("delete-me-conn")')
    await row.locator("button:has-text('Delete')").click()

    // Connection should disappear
    await expect(page.locator('td:has-text("delete-me-conn")')).toBeHidden({ timeout: 10_000 })
  })

  test('connection type selector includes PostgreSQL, MongoDB, Azure SQL, MySQL', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/connections`)
    await waitForPageHeading(page, 'Data Connections')

    await page.click("button:has-text('Add Connection')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    const typeSelect = page.locator("[role='dialog'] select.form-control")
    const options = await typeSelect.locator('option').allTextContents()

    expect(options).toContain('PostgreSQL')
    expect(options).toContain('MongoDB')
    expect(options).toContain('Azure SQL')
    expect(options).toContain('MySQL')
  })
})
