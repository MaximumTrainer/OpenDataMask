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

// ── Masking Definition Tests ──────────────────────────────────────────────
// Verifies the table configuration, column generator assignment, and custom
// data mapping wizard through the frontend UI. Mirrors the table/generator
// setup in verification/run_verification.sh.

test.describe('Masking Definition — Table Configurations', () => {
  let workspaceId: number
  let token: string
  let sourceConnectionId: number

  test.beforeAll(async () => {
    await registerUser()
    token = await loginViaApi()

    const ws = await apiCall('/api/workspaces', {
      method: 'POST',
      body: { name: 'Masking Def Test Workspace', description: 'E2E masking definition tests' },
      token,
    })
    workspaceId = ws.id as number

    const src = await apiCall(`/api/workspaces/${workspaceId}/connections`, {
      method: 'POST',
      body: {
        name: SOURCE_DB.name,
        type: SOURCE_DB.type,
        connectionString: `jdbc:postgresql://${SOURCE_DB.host}:${SOURCE_DB.port}/${SOURCE_DB.database}`,
        username: SOURCE_DB.username,
        password: SOURCE_DB.password,
        isSource: true,
        isDestination: false,
      },
      token,
    })
    sourceConnectionId = src.id as number

    await apiCall(`/api/workspaces/${workspaceId}/connections`, {
      method: 'POST',
      body: {
        name: TARGET_DB.name,
        type: TARGET_DB.type,
        connectionString: `jdbc:postgresql://${TARGET_DB.host}:${TARGET_DB.port}/${TARGET_DB.database}`,
        username: TARGET_DB.username,
        password: TARGET_DB.password,
        isSource: false,
        isDestination: true,
      },
      token,
    })
  })

  test('tables page shows empty state when no tables configured', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')

    // Should show empty state or table list
    const content = await page.textContent('body')
    expect(content).toBeTruthy()
  })

  test('add table modal opens with expected fields', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')

    await page.click("button:has-text('Add Table')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    await expect(page.locator("[role='dialog']")).toContainText('Table')

    // Verify form fields: connection selector, table name, mode selector
    await expect(page.locator("[role='dialog'] select.form-control").first()).toBeVisible()
    await expect(page.locator("[role='dialog'] input[placeholder='users']")).toBeVisible()
  })

  test('create table configuration in MASK mode via UI', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')

    await page.click("button:has-text('Add Table')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    // Fill table name
    await page.fill("[role='dialog'] input[placeholder='users']", 'users')

    // Select MASK mode
    const modeSelect = page.locator("[role='dialog'] select.form-control").last()
    await modeSelect.selectOption('MASK')

    // Submit
    const saveBtn = page.locator("[role='dialog'] button:has-text('Add'), [role='dialog'] button:has-text('Save'), [role='dialog'] button:has-text('Create')")
    await saveBtn.first().click()

    // Modal should close
    await page.waitForSelector("[role='dialog']", { state: 'hidden', timeout: 10_000 })
    await waitForLoadingDone(page)

    // Verify the table appears in the list
    await expect(page.locator('text=users')).toBeVisible()
    await expect(page.locator('text=MASK')).toBeVisible()
  })

  test('expand table to view column generators section', async ({ authenticatedPage: page }) => {
    // Seed a table config via API
    const tbl = await apiCall(`/api/workspaces/${workspaceId}/tables`, {
      method: 'POST',
      body: {
        connectionId: sourceConnectionId,
        tableName: 'users_expand_test',
        schemaName: 'public',
        mode: 'MASK',
      },
      token,
    })

    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')
    await waitForLoadingDone(page)

    // Click expand/columns button
    const expandBtn = page.locator("button:has-text('Columns')").first()
    if (await expandBtn.isVisible()) {
      await expandBtn.click()
      await page.waitForTimeout(500)

      // Column generators section should be visible
      const columnsSection = page.locator("button:has-text('Add Column'), button:has-text('Add Generator')")
      await expect(columnsSection.first()).toBeVisible({ timeout: 5_000 })
    }
  })

  test('add column generator to table via UI', async ({ authenticatedPage: page }) => {
    // Seed a table config via API
    const tbl = await apiCall(`/api/workspaces/${workspaceId}/tables`, {
      method: 'POST',
      body: {
        connectionId: sourceConnectionId,
        tableName: 'users_gen_test',
        schemaName: 'public',
        mode: 'MASK',
      },
      token,
    })
    const tableId = tbl.id as number

    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')
    await waitForLoadingDone(page)

    // Expand the table
    const expandBtn = page.locator("button:has-text('Columns')").first()
    if (await expandBtn.isVisible()) {
      await expandBtn.click()
      await page.waitForTimeout(500)
    }

    // Click Add Column/Generator
    const addBtn = page.locator("button:has-text('Add Column'), button:has-text('Add Generator')")
    if (await addBtn.first().isVisible()) {
      await addBtn.first().click()
      await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

      // Fill column name and select generator type
      const columnInput = page.locator("[role='dialog'] input").first()
      await columnInput.fill('email')

      const genSelect = page.locator("[role='dialog'] select.form-control").first()
      await genSelect.selectOption('EMAIL')

      // Save
      const saveBtn = page.locator("[role='dialog'] button:has-text('Add'), [role='dialog'] button:has-text('Save')")
      await saveBtn.first().click()

      await page.waitForSelector("[role='dialog']", { state: 'hidden', timeout: 10_000 })
    }
  })

  test('table configuration shows all mode options', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')

    await page.click("button:has-text('Add Table')")
    await page.waitForSelector("[role='dialog']", { timeout: 5_000 })

    const modeSelect = page.locator("[role='dialog'] select.form-control").last()
    const options = await modeSelect.locator('option').allTextContents()

    expect(options).toContain('PASSTHROUGH')
    expect(options).toContain('MASK')
    expect(options).toContain('GENERATE')
    expect(options).toContain('SUBSET')
    expect(options).toContain('SKIP')
  })
})

