<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import Modal from '@/components/Modal.vue'
import * as actionsApi from '@/api/actions'
import type { PostJobAction, PostJobActionRequest } from '@/types'
import { ActionType } from '@/types'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

const actions = ref<PostJobAction[]>([])
const loading = ref(false)
const error = ref('')
const showModal = ref(false)
const editingAction = ref<PostJobAction | null>(null)
const saving = ref(false)
const formError = ref('')

const defaultForm = (): PostJobActionRequest => ({
  actionType: ActionType.WEBHOOK,
  config: '{}',
  enabled: true
})

const form = ref<PostJobActionRequest>(defaultForm())
const actionTypes = Object.values(ActionType)

async function fetchActions() {
  loading.value = true
  error.value = ''
  try {
    actions.value = await actionsApi.listActions(workspaceId.value)
  } catch {
    error.value = 'Failed to load post-job actions.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchActions)

function openCreate() {
  editingAction.value = null
  form.value = defaultForm()
  formError.value = ''
  showModal.value = true
}

function openEdit(action: PostJobAction) {
  editingAction.value = action
  form.value = {
    actionType: action.actionType,
    config: action.config,
    enabled: action.enabled
  }
  formError.value = ''
  showModal.value = true
}

async function submitForm() {
  formError.value = ''
  try {
    const parsed = JSON.parse(form.value.config)
    if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) throw new Error()
  } catch {
    formError.value = 'Config must be valid JSON object.'
    return
  }
  saving.value = true
  try {
    if (editingAction.value) {
      const updated = await actionsApi.updateAction(
        workspaceId.value,
        editingAction.value.id,
        form.value
      )
      const idx = actions.value.findIndex((a) => a.id === updated.id)
      if (idx !== -1) actions.value[idx] = updated
    } else {
      const created = await actionsApi.createAction(workspaceId.value, form.value)
      actions.value.push(created)
    }
    showModal.value = false
  } catch {
    formError.value = 'Failed to save action.'
  } finally {
    saving.value = false
  }
}

async function handleDelete(action: PostJobAction) {
  if (!confirm(`Delete this ${action.actionType} action?`)) return
  try {
    await actionsApi.deleteAction(workspaceId.value, action.id)
    actions.value = actions.value.filter((a) => a.id !== action.id)
  } catch {
    alert('Failed to delete action.')
  }
}

async function toggleEnabled(action: PostJobAction) {
  try {
    const updated = await actionsApi.updateAction(workspaceId.value, action.id, {
      actionType: action.actionType,
      config: action.config,
      enabled: !action.enabled
    })
    const idx = actions.value.findIndex((a) => a.id === action.id)
    if (idx !== -1) actions.value[idx] = updated
  } catch {
    alert('Failed to update action.')
  }
}

function actionTypeIcon(type: ActionType): string {
  const icons: Record<ActionType, string> = {
    [ActionType.WEBHOOK]: '🔗',
    [ActionType.EMAIL]: '📧',
    [ActionType.SCRIPT]: '📜'
  }
  return icons[type] ?? '⚡'
}

function configSummary(action: PostJobAction): string {
  try {
    const cfg = JSON.parse(action.config) as Record<string, string>
    if (action.actionType === ActionType.WEBHOOK) return cfg.url ?? '—'
    if (action.actionType === ActionType.EMAIL) return cfg.to ?? '—'
    if (action.actionType === ActionType.SCRIPT) return cfg.path ?? '—'
  } catch {
    // ignore
  }
  return action.config
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
      <RouterLink :to="`/workspaces/${workspaceId}`">Workspace</RouterLink>
      <span class="breadcrumb-sep">›</span>
      <span>Actions</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Post-Job Actions</h1>
        <p class="text-gray-500 text-sm mt-1">Configure webhooks, emails, and scripts triggered after each job</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">＋ Add Action</button>
    </div>

    <div v-if="loading" class="loading-overlay">
      <span class="spinner spinner-lg" />
    </div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="actions.length === 0" class="empty-state">
      <div class="empty-state-icon">⚡</div>
      <h3>No post-job actions</h3>
      <p>Add a webhook, email, or script to run automatically when a job completes.</p>
      <button class="btn btn-primary mt-4" @click="openCreate">＋ Add Action</button>
    </div>

    <div v-else class="card">
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Type</th>
              <th>Target</th>
              <th>Enabled</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="action in actions" :key="action.id">
              <td>
                <span class="badge badge-indigo">
                  {{ actionTypeIcon(action.actionType) }} {{ action.actionType }}
                </span>
              </td>
              <td class="text-gray-600 config-cell">{{ configSummary(action) }}</td>
              <td>
                <span
                  class="badge"
                  :class="action.enabled ? 'badge-green' : 'badge-gray'"
                >
                  {{ action.enabled ? 'Enabled' : 'Disabled' }}
                </span>
              </td>
              <td class="text-gray-500 text-sm">{{ formatDate(action.createdAt) }}</td>
              <td>
                <div class="flex gap-2">
                  <button
                    class="btn btn-secondary btn-sm"
                    @click="toggleEnabled(action)"
                  >
                    {{ action.enabled ? 'Disable' : 'Enable' }}
                  </button>
                  <button class="btn btn-secondary btn-sm" @click="openEdit(action)">Edit</button>
                  <button class="btn btn-danger btn-sm" @click="handleDelete(action)">Delete</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Modal -->
    <Modal
      v-if="showModal"
      :title="editingAction ? 'Edit Action' : 'Add Post-Job Action'"
      @close="showModal = false"
    >
      <form @submit.prevent="submitForm">
        <div v-if="formError" class="alert alert-error">{{ formError }}</div>

        <div class="form-group">
          <label class="form-label">Action Type *</label>
          <select v-model="form.actionType" class="form-control">
            <option v-for="t in actionTypes" :key="t" :value="t">
              {{ actionTypeIcon(t as ActionType) }} {{ t }}
            </option>
          </select>
        </div>

        <div class="form-group">
          <label class="form-label">
            Config (JSON) *
            <span class="text-gray-400 font-normal text-xs ml-2">
              <template v-if="form.actionType === ActionType.WEBHOOK">e.g. {"url":"https://…"}</template>
              <template v-else-if="form.actionType === ActionType.EMAIL">e.g. {"to":"user@example.com"}</template>
              <template v-else>e.g. {"path":"/scripts/notify.sh"}</template>
            </span>
          </label>
          <textarea
            v-model="form.config"
            class="form-control"
            rows="4"
            style="font-family: monospace; font-size: 0.85rem;"
            placeholder="{}"
          />
        </div>

        <div class="form-group" style="display:flex;align-items:center;gap:.75rem;">
          <input
            id="enabled"
            v-model="form.enabled"
            type="checkbox"
            style="width:1rem;height:1rem;cursor:pointer;"
          />
          <label for="enabled" class="form-label" style="margin:0;cursor:pointer;">Enabled</label>
        </div>
      </form>
      <template #footer>
        <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
        <button class="btn btn-primary" :disabled="saving" @click="submitForm">
          <span v-if="saving" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          {{ editingAction ? 'Save Changes' : 'Add Action' }}
        </button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.config-cell {
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.875rem;
}
</style>
