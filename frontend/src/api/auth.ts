import apiClient from './client'
import type { AuthResponse, LoginRequest, RegisterRequest, User } from '@/types'

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

/**
 * Checks the current session against the backend and returns basic user info.
 * Works for both JWT (Bearer token) and SAML (session cookie) based sessions.
 */
export async function me(): Promise<User> {
  const { data } = await apiClient.get<{ username: string; authenticated: string }>('/auth/me')
  // The /api/auth/me endpoint returns a minimal representation; we reconstruct a
  // partial User so callers can populate the store without a full profile endpoint.
  return {
    id: 0,
    username: data.username,
    email: '',
    role: 'USER' as User['role'],
    createdAt: new Date().toISOString()
  }
}

