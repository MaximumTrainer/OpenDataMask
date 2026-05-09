<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import * as sensitivityApi from '@/api/sensitivityScan'
import { SensitivityType, ConfidenceLevel } from '@/api/sensitivityScan'
import type { ColumnSensitivity, SensitivityScanLog } from '@/api/sensitivityScan'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

const scanStatus = ref<SensitivityScanLog | null>(null)
const results = ref<ColumnSensitivity[]>([])
const loading = ref(false)
const scanning = ref(false)
const error = ref('')
const filterTable = ref('')
const filterSensitivity = ref('')
const updatingColumn = ref<string | null>(null)

async function loadResults() {
  loading.value = true
  error.value = ''
  try {
    const [status, scanResults] = await Promise.all([
      sensitivityApi.getSensitivityScanStatus(workspaceId.value),
      sensitivityApi.getSensitivityScanResults(workspaceId.value)
    ])
    scanStatus.value = status
    results.value = scanResults
  } catch {
    error.value = 'Failed to load scan data.'
  } finally {
    loading.value = false
  }
}

onMounted(loadResults)

async function runScan() {
  scanning.value = true
  error.value = ''
  try {
    scanStatus.value = await sensitivityApi.runSensitivityScan(workspaceId.value)
    let attempts = 0
    const poll = setInterval(async () => {
      try {
        const status = await sensitivityApi.getSensitivityScanStatus(workspaceId.value)
        if (status) scanStatus.value = status
        const done = !status || status.status === 'COMPLETED' || status.status === 'FAILED' || ++attempts > 60
        if (done) {
          clearInterval(poll)
          scanning.value = false
          if (status?.status === 'COMPLETED') {
            results.value = await sensitivityApi.getSensitivityScanResults(workspaceId.value)
          }
        }
      } catch {
        clearInterval(poll)
        scanning.value = false
      }
    }, 3000)
  } catch {
    error.value = 'Failed to start scan.'
    scanning.value = false
  }
}

async function updateColumnSensitivity(result: ColumnSensitivity, newSensitivity: string) {
  const key = `${result.tableName}.${result.columnName}`
  updatingColumn.value = key
  try {
    const updated = await sensitivityApi.updateColumnSensitivity(
      workspaceId.value,
      result.tableName,
      result.columnName,
      {
        isSensitive: newSensitivity !== 'NOT_SENSITIVE',
        sensitivityType: newSensitivity as SensitivityType,
        confidenceLevel: ConfidenceLevel.FULL
      }
    )
    const idx = results.value.findIndex((r) => r.tableName === result.tableName && r.columnName === result.columnName)
    if (idx !== -1) results.value[idx] = updated
  } catch {
    alert('Failed to update sensitivity.')
  } finally {
    updatingColumn.value = null
  }
}

const uniqueTables = computed(() => [...new Set(results.value.map((r) => r.tableName))].sort())
const uniqueSensitivities = computed(() => [...new Set(results.value.map((r) => r.sensitivityType))].sort())

const filteredResults = computed(() => results.value.filter((r) => {
  if (filterTable.value && r.tableName !== filterTable.value) return false
  if (filterSensitivity.value && r.sensitivityType !== filterSensitivity.value) return false
  return true
}))

function confidenceBadge(level: ConfidenceLevel) {
  if (level === ConfidenceLevel.HIGH || level === ConfidenceLevel.FULL) return 'badge-green'
  if (level === ConfidenceLevel.MEDIUM) return 'badge-orange'
  return 'badge-gray'
}

function sensitivityBadge(type: SensitivityType) {
  if (!type || type === SensitivityType.UNKNOWN) return 'badge-gray'
  if ([SensitivityType.EMAIL, SensitivityType.SSN, SensitivityType.CREDIT_CARD, SensitivityType.PASSWORD].includes(type)) return 'badge-red'
  return 'badge-orange'
}

function formatDate(d: string | null) {
  if (!d) return '—'
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
      <span>Sensitivity Scan</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Sensitivity Scan</h1>
        <p class="text-gray-500 text-sm mt-1">Detect PII and sensitive columns across your dataset</p>
      </div>
      <button class="btn btn-primary" :disabled="scanning" @click="runScan">
        <span v-if="scanning" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
        {{ scanning ? 'Scanning…' : '🔍 Run Scan' }}
      </button>
    </div>

    <!-- Scan status bar -->
    <div v-if="scanStatus" class="status-bar mb-4">
      <div class="flex items-center gap-3">
        <span v-if="scanning" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
        <span class="text-sm font-medium">
          {{ scanning ? 'Scanning…' : `Last scan: ${formatDate(scanStatus.completedAt)}` }}
        </span>
        <span v-if="scanStatus.completedAt != null" class="text-sm text-gray-500">
          — {{ scanStatus.sensitiveColumnsFound }} sensitive of {{ scanStatus.columnsScanned }} columns scanned
        </span>
      </div>
    </div>

    <div v-if="loading" class="loading-overlay"><span class="spinner spinner-lg" /></div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <template v-else>
      <!-- Filters -->
      <div v-if="results.length > 0" class="filters-bar mb-4">
        <select v-model="filterTable" class="form-control form-control-sm">
          <option value="">All tables</option>
          <option v-for="t in uniqueTables" :key="t" :value="t">{{ t }}</option>
        </select>
        <select v-model="filterSensitivity" class="form-control form-control-sm">
          <option value="">All sensitivity types</option>
          <option v-for="s in uniqueSensitivities" :key="s" :value="s">{{ s.replace(/_/g, ' ') }}</option>
        </select>
        <span class="text-sm text-gray-500">{{ filteredResults.length }} results</span>
      </div>

      <div v-if="results.length === 0" class="empty-state">
        <div class="empty-state-icon">🔍</div>
        <h3>No scan results yet</h3>
        <p>Click "Run Scan" to analyse your workspace for sensitive data.</p>
      </div>

      <div v-else class="card">
        <table class="data-table">
          <thead>
            <tr>
              <th>Table</th>
              <th>Column</th>
              <th>Sensitivity</th>
              <th>Confidence</th>
              <th>Recommended Generator</th>
              <th>Override</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in filteredResults" :key="`${r.tableName}.${r.columnName}`">
              <td class="font-mono">{{ r.tableName }}</td>
              <td class="font-mono">{{ r.columnName }}</td>
              <td>
                <span :class="`badge ${sensitivityBadge(r.sensitivityType)}`">
                  {{ r.sensitivityType.replace(/_/g, ' ') }}
                </span>
              </td>
              <td>
                <span :class="`badge ${confidenceBadge(r.confidenceLevel)}`">
                  {{ r.confidenceLevel }}
                </span>
              </td>
              <td class="font-mono text-sm">{{ r.recommendedGeneratorType ?? '—' }}</td>
              <td>
                <select
                  class="form-control form-control-sm"
                  :value="r.sensitivityType"
                  :disabled="updatingColumn === `${r.tableName}.${r.columnName}`"
                  @change="(e) => updateColumnSensitivity(r, (e.target as HTMLSelectElement).value)"
                >
                  <option v-for="s in SensitivityType" :key="s" :value="s">{{ s.replace(/_/g, ' ') }}</option>
                </select>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
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
.badge-gray { background: #f3f4f6; color: #4b5563; }
.status-bar { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 0.5rem; padding: 0.75rem 1rem; }
.filters-bar { display: flex; align-items: center; gap: 1rem; }
.form-control-sm { padding: 0.375rem 0.75rem; font-size: 0.875rem; height: auto; }
</style>