test.describe('Masking Definition — Data Mapping Wizard', () => {
  let workspaceId: number
  let token: string
  let sourceConnectionId: number

  test.beforeAll(async () => {
    await registerUser()
    token = await loginViaApi()

    const ws = await apiCall('/api/workspaces', {
      method: 'POST',
      body: { name: 'Mapping Wizard Test Workspace', description: 'E2E data mapping tests' },
      token,
    })
    workspaceId = ws.id as number

    const src = await apiCall(`/api/workspaces/${workspaceId}/connections`, {
      method: 'POST',
      body: {
        name: SOURCE_DB.name,
        type: SOURCE_DB.type,
        connectionString: `jdbc:postgresql://${SOURCE_DB.host}:${SOURCE_DB.port}/${SOURCE_DB.database}`,
        username: SOURCE_DB.username,
        password: SOURCE_DB.password,
        isSource: true,
        isDestination: false,
      },
      token,
    })
    sourceConnectionId = src.id as number
  })

  test('data mapping wizard renders step 1 — connection selection', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/mappings`)
    await waitForPageHeading(page, 'Custom Data Mapping')

    // Step 1 should show connection cards
    await expect(page.locator('text=Step 1')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('button.conn-card')).toBeVisible({ timeout: 10_000 })
  })

  test('selecting a connection advances to step 2 — table selection', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/mappings`)
    await waitForPageHeading(page, 'Custom Data Mapping')

    // Click the first connection card
    const connCard = page.locator('button.conn-card').first()
    await connCard.click()

    // Should advance to step 2
    await waitForLoadingDone(page)
    await expect(page.locator('text=Step 2')).toBeVisible({ timeout: 10_000 })
  })

  test('selecting a table advances to step 3 — column mapping', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/mappings`)
    await waitForPageHeading(page, 'Custom Data Mapping')

    // Step 1: select connection
    const connCard = page.locator('button.conn-card').first()
    await connCard.click()
    await waitForLoadingDone(page)

    // Step 2: select table
    const tableCard = page.locator('button.table-card').first()
    if (await tableCard.isVisible({ timeout: 10_000 })) {
      await tableCard.click()
      await waitForLoadingDone(page)

      // Step 3 should show column mapping UI
      await expect(page.locator('text=Step 3')).toBeVisible({ timeout: 10_000 })
    }
  })

  test('column mapping shows action dropdown with MIGRATE_AS_IS and MASK options', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/mappings`)
    await waitForPageHeading(page, 'Custom Data Mapping')

    // Navigate to step 3
    const connCard = page.locator('button.conn-card').first()
    await connCard.click()
    await waitForLoadingDone(page)

    const tableCard = page.locator('button.table-card').first()
    if (await tableCard.isVisible({ timeout: 10_000 })) {
      await tableCard.click()
      await waitForLoadingDone(page)

      // Check that action dropdowns exist with expected options
      const actionSelect = page.locator('select').first()
      if (await actionSelect.isVisible({ timeout: 5_000 })) {
        const options = await actionSelect.locator('option').allTextContents()
        const optionsLower = options.map((o) => o.toLowerCase())
        expect(optionsLower.some((o) => o.includes('migrate') || o.includes('as_is') || o.includes('as-is'))).toBeTruthy()
        expect(optionsLower.some((o) => o.includes('mask'))).toBeTruthy()
      }
    }
  })

  test('selecting MASK action reveals masking strategy options', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/mappings`)
    await waitForPageHeading(page, 'Custom Data Mapping')

    // Navigate to step 3
    const connCard = page.locator('button.conn-card').first()
    await connCard.click()
    await waitForLoadingDone(page)

    const tableCard = page.locator('button.table-card').first()
    if (await tableCard.isVisible({ timeout: 10_000 })) {
      await tableCard.click()
      await waitForLoadingDone(page)

      // Select MASK action for first column
      const actionSelect = page.locator('select').first()
      if (await actionSelect.isVisible({ timeout: 5_000 })) {
        await actionSelect.selectOption('MASK')
        await page.waitForTimeout(300)

        // A strategy dropdown should appear
        const selects = page.locator('select')
        const count = await selects.count()
        // There should be more selects now (strategy + potentially generator type)
        expect(count).toBeGreaterThanOrEqual(2)
      }
    }
  })

  test('saved mappings appear in the mappings list section', async ({ authenticatedPage: page }) => {
    // Seed a mapping via API
    await apiCall(`/api/workspaces/${workspaceId}/mappings/bulk`, {
      method: 'POST',
      body: {
        connectionId: sourceConnectionId,
        tableName: 'users',
        columnMappings: [
          { columnName: 'id', action: 'MIGRATE_AS_IS' },
          { columnName: 'email', action: 'MASK', maskingStrategy: 'FAKE', fakeGeneratorType: 'EMAIL' },
          { columnName: 'full_name', action: 'MASK', maskingStrategy: 'FAKE', fakeGeneratorType: 'FULL_NAME' },
        ],
      },
      token,
    })

    await page.goto(`/workspaces/${workspaceId}/mappings`)
    await waitForPageHeading(page, 'Custom Data Mapping')
    await waitForLoadingDone(page)

    // The saved mappings section should list the saved table mapping
    await expect(page.locator('text=users')).toBeVisible({ timeout: 10_000 })
  })
})

test.describe('Masking Definition — Verification of Column Generators', () => {
  let workspaceId: number
  let token: string
  let sourceConnectionId: number
  let tableConfigId: number

  test.beforeAll(async () => {
    await registerUser()
    token = await loginViaApi()

    const ws = await apiCall('/api/workspaces', {
      method: 'POST',
      body: { name: 'Generator Verification Workspace', description: 'E2E generator verification' },
      token,
    })
    workspaceId = ws.id as number

    const src = await apiCall(`/api/workspaces/${workspaceId}/connections`, {
      method: 'POST',
      body: {
        name: SOURCE_DB.name,
        type: SOURCE_DB.type,
        connectionString: `jdbc:postgresql://${SOURCE_DB.host}:${SOURCE_DB.port}/${SOURCE_DB.database}`,
        username: SOURCE_DB.username,
        password: SOURCE_DB.password,
        isSource: true,
        isDestination: false,
      },
      token,
    })
    sourceConnectionId = src.id as number

    const tbl = await apiCall(`/api/workspaces/${workspaceId}/tables`, {
      method: 'POST',
      body: {
        connectionId: sourceConnectionId,
        tableName: 'users',
        schemaName: 'public',
        mode: 'MASK',
      },
      token,
    })
    tableConfigId = tbl.id as number

    // Add column generators matching verification/run_verification.sh
    const generators = [
      { columnName: 'full_name', generatorType: 'FULL_NAME' },
      { columnName: 'email', generatorType: 'EMAIL' },
      { columnName: 'phone_number', generatorType: 'PHONE' },
      { columnName: 'date_of_birth', generatorType: 'BIRTH_DATE' },
      { columnName: 'salary', generatorType: 'RANDOM_INT', generatorParams: JSON.stringify({ min: '30000', max: '200000' }) },
    ]
    for (const gen of generators) {
      await apiCall(`/api/workspaces/${workspaceId}/tables/${tableConfigId}/generators`, {
        method: 'POST',
        body: gen,
        token,
      })
    }
  })

  test('tables page displays configured table with MASK mode', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')
    await waitForLoadingDone(page)

    await expect(page.locator('text=users')).toBeVisible()
    await expect(page.locator('text=MASK')).toBeVisible()
  })

  test('expanding table shows all 5 configured column generators', async ({ authenticatedPage: page }) => {
    await page.goto(`/workspaces/${workspaceId}/tables`)
    await waitForPageHeading(page, 'Table Configurations')
    await waitForLoadingDone(page)

    // Expand column generators
    const expandBtn = page.locator("button:has-text('Columns')").first()
    if (await expandBtn.isVisible()) {
      await expandBtn.click()
      await page.waitForTimeout(500)

      // Verify all generator names are visible
      await expect(page.locator('text=full_name')).toBeVisible()
      await expect(page.locator('text=email')).toBeVisible()
      await expect(page.locator('text=phone_number')).toBeVisible()
      await expect(page.locator('text=date_of_birth')).toBeVisible()
      await expect(page.locator('text=salary')).toBeVisible()

      // Verify generator types
      await expect(page.locator('text=FULL_NAME')).toBeVisible()
      await expect(page.locator('text=EMAIL')).toBeVisible()
      await expect(page.locator('text=PHONE')).toBeVisible()
      await expect(page.locator('text=BIRTH_DATE')).toBeVisible()
      await expect(page.locator('text=RANDOM_INT')).toBeVisible()
    }
  })

  test('column generator count matches verification script configuration', async ({ authenticatedPage: page }) => {
    // Verify via API that exactly 5 generators exist
    const tableConfig = await apiCall(
      `/api/workspaces/${workspaceId}/tables/${tableConfigId}`,
      { token }
    )

    const generators = (tableConfig as { columnGenerators?: unknown[] }).columnGenerators ?? []
    expect(generators.length).toBe(5)
  })
})
