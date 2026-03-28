import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import * as authApi from '@/api/auth'
import type { User, LoginRequest, RegisterRequest, UserRole } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)

  const isAuthenticated = computed(() => !!token.value && !!user.value)

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

  async function login(credentials: LoginRequest): Promise<void> {
    const response = await authApi.login(credentials)
    token.value = response.token
    user.value = response.user
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(response.user))
  }

  async function register(payload: RegisterRequest & { role?: UserRole }): Promise<void> {
    const response = await authApi.register(payload)
    token.value = response.token
    user.value = response.user
    localStorage.setItem('token', response.token)
    localStorage.setItem('user', JSON.stringify(response.user))
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

  return { token, user, isAuthenticated, initializeFromStorage, login, logout, register }
})
