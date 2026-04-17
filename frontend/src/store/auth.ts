import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { User, LoginRequest, RegisterRequest, UserRole } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!user.value)

  function initializeFromStorage(): void {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    if (storedToken && storedUser) {
      try {
        token.value = storedToken
        user.value = JSON.parse(storedUser) as User
      } catch {
        clearStorage()
      }
    }
  }

  /**
   * Checks whether the current browser session is authenticated by calling
   * `/api/auth/me`. This covers both JWT-based and SAML session-based logins.
   * On success the user is marked as authenticated; on failure local state is cleared.
   */
  async function initializeFromSession(): Promise<void> {
    // Seed local state from storage first so JWT sessions work without a round-trip
    initializeFromStorage()
    if (isAuthenticated.value) return

    try {
      const sessionUser = await authApi.me()
      user.value = sessionUser
    } catch {
      clearStorage()
      user.value = null
      token.value = null
    }
  }

  async function login(credentials: LoginRequest): Promise<void> {
    const response = await authApi.login(credentials)
    token.value = response.token
    const loginUser: User = {
      id: response.userId,
      username: response.username,
      email: response.email,
      role: response.role,
      createdAt: new Date().toISOString()
    }
    user.value = loginUser
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(loginUser))
  }

  async function register(payload: RegisterRequest & { role?: UserRole }): Promise<void> {
    const response = await authApi.register(payload)
    token.value = response.token
    const regUser: User = {
      id: response.userId,
      username: response.username,
      email: response.email,
      role: response.role,
      createdAt: new Date().toISOString()
    }
    user.value = regUser
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(regUser))
  }

  async function logout(): Promise<void> {
    await authApi.logout()
    token.value = null
    user.value = null
    clearStorage()
  }

  function clearStorage(): void {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // Eagerly restore JWT auth state from localStorage so the router guard
  // can check isAuthenticated before App.vue's onMounted fires.
  initializeFromStorage()

  return {
    token,
    user,
    isAuthenticated,
    initializeFromStorage,
    initializeFromSession,
    login,
    logout,
    register
  }
})

