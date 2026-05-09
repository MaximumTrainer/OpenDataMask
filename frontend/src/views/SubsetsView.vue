<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import AppModal from '@/components/AppModal.vue'
import * as subsetsApi from '@/api/subsets'
import { SubsetLimitType } from '@/api/subsets'
import type { SubsetTableConfig, SubsetTableConfigRequest } from '@/api/subsets'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

const configs = ref<SubsetTableConfig[]>([])
const loading = ref(false)
const error = ref('')
const showModal = ref(false)
const editingConfig = ref<SubsetTableConfig | null>(null)
const saving = ref(false)
const formError = ref('')
const deleting = ref<number | null>(null)

const defaultForm = (): SubsetTableConfigRequest => ({
  tableName: '',
  limitType: SubsetLimitType.PERCENTAGE,
  limitValue: 10,
  isTargetTable: false,
  isLookupTable: false
})

const form = ref<SubsetTableConfigRequest>(defaultForm())

async function fetchConfigs() {
  loading.value = true
  error.value = ''
  try {
    configs.value = await subsetsApi.listSubsetConfigs(workspaceId.value)
  } catch {
    error.value = 'Failed to load subset configurations.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchConfigs)

function openCreate() {
  editingConfig.value = null
  form.value = defaultForm()
  formError.value = ''
  showModal.value = true
}

function openEdit(cfg: SubsetTableConfig) {
  editingConfig.value = cfg
  form.value = {
    tableName: cfg.tableName,
    limitType: cfg.limitType,
    limitValue: cfg.limitValue,
    isTargetTable: cfg.isTargetTable,
    isLookupTable: cfg.isLookupTable
  }
  formError.value = ''
  showModal.value = true
}

async function handleSave() {
  if (!form.value.tableName.trim()) {
    formError.value = 'Table name is required.'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    if (editingConfig.value) {
      const updated = await subsetsApi.updateSubsetConfig(workspaceId.value, editingConfig.value.id, form.value)
      const idx = configs.value.findIndex((c) => c.id === editingConfig.value!.id)
      if (idx !== -1) configs.value[idx] = updated
    } else {
      const created = await subsetsApi.createSubsetConfig(workspaceId.value, form.value)
      configs.value.push(created)
    }
    showModal.value = false
  } catch {
    formError.value = 'Failed to save configuration.'
  } finally {
    saving.value = false
  }
}

async function handleDelete(cfg: SubsetTableConfig) {
  if (!confirm(`Delete subset configuration for "${cfg.tableName}"?`)) return
  deleting.value = cfg.id
  try {
    await subsetsApi.deleteSubsetConfig(workspaceId.value, cfg.id)
    configs.value = configs.value.filter((c) => c.id !== cfg.id)
  } catch {
    alert('Failed to delete configuration.')
  } finally {
    deleting.value = null
  }
}

function limitLabel(cfg: SubsetTableConfig) {
  if (cfg.limitType === SubsetLimitType.ALL) return 'All rows'
  if (cfg.limitType === SubsetLimitType.PERCENTAGE) return `${cfg.limitValue}%`
  return `${cfg.limitValue.toLocaleString()} rows`
}
</script>

<template>
  <div class="page">
    <div class="breadcrumb">
      <RouterLink to="/workspaces">Workspaces</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <RouterLink :to="`/workspaces/${workspaceId}`">Workspace</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <span>Subset Configuration</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Subset Configuration</h1>
        <p class="text-gray-500 text-sm mt-1">Control how much data is copied per table during masking jobs</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">+ Add Table Config</button>
    </div>

    <div v-if="loading" class="loading-overlay"><span class="spinner spinner-lg" /></div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="configs.length === 0" class="empty-state">
      <div class="empty-state-icon">✂️</div>
      <h3>No subset configurations</h3>
      <p>By default all rows are copied. Add table configurations to limit data volume.</p>
      <button class="btn btn-primary mt-4" @click="openCreate">+ Add Table Config</button>
    </div>

    <div v-else class="card">
      <table class="data-table">
        <thead>
          <tr>
            <th>Table</th>
            <th>Limit</th>
            <th>Type</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="cfg in configs" :key="cfg.id">
            <td class="font-mono">{{ cfg.tableName }}</td>
            <td>{{ limitLabel(cfg) }}</td>
            <td>
              <span v-if="cfg.isTargetTable" class="badge badge-blue">Target</span>
              <span v-else-if="cfg.isLookupTable" class="badge badge-gray">Lookup</span>
              <span v-else class="badge badge-green">Source</span>
            </td>
            <td>
              <div class="flex gap-2">
                <button class="btn btn-secondary btn-sm" @click="openEdit(cfg)">Edit</button>
                <button
                  class="btn btn-danger btn-sm"
                  :disabled="deleting === cfg.id"
                  @click="handleDelete(cfg)"
                >Delete</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <AppModal v-if="showModal" :title="editingConfig ? 'Edit Subset Config' : 'Add Subset Config'" @close="showModal = false">
      <form @submit.prevent="handleSave">
        <div v-if="formError" class="alert alert-error">{{ formError }}</div>

        <div class="form-group">
          <label class="form-label">Table Name *</label>
          <input v-model="form.tableName" :disabled="!!editingConfig" type="text" class="form-control" placeholder="e.g. users" required />
        </div>

        <div class="form-group">
          <label class="form-label">Limit Type</label>
          <select v-model="form.limitType" class="form-control">
            <option :value="SubsetLimitType.ALL">All rows</option>
            <option :value="SubsetLimitType.PERCENTAGE">Percentage</option>
            <option :value="SubsetLimitType.ROW_COUNT">Row count</option>
          </select>
        </div>

        <div v-if="form.limitType !== SubsetLimitType.ALL" class="form-group">
          <label class="form-label">
            {{ form.limitType === SubsetLimitType.PERCENTAGE ? 'Percentage (1–100)' : 'Row Count' }}
          </label>
          <input v-model.number="form.limitValue" type="number" min="1" class="form-control" />
        </div>

        <div class="form-group">
          <label class="checkbox-label">
            <input v-model="form.isTargetTable" type="checkbox" />
            This is a target/seed table (subset traversal starts here)
          </label>
        </div>

        <div class="form-group">
          <label class="checkbox-label">
            <input v-model="form.isLookupTable" type="checkbox" />
            This is a lookup/reference table (always copy all rows)
          </label>
        </div>
      </form>
      <template #footer>
        <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
        <button class="btn btn-primary" :disabled="saving" @click="handleSave">
          <span v-if="saving" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          {{ editingConfig ? 'Save Changes' : 'Add Config' }}
        </button>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 0.75rem 1rem; text-align: left; border-bottom: 1px solid #e5e7eb; }
.data-table th { font-weight: 600; color: #6b7280; font-size: 0.85rem; text-transform: uppercase; }
.badge { padding: 0.2rem 0.6rem; border-radius: 9999px; font-size: 0.75rem; font-weight: 500; }
.badge-blue { background: #dbeafe; color: #1d4ed8; }
.badge-green { background: #dcfce7; color: #15803d; }
.badge-gray { background: #f3f4f6; color: #4b5563; }
.checkbox-label { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; }
</style>
