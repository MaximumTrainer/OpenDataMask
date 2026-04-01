<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import AppModal from '@/components/AppModal.vue'
import StatusBadge from '@/components/StatusBadge.vue'
import * as jobsApi from '@/api/jobs'
import * as connectionsApi from '@/api/connections'
import type { Job, JobRequest, JobLog, DataConnection } from '@/types'
import { JobStatus } from '@/types'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

const jobs = ref<Job[]>([])
const connections = ref<DataConnection[]>([])
const loading = ref(false)
const error = ref('')

// Create modal
const showCreateModal = ref(false)
const createForm = ref<JobRequest>({ name: '', sourceConnectionId: 0, targetConnectionId: 0 })
const createError = ref('')
const creating = ref(false)

// Logs
const expandedJobId = ref<number | null>(null)
const jobLogs = ref<Record<number, JobLog[]>>({})
const loadingLogs = ref<number | null>(null)

// Auto-refresh for running jobs
let refreshTimer: ReturnType<typeof setInterval> | null = null

async function fetchJobs() {
  loading.value = true
  error.value = ''
  try {
    jobs.value = await jobsApi.listJobs(workspaceId.value)
  } catch {
    error.value = 'Failed to load jobs.'
  } finally {
    loading.value = false
  }
}

async function fetchConnections() {
  try {
    connections.value = await connectionsApi.listConnections(workspaceId.value)
  } catch {
    // ignore
  }
}

onMounted(async () => {
  await Promise.all([fetchJobs(), fetchConnections()])
  startAutoRefresh()
})

function startAutoRefresh() {
  refreshTimer = setInterval(async () => {
    const hasActive = jobs.value.some(
      (j) => j.status === JobStatus.RUNNING || j.status === JobStatus.PENDING
    )
    if (hasActive) {
      const fresh = await jobsApi.listJobs(workspaceId.value)
      jobs.value = fresh
    }
  }, 5000)
}

// Cleanup timer on unmount
import { onUnmounted } from 'vue'
onUnmounted(() => {
  if (refreshTimer !== null) clearInterval(refreshTimer)
})

// ── Create ──
function openCreate() {
  createForm.value = {
    name: '',
    sourceConnectionId: connections.value[0]?.id ?? 0,
    targetConnectionId: connections.value[0]?.id ?? 0
  }
  createError.value = ''
  showCreateModal.value = true
}

async function submitCreate() {
  if (!createForm.value.name || !createForm.value.sourceConnectionId || !createForm.value.targetConnectionId) {
    createError.value = 'All fields are required.'
    return
  }
  creating.value = true
  createError.value = ''
  try {
    const job = await jobsApi.createJob(workspaceId.value, createForm.value)
    jobs.value.unshift(job)
    showCreateModal.value = false
  } catch {
    createError.value = 'Failed to create job.'
  } finally {
    creating.value = false
  }
}

// ── Cancel ──
async function handleCancel(job: Job) {
  if (!confirm(`Cancel job "${job.name}"?`)) return
  try {
    const updated = await jobsApi.cancelJob(workspaceId.value, job.id)
    const idx = jobs.value.findIndex((j) => j.id === job.id)
    if (idx !== -1) jobs.value[idx] = updated
  } catch {
    alert('Failed to cancel job.')
  }
}

// ── Logs ──
async function toggleLogs(job: Job) {
  if (expandedJobId.value === job.id) {
    expandedJobId.value = null
    return
  }
  expandedJobId.value = job.id
  if (!jobLogs.value[job.id]) {
    loadingLogs.value = job.id
    try {
      jobLogs.value[job.id] = await jobsApi.getJobLogs(workspaceId.value, job.id)
    } catch {
      jobLogs.value[job.id] = []
    } finally {
      loadingLogs.value = null
    }
  }
}

// ── Helpers ──
function formatDate(d?: string) {
  if (!d) return '—'
  return new Date(d).toLocaleString()
}

function duration(job: Job): string {
  if (!job.startedAt) return '—'
  const end = job.completedAt ? new Date(job.completedAt) : new Date()
  const secs = Math.round((end.getTime() - new Date(job.startedAt).getTime()) / 1000)
  if (secs < 60) return `${secs}s`
  if (secs < 3600) return `${Math.floor(secs / 60)}m ${secs % 60}s`
  return `${Math.floor(secs / 3600)}h ${Math.floor((secs % 3600) / 60)}m`
}

function connectionName(id: number) {
  return connections.value.find((c) => c.id === id)?.name ?? `#${id}`
}

function logLevelClass(level: string) {
  const map: Record<string, string> = {
    INFO: 'log-level INFO',
    WARN: 'log-level WARN',
    ERROR: 'log-level ERROR',
    DEBUG: 'log-level DEBUG'
  }
  return map[level] ?? 'log-level'
}

function canCancel(job: Job) {
  return job.status === JobStatus.RUNNING || job.status === JobStatus.PENDING
}

function progressPercent(job: Job) {
  if (!job.tablesTotal) return 0
  return Math.round((job.tablesProcessed / job.tablesTotal) * 100)
}
</script>

