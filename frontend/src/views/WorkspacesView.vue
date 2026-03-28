<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useWorkspaceStore } from '@/store/workspace'
import Modal from '@/components/Modal.vue'
import type { WorkspaceRequest } from '@/types'

const store = useWorkspaceStore()
const router = useRouter()

const showCreateModal = ref(false)
const showEditModal = ref(false)
const editingId = ref<number | null>(null)
const formName = ref('')
const formDesc = ref('')
const formError = ref('')
const saving = ref(false)

onMounted(() => store.fetchWorkspaces())

function openCreate() {
  formName.value = ''
  formDesc.value = ''
  formError.value = ''
  showCreateModal.value = true
}

function openEdit(ws: { id: number; name: string; description: string }) {
  editingId.value = ws.id
  formName.value = ws.name
  formDesc.value = ws.description ?? ''
  formError.value = ''
  showEditModal.value = true
}

async function submitCreate() {
  if (!formName.value.trim()) { formError.value = 'Name is required.'; return }
  saving.value = true
  formError.value = ''
  try {
    const payload: WorkspaceRequest = { name: formName.value.trim(), description: formDesc.value.trim() }
    await store.createWorkspace(payload)
    showCreateModal.value = false
  } catch {
    formError.value = 'Failed to create workspace.'
  } finally {
    saving.value = false
  }
}

async function submitEdit() {
  if (!formName.value.trim() || editingId.value === null) return
  saving.value = true
  formError.value = ''
  try {
    const payload: WorkspaceRequest = { name: formName.value.trim(), description: formDesc.value.trim() }
    await store.updateWorkspace(editingId.value, payload)
    showEditModal.value = false
  } catch {
    formError.value = 'Failed to update workspace.'
  } finally {
    saving.value = false
  }
}

async function handleDelete(id: number, name: string) {
  if (!confirm(`Delete workspace "${name}"? This cannot be undone.`)) return
  try {
    await store.deleteWorkspace(id)
  } catch {
    alert('Failed to delete workspace.')
  }
}

function viewWorkspace(id: number) {
  router.push(`/workspaces/${id}`)
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric'
  })
}
</script>

<template>
  <div class="page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1>Workspaces</h1>
        <p class="text-gray-500 text-sm mt-1">Manage your data masking workspaces</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">
        ＋ New Workspace
      </button>
    </div>

    <!-- Loading -->
    <div v-if="store.loading" class="loading-overlay">
      <span class="spinner spinner-lg" />
      <span>Loading workspaces…</span>
    </div>

    <!-- Error -->
    <div v-else-if="store.error" class="alert alert-error">{{ store.error }}</div>

    <!-- Empty -->
    <div v-else-if="store.workspaces.length === 0" class="empty-state">
      <div class="empty-state-icon">📂</div>
      <h3>No workspaces yet</h3>
      <p>Create your first workspace to get started with data masking.</p>
      <button class="btn btn-primary mt-4" @click="openCreate">
        ＋ Create Workspace
      </button>
    </div>

    <!-- Grid -->
    <div v-else class="grid grid-cols-3">
      <div
        v-for="ws in store.workspaces"
        :key="ws.id"
        class="workspace-card"
        @click="viewWorkspace(ws.id)"
      >
        <div class="ws-card-header">
          <span class="ws-icon">🗂️</span>
          <div class="ws-meta">
            <h3 class="ws-name">{{ ws.name }}</h3>
            <span class="text-xs text-gray-400">Created {{ formatDate(ws.createdAt) }}</span>
          </div>
        </div>

        <p v-if="ws.description" class="ws-desc">{{ ws.description }}</p>
        <p v-else class="ws-desc text-gray-400 italic">No description provided.</p>

        <div class="ws-owner">
          <span class="badge badge-gray">{{ ws.ownerUsername }}</span>
        </div>

        <div class="ws-actions" @click.stop>
          <button class="btn btn-secondary btn-sm" @click="viewWorkspace(ws.id)">
            View
          </button>
          <button class="btn btn-secondary btn-sm" @click="openEdit(ws)">
            Edit
          </button>
          <button class="btn btn-danger btn-sm" @click="handleDelete(ws.id, ws.name)">
            Delete
          </button>
        </div>
      </div>
    </div>

    <!-- Create Modal -->
    <Modal v-if="showCreateModal" title="Create Workspace" @close="showCreateModal = false">
      <form @submit.prevent="submitCreate">
        <div v-if="formError" class="alert alert-error">{{ formError }}</div>
        <div class="form-group">
          <label class="form-label">Name *</label>
          <input v-model="formName" type="text" class="form-control" placeholder="My Workspace" required />
        </div>
        <div class="form-group">
          <label class="form-label">Description</label>
          <textarea v-model="formDesc" class="form-control" placeholder="Optional description…" rows="3" />
        </div>
      </form>
      <template #footer>
        <button class="btn btn-secondary" @click="showCreateModal = false">Cancel</button>
        <button class="btn btn-primary" :disabled="saving" @click="submitCreate">
          <span v-if="saving" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          Create
        </button>
      </template>
    </Modal>

    <!-- Edit Modal -->
    <Modal v-if="showEditModal" title="Edit Workspace" @close="showEditModal = false">
      <form @submit.prevent="submitEdit">
        <div v-if="formError" class="alert alert-error">{{ formError }}</div>
        <div class="form-group">
          <label class="form-label">Name *</label>
          <input v-model="formName" type="text" class="form-control" required />
        </div>
        <div class="form-group">
          <label class="form-label">Description</label>
          <textarea v-model="formDesc" class="form-control" rows="3" />
        </div>
      </form>
      <template #footer>
        <button class="btn btn-secondary" @click="showEditModal = false">Cancel</button>
        <button class="btn btn-primary" :disabled="saving" @click="submitEdit">
          <span v-if="saving" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          Save Changes
        </button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.ws-card-header {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}
.ws-icon { font-size: 1.75rem; flex-shrink: 0; }
.ws-meta { flex: 1; min-width: 0; }
.ws-name {
  font-size: 1.05rem;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ws-desc {
  font-size: 0.875rem;
  color: #4b5563;
  margin-bottom: 0.75rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ws-owner { margin-bottom: 1rem; }
.ws-actions { display: flex; gap: 0.5rem; }
</style>
