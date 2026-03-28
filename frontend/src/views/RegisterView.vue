<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { UserRole } from '@/types'

const auth = useAuthStore()
const router = useRouter()

const username = ref('')
const email = ref('')
const password = ref('')
const confirmPassword = ref('')
const role = ref<UserRole>(UserRole.USER)
const error = ref('')
const loading = ref(false)

async function handleRegister() {
  error.value = ''
  if (!username.value || !email.value || !password.value) {
    error.value = 'All fields are required.'
    return
  }
  if (password.value !== confirmPassword.value) {
    error.value = 'Passwords do not match.'
    return
  }
  loading.value = true
  try {
    await auth.register({
      username: username.value,
      email: email.value,
      password: password.value,
      role: role.value
    })
    router.push('/workspaces')
  } catch (e: unknown) {
    error.value = extractMessage(e)
  } finally {
    loading.value = false
  }
}

function extractMessage(e: unknown): string {
  if (e && typeof e === 'object' && 'response' in e) {
    const resp = (e as { response?: { data?: { message?: string } } }).response
    return resp?.data?.message ?? 'Registration failed. Please try again.'
  }
  if (e instanceof Error) return e.message
  return 'Registration failed.'
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-header">
        <div class="auth-logo">🛡️</div>
        <h1 class="auth-title">OpenDataMask</h1>
        <p class="auth-subtitle">Create a new account</p>
      </div>

      <form class="auth-form" @submit.prevent="handleRegister">
        <div v-if="error" class="alert alert-error">{{ error }}</div>

        <div class="form-group">
          <label class="form-label" for="username">Username</label>
          <input
            id="username"
            v-model="username"
            type="text"
            class="form-control"
            placeholder="Choose a username"
            autocomplete="username"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="email">Email</label>
          <input
            id="email"
            v-model="email"
            type="email"
            class="form-control"
            placeholder="you@example.com"
            autocomplete="email"
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
            placeholder="Min. 8 characters"
            autocomplete="new-password"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="confirm">Confirm Password</label>
          <input
            id="confirm"
            v-model="confirmPassword"
            type="password"
            class="form-control"
            placeholder="Repeat your password"
            autocomplete="new-password"
            required
          />
        </div>

        <div class="form-group">
          <label class="form-label" for="role">Role</label>
          <select id="role" v-model="role" class="form-control">
            <option :value="UserRole.USER">User</option>
            <option :value="UserRole.ADMIN">Admin</option>
          </select>
        </div>

        <button type="submit" class="btn btn-primary btn-full" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:1rem;height:1rem;border-width:2px;" />
          {{ loading ? 'Creating account…' : 'Create Account' }}
        </button>
      </form>

      <div class="auth-footer">
        Already have an account?
        <RouterLink to="/login">Sign in</RouterLink>
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
  max-width: 440px;
}
.auth-header { text-align: center; margin-bottom: 2rem; }
.auth-logo { font-size: 3rem; margin-bottom: 0.5rem; }
.auth-title { font-size: 1.75rem; font-weight: 700; color: #111827; }
.auth-subtitle { color: #6b7280; margin-top: 0.25rem; }
.auth-form { display: flex; flex-direction: column; }
.btn-full { width: 100%; justify-content: center; padding: 0.65rem; font-size: 1rem; }
.auth-footer {
  text-align: center;
  margin-top: 1.5rem;
  font-size: 0.875rem;
  color: #6b7280;
}
.auth-footer a { color: #3b82f6; font-weight: 500; }
</style>