<template>
  <div class="page">
    <!-- Breadcrumb -->
    <div class="breadcrumb">
      <RouterLink to="/workspaces">Workspaces</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <RouterLink :to="`/workspaces/${workspaceId}`">Workspace</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <span>Jobs</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Jobs</h1>
        <p class="text-gray-500 text-sm mt-1">Monitor and manage masking jobs</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">⚙ Run New Job</button>
    </div>

    <div v-if="loading" class="loading-overlay">
      <span class="spinner spinner-lg" />
    </div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="jobs.length === 0" class="empty-state">
      <div class="empty-state-icon">⚙️</div>
      <h3>No jobs yet</h3>
      <p>Run a masking job to anonymize your data.</p>
      <button class="btn btn-primary mt-4" @click="openCreate">⚙ Run New Job</button>
    </div>

    <div v-else class="jobs-list">
      <div v-for="job in jobs" :key="job.id" class="job-card card">
        <!-- Job header -->
        <div class="job-header">
          <div class="job-title-row">
            <div>
              <div class="flex items-center gap-2 mb-1">
                <span class="font-semibold text-lg">{{ job.name }}</span>
                <StatusBadge :status="job.status" />
              </div>
              <div class="text-sm text-gray-500 flex flex-wrap gap-3">
                <span>Source: <strong>{{ connectionName(job.sourceConnectionId) }}</strong></span>
                <span>Target: <strong>{{ connectionName(job.targetConnectionId) }}</strong></span>
                <span>Created: {{ formatDate(job.createdAt) }}</span>
                <span v-if="job.startedAt">Duration: {{ duration(job) }}</span>
              </div>
            </div>
          </div>
          <div class="flex gap-2 items-start">
            <button
              class="btn btn-secondary btn-sm"
              @click="toggleLogs(job)"
            >
              {{ expandedJobId === job.id ? '▲ Hide Logs' : '▼ View Logs' }}
            </button>
            <button
              v-if="canCancel(job)"
              class="btn btn-danger btn-sm"
              @click="handleCancel(job)"
            >
              Cancel
            </button>
          </div>
        </div>

        <!-- Progress bar (for RUNNING) -->
        <div v-if="job.status === JobStatus.RUNNING && job.tablesTotal > 0" class="progress-section">
          <div class="progress-label">
            <span class="text-sm text-gray-600">
              Tables: {{ job.tablesProcessed }}/{{ job.tablesTotal }}
              · Rows processed: {{ job.rowsProcessed.toLocaleString() }}
            </span>
            <span class="text-sm font-semibold">{{ progressPercent(job) }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: `${progressPercent(job)}%` }" />
          </div>
        </div>

        <!-- Error message -->
        <div v-if="job.status === JobStatus.FAILED && job.errorMessage" class="alert alert-error mt-3">
          {{ job.errorMessage }}
        </div>

        <!-- Completed stats -->
        <div v-if="job.status === JobStatus.COMPLETED" class="job-stats">
          <span class="stat-chip">
            📊 {{ job.tablesProcessed }} tables
          </span>
          <span class="stat-chip">
            📝 {{ job.rowsProcessed.toLocaleString() }} rows
          </span>
          <span v-if="job.completedAt" class="stat-chip">
            ⏱ {{ duration(job) }}
          </span>
        </div>

        <!-- Logs section -->
        <div v-if="expandedJobId === job.id" class="logs-section">
          <div v-if="loadingLogs === job.id" class="flex items-center gap-2 py-3">
            <span class="spinner" />
            <span class="text-sm text-gray-500">Loading logs…</span>
          </div>
          <div v-else-if="!jobLogs[job.id]?.length" class="text-sm text-gray-400 py-3">
            No logs available.
          </div>
          <div v-else class="code-block">
            <div v-for="log in jobLogs[job.id]" :key="log.id" class="log-line">
              <span class="log-time">{{ new Date(log.timestamp).toLocaleTimeString() }}</span>
              <span :class="logLevelClass(log.level)">{{ log.level }}</span>
              <span class="log-msg">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Modal -->
    <AppModal v-if="showCreateModal" title="Run New Masking Job" @close="showCreateModal = false">
      <form @submit.prevent="submitCreate">
        <div v-if="createError" class="alert alert-error">{{ createError }}</div>

        <div v-if="connections.length < 2" class="alert alert-warning">
          You need at least 2 connections (source and target) to run a job.
        </div>

        <div class="form-group">
          <label class="form-label">Job Name *</label>
          <input
            v-model="createForm.name"
            type="text"
            class="form-control"
            placeholder="e.g. Mask Production → Staging"
            required
          />
        </div>
        <div class="form-group">
          <label class="form-label">Source Connection *</label>
          <select v-model.number="createForm.sourceConnectionId" class="form-control">
            <option v-for="c in connections" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Target Connection *</label>
          <select v-model.number="createForm.targetConnectionId" class="form-control">
            <option v-for="c in connections" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
      </form>
      <template #footer>
        <button class="btn btn-secondary" @click="showCreateModal = false">Cancel</button>
        <button
          class="btn btn-primary"
          :disabled="creating || connections.length < 2"
          @click="submitCreate"
        >
          <span v-if="creating" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          Run Job
        </button>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.jobs-list { display: flex; flex-direction: column; gap: 1rem; }

.job-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.job-title-row { flex: 1; min-width: 0; }

.progress-section { margin-top: 1rem; }
.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 0.375rem;
}
.progress-bar {
  height: 8px;
  background: #e5e7eb;
  border-radius: 9999px;
  overflow: hidden;
}
.progress-fill {
  height: 100%;
  background: #3b82f6;
  border-radius: 9999px;
  transition: width 0.4s ease;
}

.job-stats {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: 0.75rem;
}
.stat-chip {
  font-size: 0.8rem;
  background: #f3f4f6;
  color: #374151;
  padding: 0.25rem 0.625rem;
  border-radius: 9999px;
  font-weight: 500;
}

.logs-section {
  margin-top: 1rem;
  border-top: 1px solid #e5e7eb;
  padding-top: 1rem;
}
</style>
