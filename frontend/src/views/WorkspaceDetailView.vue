<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorkspaceStore } from '@/store/workspace'

const route = useRoute()
const router = useRouter()
const store = useWorkspaceStore()

const workspaceId = computed(() => Number(route.params.id))
const ws = computed(() => store.currentWorkspace)

onMounted(() => store.fetchWorkspace(workspaceId.value))

const tabs = [
  { label: 'Overview',    icon: '📋', path: () => `/workspaces/${workspaceId.value}` },
  { label: 'Connections', icon: '🔌', path: () => `/workspaces/${workspaceId.value}/connections` },
  { label: 'Tables',      icon: '📊', path: () => `/workspaces/${workspaceId.value}/tables` },
  { label: 'Jobs',        icon: '⚙️', path: () => `/workspaces/${workspaceId.value}/jobs` }
]

function navigate(path: string) {
  router.push(path)
}

function isCurrentTab(tab: typeof tabs[0]) {
  const p = tab.path()
  return route.path === p
}

function formatDate(d: string) {
  return new Date(d).toLocaleString()
}
</script>

<template>
  <div class="page">
    <!-- Breadcrumb -->
    <div class="breadcrumb">
      <RouterLink to="/workspaces">Workspaces</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <span>{{ ws?.name ?? 'Loading…' }}</span>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="loading-overlay">
      <span class="spinner spinner-lg" />
    </div>

    <template v-else-if="ws">
      <!-- Header -->
      <div class="page-header">
        <div>
          <h1>{{ ws.name }}</h1>
          <p v-if="ws.description" class="text-gray-500 text-sm mt-1">{{ ws.description }}</p>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs">
        <button
          v-for="tab in tabs"
          :key="tab.label"
          class="tab"
          :class="{ active: isCurrentTab(tab) }"
          @click="navigate(tab.path())"
        >
          {{ tab.icon }} {{ tab.label }}
        </button>
      </div>

      <!-- Overview Content -->
      <div class="grid grid-cols-2">
        <div class="card">
          <h3 class="card-title mb-4">Workspace Details</h3>
          <dl class="detail-list">
            <dt>ID</dt>
            <dd>#{{ ws.id }}</dd>
            <dt>Name</dt>
            <dd>{{ ws.name }}</dd>
            <dt>Owner</dt>
            <dd>{{ ws.ownerUsername }}</dd>
            <dt>Created</dt>
            <dd>{{ formatDate(ws.createdAt) }}</dd>
            <dt>Updated</dt>
            <dd>{{ formatDate(ws.updatedAt) }}</dd>
          </dl>
        </div>

        <div class="card">
          <h3 class="card-title mb-4">Quick Actions</h3>
          <div class="quick-actions">
            <button
              class="quick-btn"
              @click="navigate(`/workspaces/${workspaceId}/connections`)"
            >
              <span class="quick-icon">🔌</span>
              <div>
                <div class="font-semibold">Connections</div>
                <div class="text-sm text-gray-500">Manage data sources</div>
              </div>
            </button>
            <button
              class="quick-btn"
              @click="navigate(`/workspaces/${workspaceId}/tables`)"
            >
              <span class="quick-icon">📊</span>
              <div>
                <div class="font-semibold">Tables</div>
                <div class="text-sm text-gray-500">Configure masking rules</div>
              </div>
            </button>
            <button
              class="quick-btn"
              @click="navigate(`/workspaces/${workspaceId}/jobs`)"
            >
              <span class="quick-icon">⚙️</span>
              <div>
                <div class="font-semibold">Jobs</div>
                <div class="text-sm text-gray-500">Run & monitor jobs</div>
              </div>
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- Not found -->
    <div v-else-if="store.error" class="alert alert-error">{{ store.error }}</div>
  </div>
</template>

<style scoped>
.detail-list {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 0.5rem 1rem;
  font-size: 0.9rem;
}
dt {
  font-weight: 500;
  color: #6b7280;
}
dd {
  color: #111827;
}
.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.quick-btn {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.875rem 1rem;
  border-radius: 0.5rem;
  border: 1px solid #e5e7eb;
  background: #f9fafb;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
  width: 100%;
  text-align: left;
}
.quick-btn:hover { background: #eff6ff; border-color: #93c5fd; }
.quick-icon { font-size: 1.5rem; flex-shrink: 0; }
</style>
