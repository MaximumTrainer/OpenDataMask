<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import * as schemaChangesApi from '@/api/schemaChanges'
import { SchemaChangeStatus } from '@/api/schemaChanges'
import type { SchemaChange } from '@/api/schemaChanges'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

const changes = ref<SchemaChange[]>([])
const loading = ref(false)
const detecting = ref(false)
const error = ref('')
const resolving = ref<number | null>(null)
const dismissing = ref<number | null>(null)
const batchProcessing = ref(false)
const lastDetected = ref<string | null>(null)

async function fetchChanges() {
  loading.value = true
  error.value = ''
  try {
    const response = await schemaChangesApi.listSchemaChanges(workspaceId.value)
    changes.value = [...response.exposing, ...response.notifications]
  } catch {
    error.value = 'Failed to load schema changes.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchChanges)

async function detectChanges() {
  detecting.value = true
  error.value = ''
  try {
    const detected = await schemaChangesApi.detectSchemaChanges(workspaceId.value)
    changes.value = detected
    lastDetected.value = new Date().toLocaleTimeString()
  } catch {
    error.value = 'Schema detection failed.'
  } finally {
    detecting.value = false
  }
}

async function resolveChange(change: SchemaChange) {
  resolving.value = change.id
  try {
    await schemaChangesApi.resolveSchemaChange(workspaceId.value, change.id)
    const idx = changes.value.findIndex((c) => c.id === change.id)
    if (idx !== -1) changes.value[idx] = { ...changes.value[idx], status: SchemaChangeStatus.RESOLVED }
  } catch {
    alert('Failed to resolve change.')
  } finally {
    resolving.value = null
  }
}

async function dismissChange(change: SchemaChange) {
  dismissing.value = change.id
  try {
    await schemaChangesApi.dismissSchemaChange(workspaceId.value, change.id)
    const idx = changes.value.findIndex((c) => c.id === change.id)
    if (idx !== -1) changes.value[idx] = { ...changes.value[idx], status: SchemaChangeStatus.DISMISSED }
  } catch {
    alert('Failed to dismiss change.')
  } finally {
    dismissing.value = null
  }
}

async function resolveAll() {
  const pending = changes.value.filter((c) => c.status === SchemaChangeStatus.UNRESOLVED)
  if (pending.length === 0) return
  if (!confirm(`Resolve ${pending.length} pending change${pending.length !== 1 ? 's' : ''}?`)) return
  batchProcessing.value = true
  try {
    await schemaChangesApi.resolveAllSchemaChanges(workspaceId.value)
    changes.value = changes.value.map((c) =>
      c.status === SchemaChangeStatus.UNRESOLVED ? { ...c, status: SchemaChangeStatus.RESOLVED } : c
    )
  } catch {
    alert('Some changes could not be resolved.')
  } finally {
    batchProcessing.value = false
  }
}

async function dismissAll() {
  const pending = changes.value.filter((c) => c.status === SchemaChangeStatus.UNRESOLVED)
  if (pending.length === 0) return
  if (!confirm(`Dismiss ${pending.length} pending change${pending.length !== 1 ? 's' : ''}?`)) return
  batchProcessing.value = true
  try {
    await schemaChangesApi.dismissAllSchemaChanges(workspaceId.value)
    changes.value = changes.value.map((c) =>
      c.status === SchemaChangeStatus.UNRESOLVED ? { ...c, status: SchemaChangeStatus.DISMISSED } : c
    )
  } catch {
    alert('Some changes could not be dismissed.')
  } finally {
    batchProcessing.value = false
  }
}

const pendingCount = computed(() => changes.value.filter((c) => c.status === SchemaChangeStatus.UNRESOLVED).length)

function changeTypeBadgeClass(type: string) {
  if (type === 'NEW_TABLE' || type === 'NEW_COLUMN') return 'badge-green'
  if (type === 'DROPPED_TABLE' || type === 'DROPPED_COLUMN') return 'badge-red'
  return 'badge-orange'
}

function statusBadgeClass(status: SchemaChangeStatus) {
  if (status === SchemaChangeStatus.RESOLVED) return 'badge-green'
  if (status === SchemaChangeStatus.DISMISSED) return 'badge-gray'
  return 'badge-orange'
}

function changeDetails(c: SchemaChange): string {
  const parts: string[] = []
  if (c.oldValue) parts.push(`was: ${c.oldValue}`)
  if (c.newValue) parts.push(`now: ${c.newValue}`)
  return parts.join(' → ') || '—'
}

function formatDate(d: string) {
  return new Date(d).toLocaleString()
}
</script>

<template>
  <div class="page">
    <div class="breadcrumb">
      <RouterLink to="/workspaces">Workspaces</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <RouterLink :to="`/workspaces/${workspaceId}`">Workspace</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <span>Schema Changes</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Schema Changes</h1>
        <p class="text-gray-500 text-sm mt-1">Track and resolve differences between source and destination schemas</p>
      </div>
      <div class="flex gap-2">
        <button
          v-if="pendingCount > 0"
          class="btn btn-secondary"
          :disabled="batchProcessing"
          @click="dismissAll"
        >Dismiss All</button>
        <button
          v-if="pendingCount > 0"
          class="btn btn-secondary"
          :disabled="batchProcessing"
          @click="resolveAll"
        >Resolve All</button>
        <button class="btn btn-primary" :disabled="detecting" @click="detectChanges">
          <span v-if="detecting" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          🔍 Detect Changes
        </button>
      </div>
    </div>

    <div v-if="lastDetected" class="alert alert-info mb-4">
      Last detection ran at {{ lastDetected }}
    </div>

    <div v-if="loading" class="loading-overlay"><span class="spinner spinner-lg" /></div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="changes.length === 0" class="empty-state">
      <div class="empty-state-icon">✅</div>
      <h3>No schema changes detected</h3>
      <p>Click "Detect Changes" to compare source and destination schemas.</p>
    </div>

    <div v-else class="card">
      <div class="table-toolbar" v-if="pendingCount > 0">
        <span class="text-sm text-gray-500">{{ pendingCount }} pending change{{ pendingCount !== 1 ? 's' : '' }}</span>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>Detected At</th>
            <th>Table</th>
            <th>Column</th>
            <th>Change Type</th>
            <th>Details</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in changes" :key="c.id">
            <td class="text-sm text-gray-500">{{ formatDate(c.detectedAt) }}</td>
            <td class="font-mono">{{ c.tableName }}</td>
            <td class="font-mono">{{ c.columnName ?? '—' }}</td>
            <td><span :class="`badge ${changeTypeBadgeClass(c.changeType)}`">{{ c.changeType.replace(/_/g, ' ') }}</span></td>
            <td class="text-sm text-gray-600 max-w-xs truncate" :title="changeDetails(c)">{{ changeDetails(c) }}</td>
            <td><span :class="`badge ${statusBadgeClass(c.status)}`">{{ c.status }}</span></td>
            <td>
              <div v-if="c.status === SchemaChangeStatus.UNRESOLVED" class="flex gap-2">
                <button
                  class="btn btn-secondary btn-sm"
                  :disabled="resolving === c.id"
                  @click="resolveChange(c)"
                >Resolve</button>
                <button
                  class="btn btn-secondary btn-sm"
                  :disabled="dismissing === c.id"
                  @click="dismissChange(c)"
                >Dismiss</button>
              </div>
              <span v-else class="text-sm text-gray-400">—</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e5e7eb; }
.data-table th { font-weight: 600; color: #6b7280; font-size: 0.85rem; text-transform: uppercase; }
.badge { padding: 0.2rem 0.6rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 500; }
.badge-red { background: #fee2e2; color: #991b1b; }
.badge-green { background: #dcfce7; color: #15803d; }
.badge-orange { background: #ffedd5; color: #9a3412; }
.badge-blue { background: #dbeafe; color: #1d4ed8; }
.badge-gray { background: #f3f4f6; color: #4b5563; }
.table-toolbar { padding: 0.75rem 1rem; border-bottom: 1px solid #e5e7eb; }
.alert-info { background: #eff6ff; color: #1d4ed8; border: 1px solid #bfdbfe; border-radius: 0.5rem; padding: 0.75rem 1rem; }
.max-w-xs { max-width: 16rem; }
.truncate { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
