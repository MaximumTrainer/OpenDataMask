import apiClient from './client'
import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types'

export async function login(credentials: LoginRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', credentials)
  return data
}

export async function register(payload: RegisterRequest): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/register', payload)
  return data
}

export async function logout(): Promise<void> {
  await apiClient.post('/auth/logout').catch(() => {
    // Ignore – we always clear local state regardless
  })
}
