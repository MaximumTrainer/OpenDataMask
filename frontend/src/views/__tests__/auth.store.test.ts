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
