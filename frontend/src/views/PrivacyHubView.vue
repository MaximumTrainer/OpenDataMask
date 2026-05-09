<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import * as privacyApi from '@/api/privacy'
import type { PrivacyHubSummary, PrivacyRecommendation } from '@/api/privacy'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

const summary = ref<PrivacyHubSummary | null>(null)
const recommendations = ref<PrivacyRecommendation[]>([])
const loading = ref(false)
const error = ref('')
const applyingRecs = ref(false)
const applyResult = ref<string | null>(null)
const downloadingReport = ref(false)
const activeTab = ref<'overview' | 'recommendations'>('overview')

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    const [sum, recs] = await Promise.all([
      privacyApi.getPrivacyHubSummary(workspaceId.value),
      privacyApi.getPrivacyRecommendations(workspaceId.value)
    ])
    summary.value = sum
    recommendations.value = recs
  } catch {
    error.value = 'Failed to load privacy data.'
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

async function applyRecommendations() {
  if (!confirm('Apply all recommendations? This will update masking configuration for unprotected sensitive columns.')) return
  applyingRecs.value = true
  applyResult.value = null
  try {
    const result = await privacyApi.applyPrivacyRecommendations(workspaceId.value)
    applyResult.value = `✓ Applied ${result.applied} recommendation${result.applied !== 1 ? 's' : ''}.`
    await loadAll()
  } catch {
    applyResult.value = '✗ Failed to apply recommendations.'
  } finally {
    applyingRecs.value = false
  }
}

async function downloadReport() {
  downloadingReport.value = true
  try {
    const blob = await privacyApi.downloadPrivacyReport(workspaceId.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `privacy-report-workspace-${workspaceId.value}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    alert('Failed to download report.')
  } finally {
    downloadingReport.value = false
  }
}

function confidenceBadgeClass(level: string) {
  return level === 'HIGH' ? 'badge-green' : level === 'MEDIUM' ? 'badge-orange' : 'badge-gray'
}

function protectionPercent(sum: PrivacyHubSummary) {
  const total = sum.atRiskCount + sum.protectedCount + sum.notSensitiveCount
  return total > 0 ? Math.round((sum.protectedCount / (total - sum.notSensitiveCount || 1)) * 100) : 0
}
</script>

<template>
  <div class="page">
    <div class="breadcrumb">
      <RouterLink to="/workspaces">Workspaces</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <RouterLink :to="`/workspaces/${workspaceId}`">Workspace</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <span>Privacy Hub</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Privacy Hub</h1>
        <p class="text-gray-500 text-sm mt-1">Monitor data protection coverage and act on recommendations</p>
      </div>
      <div class="flex gap-2">
        <button class="btn btn-secondary" :disabled="downloadingReport" @click="downloadReport">
          <span v-if="downloadingReport" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          📄 Download Report
        </button>
        <button
          v-if="recommendations.length > 0"
          class="btn btn-primary"
          :disabled="applyingRecs"
          @click="applyRecommendations"
        >
          <span v-if="applyingRecs" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          ✓ Apply All Recommendations
        </button>
      </div>
    </div>

    <div v-if="applyResult" :class="`alert ${applyResult.startsWith('✓') ? 'alert-success' : 'alert-error'} mb-4`">
      {{ applyResult }}
    </div>

    <div v-if="loading" class="loading-overlay"><span class="spinner spinner-lg" /></div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <template v-else-if="summary">
      <!-- Summary Cards -->
      <div class="grid grid-cols-3 mb-6">
        <div class="card stat-card">
          <div class="stat-icon stat-red">🚨</div>
          <div class="stat-value">{{ summary.atRiskCount }}</div>
          <div class="stat-label">At Risk</div>
        </div>
        <div class="card stat-card">
          <div class="stat-icon stat-green">🛡️</div>
          <div class="stat-value">{{ summary.protectedCount }}</div>
          <div class="stat-label">Protected</div>
        </div>
        <div class="card stat-card">
          <div class="stat-icon stat-blue">💡</div>
          <div class="stat-value">{{ summary.recommendationsCount }}</div>
          <div class="stat-label">Recommendations</div>
        </div>
      </div>

      <!-- Tabs -->
      <div class="tabs mb-4">
        <button class="tab" :class="{ active: activeTab === 'overview' }" @click="activeTab = 'overview'">
          📊 Table Overview
        </button>
        <button class="tab" :class="{ active: activeTab === 'recommendations' }" @click="activeTab = 'recommendations'">
          💡 Recommendations
          <span v-if="recommendations.length > 0" class="tab-badge">{{ recommendations.length }}</span>
        </button>
      </div>

      <!-- Table Overview -->
      <div v-if="activeTab === 'overview'" class="card">
        <div v-if="summary.tables.length === 0" class="empty-state">
          <div class="empty-state-icon">📊</div>
          <h3>No data yet</h3>
          <p>Run a sensitivity scan first to populate privacy metrics.</p>
        </div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>Table</th>
              <th>At Risk</th>
              <th>Protected</th>
              <th>Not Sensitive</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="t in summary.tables" :key="t.name">
              <td class="font-mono">{{ t.name }}</td>
              <td><span class="badge badge-red">{{ t.atRisk }}</span></td>
              <td><span class="badge badge-green">{{ t.protected }}</span></td>
              <td><span class="badge badge-gray">{{ t.notSensitive }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Recommendations -->
      <div v-if="activeTab === 'recommendations'" class="card">
        <div v-if="recommendations.length === 0" class="empty-state">
          <div class="empty-state-icon">✅</div>
          <h3>No recommendations</h3>
          <p>All sensitive columns appear to be protected.</p>
        </div>
        <table v-else class="data-table">
          <thead>
            <tr>
              <th>Table</th>
              <th>Column</th>
              <th>Sensitivity</th>
              <th>Confidence</th>
              <th>Recommended Generator</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(rec, i) in recommendations" :key="i">
              <td class="font-mono">{{ rec.tableName }}</td>
              <td class="font-mono">{{ rec.columnName }}</td>
              <td><span class="badge badge-orange">{{ rec.sensitivityType.replace(/_/g, ' ') }}</span></td>
              <td><span :class="`badge ${confidenceBadgeClass(rec.confidenceLevel)}`">{{ rec.confidenceLevel }}</span></td>
              <td class="font-mono text-sm">{{ rec.recommendedGenerator }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<style scoped>
.grid-cols-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 1rem; }
.stat-card { display: flex; flex-direction: column; align-items: center; padding: 1.5rem; text-align: center; }
.stat-icon { font-size: 2rem; margin-bottom: 0.5rem; }
.stat-value { font-size: 2rem; font-weight: 700; }
.stat-label { color: #6b7280; font-size: 0.875rem; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e5e7eb; }
.data-table th { font-weight: 600; color: #6b7280; font-size: 0.85rem; text-transform: uppercase; }
.badge { padding: 0.2rem 0.6rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 500; }
.badge-red { background: #fee2e2; color: #991b1b; }
.badge-green { background: #dcfce7; color: #15803d; }
.badge-orange { background: #ffedd5; color: #9a3412; }
.badge-blue { background: #dbeafe; color: #1d4ed8; }
.badge-gray { background: #f3f4f6; color: #4b5563; }
.tab-badge { background: #ef4444; color: white; border-radius: 9999px; font-size: 0.7rem; padding: 0.1rem 0.4rem; margin-left: 0.25rem; }
.alert-success { background: #dcfce7; color: #15803d; border: 1px solid #bbf7d0; border-radius: 0.5rem; padding: 0.75rem 1rem; }
</style>
