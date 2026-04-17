import { test as base, expect, type Page } from '@playwright/test'

// ── Verification-sandbox defaults ─────────────────────────────────────────
// These match the credentials used by verification/run_verification.sh
// and verification/docker-compose.yml so the tests work out-of-the-box
// against the sandboxed environment without any extra configuration.

export const ODM_USERNAME = process.env.ODM_USERNAME ?? 'e2e_test_user'
export const ODM_PASSWORD = process.env.ODM_PASSWORD ?? 'E2eTest!Pass123'
export const ODM_EMAIL = process.env.ODM_EMAIL ?? 'e2e@odm-test.local'
export const API_BASE = process.env.ODM_API ?? 'http://localhost:8080'

export const SOURCE_DB = {
  name: 'e2e-source-db',
  type: 'POSTGRESQL' as const,
  host: process.env.SOURCE_DB_HOST ?? 'source_db',
  port: 5432,
  database: process.env.SOURCE_DB_NAME ?? 'source_db',
  username: process.env.SOURCE_DB_USER ?? 'source_user',
  password: process.env.SOURCE_DB_PASS ?? 'source_pass',
}

export const TARGET_DB = {
  name: 'e2e-target-db',
  type: 'POSTGRESQL' as const,
  host: process.env.TARGET_DB_HOST ?? 'target_db',
  port: 5432,
  database: process.env.TARGET_DB_NAME ?? 'target_db',
  username: process.env.TARGET_DB_USER ?? 'target_user',
  password: process.env.TARGET_DB_PASS ?? 'target_pass',
}

// ── REST API helpers ──────────────────────────────────────────────────────
// These mirror verification/run_verification.sh — we use the API to seed
// state and read results while keeping tests focused on the frontend.

// ── API response types ────────────────────────────────────────────────────

interface ApiOptions {
  method?: string
  body?: Record<string, unknown>
  token?: string
}

export interface IdResponse {
  id: number
}

export interface AuthTokenResponse {
  token: string
}

export interface ConnectionTestResponse {
  success: boolean
  message: string
}

export interface JobResponse {
  id: number
  status: string
  rowsProcessed: number
  tablesProcessed: number
  name: string
}

export interface JobLogEntry {
  level: string
  message: string
}

export interface WorkspaceResponse {
  id: number
  name: string
}

export interface ConnectionResponse {
  id: number
  name: string
  type: string
  isSource: boolean
  isDestination: boolean
}

export interface TableConfigResponse {
  id: number
  columnGenerators: Array<{ columnName: string; generatorType: string }>
}

export async function apiCall<T = unknown>(
  path: string,
  { method = 'GET', body, token }: ApiOptions = {}
): Promise<T> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`API ${method} ${path} → ${res.status}: ${text}`)
  }

  const text = await res.text()
  return text ? (JSON.parse(text) as T) : (undefined as unknown as T)
}

function isAlreadyRegisteredError(error: unknown): boolean {
  if (!(error instanceof Error)) return false
  const message = error.message.toLowerCase()
  return (
    message.includes('→ 409:') ||
    message.includes('already exists') ||
    message.includes('already registered')
  )
}

export async function registerUser(): Promise<void> {
  try {
    await apiCall('/api/auth/register', {
      method: 'POST',
      body: { username: ODM_USERNAME, email: ODM_EMAIL, password: ODM_PASSWORD },
    })
  } catch (error) {
    if (!isAlreadyRegisteredError(error)) {
      throw error
    }
  }
}

export async function loginViaApi(): Promise<string> {
  const resp = await apiCall<AuthTokenResponse>('/api/auth/login', {
    method: 'POST',
    body: { username: ODM_USERNAME, password: ODM_PASSWORD },
  })
  return resp.token
}

// ── Browser helpers ───────────────────────────────────────────────────────

export async function loginViaUi(page: Page): Promise<void> {
  await page.goto('/login')
  await page.fill('#username', ODM_USERNAME)
  await page.fill('#password', ODM_PASSWORD)
  await page.click("button[type='submit']")
  await page.waitForURL(/\/workspaces/, { timeout: 15_000 })
}

export async function waitForLoadingDone(page: Page, timeout = 10_000): Promise<void> {
  try {
    await page.waitForSelector('.loading-overlay', { state: 'hidden', timeout })
  } catch {
    // Overlay may never appear — safe to continue.
  }
}

export async function waitForPageHeading(page: Page, text: string, timeout = 15_000): Promise<void> {
  await page.waitForSelector(`h1:has-text("${text}")`, { timeout })
  await waitForLoadingDone(page)
}

// ── Seed helpers (API-driven, matching run_verification.sh) ────────────────

export interface SeedResult {
  token: string
  workspaceId: number
  sourceConnectionId: number
  targetConnectionId: number
  tableConfigId: number
}

export async function seedVerificationData(): Promise<SeedResult> {
  await registerUser()
  const token = await loginViaApi()

  const ws = await apiCall<IdResponse>('/api/workspaces', {
    method: 'POST',
    body: { name: 'E2E Verification Workspace', description: 'Playwright E2E test workspace' },
    token,
  })
  const workspaceId = ws.id

  const src = await apiCall<IdResponse>(`/api/workspaces/${workspaceId}/connections`, {
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
  const sourceConnectionId = src.id

  const tgt = await apiCall<IdResponse>(`/api/workspaces/${workspaceId}/connections`, {
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
  const targetConnectionId = tgt.id

  const tbl = await apiCall<IdResponse>(`/api/workspaces/${workspaceId}/tables`, {
    method: 'POST',
    body: {
      connectionId: sourceConnectionId,
      tableName: 'users',
      schemaName: 'public',
      mode: 'MASK',
    },
    token,
  })
  const tableConfigId = tbl.id

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

  return { token, workspaceId, sourceConnectionId, targetConnectionId, tableConfigId }
}

export async function runMaskingJobViaApi(
  token: string,
  workspaceId: number,
  sourceConnectionId: number,
  targetConnectionId: number
): Promise<number> {
  const job = await apiCall<IdResponse>(`/api/workspaces/${workspaceId}/jobs`, {
    method: 'POST',
    body: {
      name: 'E2E Verification Job',
      sourceConnectionId,
      targetConnectionId,
    },
    token,
  })
  return job.id
}

export async function waitForJobCompletion(
  token: string,
  workspaceId: number,
  jobId: number,
  timeoutMs = 120_000
): Promise<string> {
  const start = Date.now()
  while (Date.now() - start < timeoutMs) {
    const resp = await apiCall<JobResponse>(`/api/workspaces/${workspaceId}/jobs/${jobId}`, { token })
    const status = resp.status
    if (status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED') {
      return status
    }
    await new Promise((r) => setTimeout(r, 3_000))
  }
  return 'TIMEOUT'
}

// ── Custom test fixture ───────────────────────────────────────────────────

export type OdmFixtures = {
  authenticatedPage: Page
}

export const test = base.extend<OdmFixtures>({
  authenticatedPage: async ({ page }, use) => {
    await registerUser()
    await loginViaUi(page)
    await use(page)
  },
})

export { expect }
