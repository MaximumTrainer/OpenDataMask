<script setup lang="ts">
import { useAuthStore } from '@/store/auth'
import { useRouter, useRoute } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}

function isActive(path: string): boolean {
  return route.path.startsWith(path)
}
</script>

<template>
  <header class="navbar">
    <div class="navbar-inner container">
      <!-- Brand -->
      <RouterLink to="/workspaces" class="brand">
        <span class="brand-icon">🛡️</span>
        <span class="brand-name">OpenDataMask</span>
      </RouterLink>

      <!-- Nav links -->
      <nav class="nav-links">
        <RouterLink
          to="/workspaces"
          class="nav-link"
          :class="{ active: isActive('/workspaces') }"
        >
          Workspaces
        </RouterLink>
        <RouterLink
          to="/settings/sensitivity-rules"
          class="nav-link"
          :class="{ active: isActive('/settings/sensitivity-rules') }"
        >
          Sensitivity Rules
        </RouterLink>
      </nav>

      <!-- User section -->
      <div class="nav-user">
        <span class="nav-username">
          👤 {{ auth.user?.username }}
          <span class="badge badge-gray" style="margin-left:4px;text-transform:lowercase;">
            {{ auth.user?.role }}
          </span>
        </span>
        <button class="btn btn-secondary btn-sm" @click="handleLogout">
          Logout
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.navbar {
  background: #1e293b;
  border-bottom: 1px solid #334155;
  position: sticky;
  top: 0;
  z-index: 100;
}
.navbar-inner {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  height: 3.5rem;
}
.brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  text-decoration: none;
  flex-shrink: 0;
}
.brand-icon { font-size: 1.25rem; }
.brand-name {
  font-weight: 700;
  font-size: 1.1rem;
  color: #f1f5f9;
  letter-spacing: -0.01em;
}
.nav-links {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  flex: 1;
}
.nav-link {
  color: #94a3b8;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  padding: 0.4rem 0.75rem;
  border-radius: 0.375rem;
  transition: color 0.15s, background 0.15s;
}
.nav-link:hover { color: #f1f5f9; background: #334155; }
.nav-link.active { color: #f1f5f9; background: #334155; }
.nav-user {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-shrink: 0;
}
.nav-username {
  font-size: 0.875rem;
  color: #94a3b8;
  display: flex;
  align-items: center;
}
@media (max-width: 640px) {
  .nav-username { display: none; }
}
</style>
