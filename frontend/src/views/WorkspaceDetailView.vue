<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useWorkspaceStore } from '@/store/workspace'
import { getWorkspaceStats } from '@/api/workspaces'
import type { WorkspaceStats } from '@/types'

const route = useRoute()
const router = useRouter()
const store = useWorkspaceStore()

const workspaceId = computed(() => Number(route.params.id))
const ws = computed(() => store.currentWorkspace)

const stats = ref<WorkspaceStats | null>(null)
const statsLoading = ref(false)
const statsError = ref<string | null>(null)

onMounted(async () => {
  store.fetchWorkspace(workspaceId.value)
  await fetchStats()
})

async function fetchStats() {
  statsLoading.value = true
  statsError.value = null
  try {
    stats.value = await getWorkspaceStats(workspaceId.value)
  } catch {
    statsError.value = 'Failed to load workspace statistics'
  } finally {
    statsLoading.value = false
  }
}

const tabs = [
  { label: 'Overview',      icon: '📋', path: () => `/workspaces/${workspaceId.value}` },
  { label: 'Connections',   icon: '🔌', path: () => `/workspaces/${workspaceId.value}/connections` },
  { label: 'Tables',        icon: '📊', path: () => `/workspaces/${workspaceId.value}/tables` },
  { label: 'Data Mappings', icon: '🗺️', path: () => `/workspaces/${workspaceId.value}/mappings` },
  { label: 'Jobs',          icon: '⚙️', path: () => `/workspaces/${workspaceId.value}/jobs` },
  { label: 'Actions',       icon: '⚡', path: () => `/workspaces/${workspaceId.value}/actions` }
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
              @click="navigate(`/workspaces/${workspaceId}/mappings`)"
            >
              <span class="quick-icon">🗺️</span>
              <div>
                <div class="font-semibold">Data Mappings</div>
                <div class="text-sm text-gray-500">Custom column-level mapping wizard</div>
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
            <button
              class="quick-btn"
              @click="navigate(`/workspaces/${workspaceId}/actions`)"
            >
              <span class="quick-icon">⚡</span>
              <div>
                <div class="font-semibold">Actions</div>
                <div class="text-sm text-gray-500">Post-job webhooks & notifications</div>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- Statistics Card -->
      <div class="card mt-4">
        <h3 class="card-title mb-4">Statistics</h3>
        <div v-if="statsLoading" class="skeleton" style="height: 6rem;" />
        <div v-else-if="statsError" class="alert alert-error">{{ statsError }}</div>
        <dl v-else-if="stats" class="detail-list">
          <dt>Connections</dt>
          <dd>{{ stats.connectionCount }}</dd>
          <dt>Table Configurations</dt>
          <dd>{{ stats.tableConfigCount }}</dd>
          <dt>Total Jobs Run</dt>
          <dd>{{ stats.totalJobsRun }}</dd>
          <dt>Last Job Status</dt>
          <dd>
            <span
              v-if="stats.lastJobStatus"
              :class="{
                'stat-status-green': stats.lastJobStatus === 'COMPLETED',
                'stat-status-red': stats.lastJobStatus === 'FAILED',
                'stat-status-orange': stats.lastJobStatus === 'RUNNING',
                'stat-status-gray': !['COMPLETED','FAILED','RUNNING'].includes(stats.lastJobStatus)
              }"
            >{{ stats.lastJobStatus }}</span>
            <span v-else class="stat-status-gray">—</span>
          </dd>
          <dt>Last Job Date</dt>
          <dd>{{ stats.lastJobAt ? formatDate(stats.lastJobAt) : '—' }}</dd>
        </dl>
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
.stat-status-green  { color: #16a34a; font-weight: 500; }
.stat-status-red    { color: #dc2626; font-weight: 500; }
.stat-status-orange { color: #ea580c; font-weight: 500; }
.stat-status-gray   { color: #6b7280; }
</style>
