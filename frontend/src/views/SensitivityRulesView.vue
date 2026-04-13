<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useWorkspaceStore } from '@/store/workspace'
import {
  listSensitivityRules,
  createSensitivityRule,
  updateSensitivityRule,
  deleteSensitivityRule,
  previewSensitivityRule
} from '@/api/sensitivityRules'
import { listSystemPresets, listWorkspacePresets } from '@/api/presets'
import type { GeneratorPresetResponse } from '@/api/presets'
import type {
  CustomSensitivityRule,
  CustomSensitivityRuleRequest,
  CustomRulePreviewResult
} from '@/types'
import { GenericDataType, MatcherType } from '@/types'

const workspaceStore = useWorkspaceStore()

const rules = ref<CustomSensitivityRule[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

// ── Drawer state ──────────────────────────────────────────────────────────
const drawerOpen = ref(false)
const editingRule = ref<CustomSensitivityRule | null>(null)
const saving = ref(false)
const saveError = ref<string | null>(null)

// ── Form state ─────────────────────────────────────────────────────────────
const form = ref<CustomSensitivityRuleRequest>({
  name: '',
  description: null,
  dataTypeFilter: GenericDataType.ANY,
  matchers: [],
  linkedPresetId: null,
  isActive: true
})

// ── Preview state ──────────────────────────────────────────────────────────
const previewWorkspaceId = ref<number | null>(null)
const previewResults = ref<CustomRulePreviewResult[]>([])
const previewing = ref(false)
const previewError = ref<string | null>(null)

// ── Presets ────────────────────────────────────────────────────────────────
const allPresets = ref<GeneratorPresetResponse[]>([])

onMounted(async () => {
  await Promise.all([fetchRules(), fetchSystemPresets(), workspaceStore.fetchWorkspaces()])
})

async function fetchRules() {
  loading.value = true
  error.value = null
  try {
    rules.value = await listSensitivityRules()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Failed to load sensitivity rules'
  } finally {
    loading.value = false
  }
}

async function fetchSystemPresets() {
  try {
    allPresets.value = await listSystemPresets()
  } catch {
    // non-critical
  }
}

async function loadWorkspacePresets(workspaceId: number) {
  try {
    const wsPresets = await listWorkspacePresets(workspaceId)
    // Merge with system presets, deduplicating by id
    const systemIds = new Set(allPresets.value.filter((p) => p.isSystem).map((p) => p.id))
    const systemPresets = allPresets.value.filter((p) => p.isSystem)
    allPresets.value = [...systemPresets, ...wsPresets.filter((p) => !systemIds.has(p.id))]
  } catch {
    // non-critical
  }
}

function resetPreviewState() {
  previewWorkspaceId.value = null
  previewResults.value = []
  previewing.value = false
  previewError.value = null
}

function openCreateDrawer() {
  editingRule.value = null
  form.value = {
    name: '',
    description: null,
    dataTypeFilter: GenericDataType.ANY,
    matchers: [],
    linkedPresetId: null,
    isActive: true
  }
  resetPreviewState()
  saveError.value = null
  drawerOpen.value = true
}

function openEditDrawer(rule: CustomSensitivityRule) {
  editingRule.value = rule
  form.value = {
    name: rule.name,
    description: rule.description,
    dataTypeFilter: rule.dataTypeFilter,
    matchers: rule.matchers.map((m) => ({ ...m })),
    linkedPresetId: rule.linkedPresetId,
    isActive: rule.isActive
  }
  resetPreviewState()
  saveError.value = null
  drawerOpen.value = true
}

function closeDrawer() {
  drawerOpen.value = false
}

function addMatcher() {
  form.value.matchers.push({ matcherType: MatcherType.CONTAINS, value: '', caseSensitive: false })
}

function removeMatcher(index: number) {
  form.value.matchers.splice(index, 1)
}

async function saveRule() {
  saving.value = true
  saveError.value = null
  try {
    if (editingRule.value) {
      const updated = await updateSensitivityRule(editingRule.value.id, form.value)
      const idx = rules.value.findIndex((r) => r.id === updated.id)
      if (idx !== -1) rules.value[idx] = updated
    } else {
      const created = await createSensitivityRule(form.value)
      rules.value.push(created)
    }
    closeDrawer()
  } catch (e: unknown) {
    saveError.value = e instanceof Error ? e.message : 'Failed to save rule'
  } finally {
    saving.value = false
  }
}

async function deleteRule(rule: CustomSensitivityRule) {
  if (!confirm(`Delete rule "${rule.name}"? This cannot be undone.`)) return
  try {
    await deleteSensitivityRule(rule.id)
    rules.value = rules.value.filter((r) => r.id !== rule.id)
  } catch (e: unknown) {
    alert(e instanceof Error ? e.message : 'Failed to delete rule')
  }
}

async function onPreviewWorkspaceChange(workspaceId: number | null) {
  previewResults.value = []
  previewError.value = null
  if (workspaceId) await loadWorkspacePresets(workspaceId)
}

async function runPreview() {
  if (!previewWorkspaceId.value) {
    previewError.value = 'Please select a workspace to preview against.'
    return
  }
  previewing.value = true
  previewError.value = null
  previewResults.value = []
  try {
    previewResults.value = await previewSensitivityRule({
      workspaceId: previewWorkspaceId.value,
      dataTypeFilter: form.value.dataTypeFilter,
      matchers: form.value.matchers
    })
  } catch (e: unknown) {
    previewError.value = e instanceof Error ? e.message : 'Preview failed'
  } finally {
    previewing.value = false
  }
}

function presetName(presetId: number | null): string {
  if (!presetId) return '—'
  return allPresets.value.find((p) => p.id === presetId)?.name ?? `#${presetId}`
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h1>Sensitivity Rules</h1>
        <p class="text-gray-500 text-sm mt-1">
          Define custom rules to detect sensitive columns across all workspaces.
        </p>
      </div>
      <button class="btn btn-primary" @click="openCreateDrawer">+ New Rule</button>
    </div>

    <!-- Error -->
    <div v-if="error" class="alert alert-error mb-4">{{ error }}</div>

    <!-- Loading skeleton -->
    <div v-if="loading" class="card">
      <div v-for="i in 3" :key="i" class="skeleton mb-3" style="height: 3rem" />
    </div>

    <!-- Empty state -->
    <div v-else-if="rules.length === 0" class="card text-center py-12">
      <div class="text-4xl mb-3">🔍</div>
      <h3 class="font-semibold text-lg mb-1">No custom sensitivity rules yet</h3>
      <p class="text-gray-500 text-sm mb-4">
        Create rules to automatically detect sensitive columns by name pattern and data type.
      </p>
      <button class="btn btn-primary" @click="openCreateDrawer">Create your first rule</button>
    </div>

    <!-- Rules table -->
    <div v-else class="card p-0">
      <table class="table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Data Type</th>
            <th>Matchers</th>
            <th>Linked Preset</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="rule in rules" :key="rule.id">
            <td class="font-medium">{{ rule.name }}</td>
            <td>
              <span class="badge badge-blue">{{ rule.dataTypeFilter }}</span>
            </td>
            <td>
              <span v-for="(m, i) in rule.matchers" :key="i" class="matcher-chip">
                {{ m.matcherType.toLowerCase().replace('_', ' ') }}: <em>{{ m.value }}</em>
              </span>
              <span v-if="rule.matchers.length === 0" class="text-gray-400 text-sm">—</span>
            </td>
            <td>{{ presetName(rule.linkedPresetId) }}</td>
            <td>
              <span :class="rule.isActive ? 'badge badge-green' : 'badge badge-gray'">
                {{ rule.isActive ? 'Active' : 'Inactive' }}
              </span>
            </td>
            <td>
              <div class="row-actions">
                <button class="btn btn-secondary btn-sm" @click="openEditDrawer(rule)">Edit</button>
                <button class="btn btn-danger btn-sm" @click="deleteRule(rule)">Delete</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Side Drawer -->
    <div v-if="drawerOpen" class="drawer-overlay" @click.self="closeDrawer">
      <aside class="drawer">
        <div class="drawer-header">
          <h2>{{ editingRule ? 'Edit Rule' : 'Create Rule' }}</h2>
          <button class="btn-icon" @click="closeDrawer">✕</button>
        </div>

        <div class="drawer-body">
          <!-- Rule name -->
          <div class="form-group">
            <label class="form-label">Rule Name <span class="required">*</span></label>
            <input
              v-model="form.name"
              class="form-input"
              placeholder="e.g. Internal_ID"
            />
            <p class="form-hint">This name appears as the "Sensitivity Type" in the Privacy Hub.</p>
          </div>

          <!-- Description -->
          <div class="form-group">
            <label class="form-label">Description</label>
            <textarea
              v-model="form.description"
              class="form-input"
              rows="2"
              placeholder="Optional description"
            />
          </div>

          <!-- Data type filter -->
          <div class="form-group">
            <label class="form-label">Generic Data Type Filter</label>
            <select v-model="form.dataTypeFilter" class="form-input">
              <option v-for="dt in GenericDataType" :key="dt" :value="dt">{{ dt }}</option>
            </select>
            <p class="form-hint">
              ANY matches all column types. TEXT, NUMERIC, DATE, BOOLEAN filter by inferred type.
            </p>
          </div>

          <!-- Matchers -->
          <div class="form-group">
            <label class="form-label">Column Name Matchers</label>
            <div
              v-for="(matcher, index) in form.matchers"
              :key="index"
              class="matcher-row"
            >
              <select v-model="matcher.matcherType" class="form-input form-input-sm">
                <option v-for="mt in MatcherType" :key="mt" :value="mt">
                  {{ mt.toLowerCase().replace('_', ' ') }}
                </option>
              </select>
              <input
                v-model="matcher.value"
                class="form-input form-input-sm flex-1"
                placeholder="value"
              />
              <label class="flex items-center gap-1 text-sm text-gray-600 whitespace-nowrap">
                <input type="checkbox" v-model="matcher.caseSensitive" />
                Case
              </label>
              <button class="btn-icon text-red-500" @click="removeMatcher(index)">✕</button>
            </div>
            <button class="btn btn-secondary btn-sm mt-2" @click="addMatcher">+ Add Matcher</button>
          </div>

          <!-- Linked preset -->
          <div class="form-group">
            <label class="form-label">Linked Generator Preset</label>
            <select v-model="form.linkedPresetId" class="form-input">
              <option :value="null">— None —</option>
              <option v-for="preset in allPresets" :key="preset.id" :value="preset.id">
                {{ preset.name }} ({{ preset.generatorType }})
              </option>
            </select>
            <p class="form-hint">When a column matches this rule, the linked preset is suggested.</p>
          </div>

          <!-- Active toggle -->
          <div class="form-group">
            <label class="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" v-model="form.isActive" />
              <span class="form-label mb-0">Active</span>
            </label>
          </div>

          <!-- Save error -->
          <div v-if="saveError" class="alert alert-error mb-2">{{ saveError }}</div>

          <!-- ── Preview Panel ─────────────────────────────────────────── -->
          <div class="preview-panel">
            <h3 class="preview-title">Preview</h3>
            <p class="form-hint mb-2">
              Select a workspace to see which columns this rule would match (without saving).
            </p>
            <div class="flex gap-2 mb-3">
              <select
                v-model="previewWorkspaceId"
                class="form-input flex-1"
                @change="onPreviewWorkspaceChange(previewWorkspaceId)"
              >
                <option :value="null">— Select workspace —</option>
                <option
                  v-for="ws in workspaceStore.workspaces"
                  :key="ws.id"
                  :value="ws.id"
                >
                  {{ ws.name }}
                </option>
              </select>
              <button
                class="btn btn-secondary"
                :disabled="previewing"
                @click="runPreview"
              >
                {{ previewing ? 'Running…' : 'Preview' }}
              </button>
            </div>

            <div v-if="previewError" class="alert alert-error mb-2">{{ previewError }}</div>

            <div v-if="previewResults.length > 0" class="preview-results">
              <p class="text-sm font-medium mb-2 text-green-700">
                {{ previewResults.length }} column{{ previewResults.length !== 1 ? 's' : '' }} matched
              </p>
              <div
                v-for="(result, i) in previewResults"
                :key="i"
                class="preview-result-item"
              >
                <span class="font-mono text-sm">{{ result.tableName }}.{{ result.columnName }}</span>
                <span class="badge badge-gray text-xs">{{ result.columnType }}</span>
              </div>
            </div>
            <div
              v-else-if="!previewing && previewWorkspaceId && previewResults.length === 0 && !previewError"
              class="text-sm text-gray-500 italic"
            >
              No columns matched.
            </div>
          </div>
        </div>

        <div class="drawer-footer">
          <button class="btn btn-secondary" @click="closeDrawer">Cancel</button>
          <button class="btn btn-primary" :disabled="saving" @click="saveRule">
            {{ saving ? 'Saving…' : editingRule ? 'Update Rule' : 'Create Rule' }}
          </button>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}
.table th {
  text-align: left;
  padding: 0.75rem 1rem;
  background: #f9fafb;
  color: #6b7280;
  font-weight: 500;
  border-bottom: 1px solid #e5e7eb;
}
.table td {
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f3f4f6;
  vertical-align: middle;
}
.row-actions {
  display: flex;
  gap: 0.5rem;
}
.matcher-chip {
  display: inline-block;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 0.25rem;
  padding: 0.125rem 0.375rem;
  font-size: 0.75rem;
  margin-right: 0.25rem;
}
.badge-blue  { background: #dbeafe; color: #1d4ed8; }
.badge-green { background: #dcfce7; color: #15803d; }
.badge-gray  { background: #f3f4f6; color: #6b7280; }

/* Drawer */
.drawer-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 200;
  display: flex;
  justify-content: flex-end;
}
.drawer {
  background: #fff;
  width: 520px;
  max-width: 100vw;
  display: flex;
  flex-direction: column;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.12);
  overflow: hidden;
}
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.25rem;
  border-bottom: 1px solid #e5e7eb;
}
.drawer-header h2 { font-size: 1.1rem; font-weight: 600; }
.drawer-body {
  flex: 1;
  overflow-y: auto;
  padding: 1.25rem;
}
.drawer-footer {
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
  padding: 1rem 1.25rem;
  border-top: 1px solid #e5e7eb;
}

/* Form */
.form-group { margin-bottom: 1rem; }
.form-label { display: block; font-size: 0.875rem; font-weight: 500; color: #374151; margin-bottom: 0.25rem; }
.required { color: #ef4444; }
.form-input {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  font-size: 0.875rem;
  background: #fff;
  outline: none;
}
.form-input:focus { border-color: #6366f1; box-shadow: 0 0 0 2px rgba(99,102,241,0.15); }
.form-input-sm { padding: 0.375rem 0.5rem; }
.form-hint { font-size: 0.75rem; color: #9ca3af; margin-top: 0.25rem; }
.matcher-row {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}
.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 1rem;
  padding: 0.25rem;
  line-height: 1;
}
.btn-danger {
  background: #fee2e2;
  color: #dc2626;
  border: 1px solid #fca5a5;
}
.btn-danger:hover { background: #fecaca; }
.flex { display: flex; }
.flex-1 { flex: 1; }
.gap-1 { gap: 0.25rem; }
.gap-2 { gap: 0.5rem; }
.items-center { align-items: center; }
.whitespace-nowrap { white-space: nowrap; }

/* Preview */
.preview-panel {
  border: 1px dashed #d1d5db;
  border-radius: 0.5rem;
  padding: 1rem;
  background: #f9fafb;
  margin-top: 1rem;
}
.preview-title { font-weight: 600; font-size: 0.9rem; margin-bottom: 0.5rem; }
.preview-results { max-height: 200px; overflow-y: auto; }
.preview-result-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.375rem 0.5rem;
  border-radius: 0.25rem;
  background: #fff;
  border: 1px solid #e5e7eb;
  margin-bottom: 0.25rem;
}
</style>
