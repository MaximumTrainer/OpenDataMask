// @vitest-environment jsdom
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

/**
 * Mock the API layer so tests never hit a real backend.
 * This also demonstrates how to simulate a "logged-in SAML state" for UI tests:
 * replace the `me()` call with a resolved value representing a SAML-asserted user.
 */
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  register: vi.fn(),
  logout: vi.fn(),
  me: vi.fn()
}))

import * as authApi from '@/api/auth'
import { useAuthStore } from '@/store/auth'
import type { User } from '@/types'
import { UserRole } from '@/types'

// ── Helpers ────────────────────────────────────────────────────────────────

const mockSamlUser: User = {
  id: 0,
  username: 'saml-test-user',
  email: 'saml@example.com',
  role: UserRole.USER,
  createdAt: new Date().toISOString()
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('useAuthStore – SAML session initialisation', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.resetAllMocks()
  })

  it('is not authenticated by default', () => {
    const auth = useAuthStore()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.user).toBeNull()
  })

  it('becomes authenticated after initializeFromSession resolves a SAML user', async () => {
    // Simulate the backend returning a SAML-asserted user via /api/auth/me
    vi.mocked(authApi.me).mockResolvedValue(mockSamlUser)

    const auth = useAuthStore()
    await auth.initializeFromSession()

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.user?.username).toBe('saml-test-user')
  })

  it('remains unauthenticated when the /me call fails (no active session)', async () => {
    vi.mocked(authApi.me).mockRejectedValue(new Error('401 Unauthorized'))

    const auth = useAuthStore()
    await auth.initializeFromSession()

    expect(auth.isAuthenticated).toBe(false)
    expect(auth.user).toBeNull()
  })

  it('uses localStorage JWT session without calling /me', async () => {
    const storedUser: User = { ...mockSamlUser, username: 'jwt-user' }
    localStorage.setItem('token', 'some.jwt.token')
    localStorage.setItem('user', JSON.stringify(storedUser))

    const auth = useAuthStore()
    await auth.initializeFromSession()

    // /me should NOT be called when a local JWT session already exists
    expect(authApi.me).not.toHaveBeenCalled()
    expect(auth.isAuthenticated).toBe(true)
    expect(auth.user?.username).toBe('jwt-user')
  })

  it('clears the authenticated state on logout', async () => {
    vi.mocked(authApi.me).mockResolvedValue(mockSamlUser)
    vi.mocked(authApi.logout).mockResolvedValue(undefined)

    const auth = useAuthStore()
    await auth.initializeFromSession()
    expect(auth.isAuthenticated).toBe(true)

    await auth.logout()

    expect(auth.isAuthenticated).toBe(false)
    expect(auth.user).toBeNull()
  })
})

describe('useAuthStore – login / register with flat AuthResponse', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.resetAllMocks()
  })

  const flatLoginResponse = {
    token: 'jwt.login.token',
    userId: 42,
    username: 'test-user',
    email: 'test@example.com',
    role: UserRole.USER
  }

  const flatRegisterResponse = {
    token: 'jwt.register.token',
    userId: 99,
    username: 'new-user',
    email: 'new@example.com',
    role: UserRole.ADMIN
  }

  it('populates user correctly after login with flat AuthResponse', async () => {
    vi.mocked(authApi.login).mockResolvedValue(flatLoginResponse)

    const auth = useAuthStore()
    await auth.login({ username: 'test-user', password: 'secret' })

    expect(auth.user).not.toBeNull()
    expect(auth.user?.id).toBe(42)
    expect(auth.user?.username).toBe('test-user')
    expect(auth.user?.email).toBe('test@example.com')
    expect(auth.user?.role).toBe(UserRole.USER)
    expect(auth.token).toBe('jwt.login.token')
  })

  it('sets isAuthenticated to true after login', async () => {
    vi.mocked(authApi.login).mockResolvedValue(flatLoginResponse)

    const auth = useAuthStore()
    expect(auth.isAuthenticated).toBe(false)

    await auth.login({ username: 'test-user', password: 'secret' })

    expect(auth.isAuthenticated).toBe(true)
  })

  it('persists constructed User to localStorage after login', async () => {
    vi.mocked(authApi.login).mockResolvedValue(flatLoginResponse)

    const auth = useAuthStore()
    await auth.login({ username: 'test-user', password: 'secret' })

    expect(localStorage.getItem('token')).toBe('jwt.login.token')
    const storedUser = JSON.parse(localStorage.getItem('user')!)
    expect(storedUser.id).toBe(42)
    expect(storedUser.username).toBe('test-user')
    expect(storedUser.email).toBe('test@example.com')
    expect(storedUser.role).toBe(UserRole.USER)
  })

  it('populates user correctly after register with flat AuthResponse', async () => {
    vi.mocked(authApi.register).mockResolvedValue(flatRegisterResponse)

    const auth = useAuthStore()
    await auth.register({ username: 'new-user', email: 'new@example.com', password: 'secret123' })

    expect(auth.user).not.toBeNull()
    expect(auth.user?.id).toBe(99)
    expect(auth.user?.username).toBe('new-user')
    expect(auth.user?.email).toBe('new@example.com')
    expect(auth.user?.role).toBe(UserRole.ADMIN)
    expect(auth.token).toBe('jwt.register.token')
  })

  it('sets isAuthenticated to true after register', async () => {
    vi.mocked(authApi.register).mockResolvedValue(flatRegisterResponse)

    const auth = useAuthStore()
    expect(auth.isAuthenticated).toBe(false)

    await auth.register({ username: 'new-user', email: 'new@example.com', password: 'secret123' })

    expect(auth.isAuthenticated).toBe(true)
  })

  it('persists constructed User to localStorage after register', async () => {
    vi.mocked(authApi.register).mockResolvedValue(flatRegisterResponse)

    const auth = useAuthStore()
    await auth.register({ username: 'new-user', email: 'new@example.com', password: 'secret123' })

    expect(localStorage.getItem('token')).toBe('jwt.register.token')
    const storedUser = JSON.parse(localStorage.getItem('user')!)
    expect(storedUser.id).toBe(99)
    expect(storedUser.username).toBe('new-user')
    expect(storedUser.email).toBe('new@example.com')
    expect(storedUser.role).toBe(UserRole.ADMIN)
  })
})

describe('useAuthStore – mocking the SAML user directly (unit-test style)', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.resetAllMocks()
  })

  /**
   * Pattern for component tests: mock the store so no API calls are made.
   * Example usage in a component test:
   *
   *   const mockUser = { name: 'Test User', email: 'test@example.com', authenticated: true }
   *   vi.mock('@/store/auth', () => ({
   *     useAuthStore: () => ({ user: mockUser, isAuthenticated: true })
   *   }))
   */
  it('can stub isAuthenticated and user directly for component testing', () => {
    const auth = useAuthStore()

    // Directly patch the store state (Pinia allows this in tests via $patch)
    auth.$patch({ user: mockSamlUser })

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.user?.username).toBe('saml-test-user')
  })
})
