<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const samlEnabled = import.meta.env.VITE_SAML_ENABLED === 'true'
const samlAuthEndpoint = '/saml2/authenticate/default'

async function handleLogin() {
  if (!username.value || !password.value) {
    error.value = 'Please enter your username and password.'
    return
  }
  loading.value = true
  error.value = ''
  try {
    await auth.login({ username: username.value, password: password.value })
    const redirect = route.query.redirect as string | undefined
    router.push(redirect ?? '/workspaces')
  } catch (e: unknown) {
    error.value = extractMessage(e)
  } finally {
    loading.value = false
  }
}

function extractMessage(e: unknown): string {
  if (e && typeof e === 'object' && 'response' in e) {
    const resp = (e as { response?: { data?: { message?: string } } }).response
    return resp?.data?.message ?? 'Login failed. Please check your credentials.'
  }
  if (e instanceof Error) return e.message
  return 'Login failed.'
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">🛡️</div>
        <h1 class="auth-title">OpenDataMask</h1>
        <p class="auth-subtitle">Sign in to your account</p>
      </div>

      <form class="auth-form" @submit.prevent="handleLogin">
        <div v-if="error" class="alert alert-error">{{ error }}</div>

        <div class="form-group">
          <label class="form-label" for="username">Username</label>
          <input
            id="username"
            v-model="username"
            type="text"
            class="form-control"
            placeholder="Enter your username"
            autocomplete="username"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="password">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            class="form-control"
            placeholder="Enter your password"
            autocomplete="current-password"
            required
          />
        </div>

        <button type="submit" class="btn btn-primary btn-full" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:1rem;height:1rem;border-width:2px;" />
          {{ loading ? 'Signing in…' : 'Sign In' }}
        </button>
      </form>

      <div class="auth-footer">
        Don't have an account?
        <RouterLink to="/register">Create one</RouterLink>
      </div>

      <div v-if="samlEnabled" class="sso-divider">
        <span>or</span>
      </div>

      <div v-if="samlEnabled" class="sso-section">
        <a :href="samlAuthEndpoint" class="btn btn-sso btn-full">
          🔑 Sign in with SSO
        </a>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%);
  padding: 1rem;
}
.auth-card {
  background: #fff;
  border-radius: 0.75rem;
  box-shadow: 0 20px 60px rgba(0,0,0,0.3);
  padding: 2.5rem;
  width: 100%;
  max-width: 420px;
}
.auth-header { text-align: center; margin-bottom: 2rem; }
.auth-logo { font-size: 3rem; margin-bottom: 0.5rem; }
.auth-title { font-size: 1.75rem; font-weight: 700; color: #111827; }
.auth-subtitle { color: #6b7280; margin-top: 0.25rem; }
.auth-form { display: flex; flex-direction: column; gap: 0; }
.btn-full { width: 100%; justify-content: center; padding: 0.65rem; font-size: 1rem; }
.auth-footer {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}
.auth-footer a { color: #3b82f6; font-weight: 500; }
.sso-divider {
  display: flex;
  align-items: center;
  margin: 1.25rem 0 1rem;
  color: #9ca3af;
  font-size: 0.875rem;
}
.sso-divider::before,
.sso-divider::after {
  content: '';
  flex: 1;
  border-top: 1px solid #e5e7eb;
}
.sso-divider span { margin: 0 0.75rem; }
.btn-sso {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.65rem 1rem;
  border: 1px solid #d1d5db;
  border-radius: 0.5rem;
  background: #fff;
  color: #374151;
  font-size: 1rem;
  font-weight: 500;
  text-decoration: none;
  transition: background 0.15s, border-color 0.15s;
  cursor: pointer;
}
.btn-sso:hover { background: #f9fafb; border-color: #9ca3af; }
</style>
