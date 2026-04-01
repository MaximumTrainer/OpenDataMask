<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import AppModal from '@/components/AppModal.vue'
import * as connectionsApi from '@/api/connections'
import type { DataConnection, DataConnectionRequest } from '@/types'
import { ConnectionType } from '@/types'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

const connections = ref<DataConnection[]>([])
const loading = ref(false)
const error = ref('')
const showModal = ref(false)
const editingConnection = ref<DataConnection | null>(null)
const saving = ref(false)
const testing = ref<number | null>(null)
const testResults = ref<Record<number, { success: boolean; message: string }>>({})
const formError = ref('')

const defaultForm = (): DataConnectionRequest => ({
  name: '',
  type: ConnectionType.POSTGRESQL,
  host: 'localhost',
  port: 5432,
  database: '',
  username: '',
  password: '',
  sslEnabled: false
})

const form = ref<DataConnectionRequest>(defaultForm())

const connectionTypes = Object.values(ConnectionType)

const defaultPorts: Record<ConnectionType, number> = {
  [ConnectionType.POSTGRESQL]: 5432,
  [ConnectionType.MONGODB]: 27017,
  [ConnectionType.AZURE_SQL]: 1433,
  [ConnectionType.MONGODB_COSMOS]: 10255
}

function onTypeChange() {
  form.value.port = defaultPorts[form.value.type]
}

async function fetchConnections() {
  loading.value = true
  error.value = ''
  try {
    connections.value = await connectionsApi.listConnections(workspaceId.value)
  } catch {
    error.value = 'Failed to load connections.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchConnections)

function openCreate() {
  editingConnection.value = null
  form.value = defaultForm()
  formError.value = ''
  showModal.value = true
}

function openEdit(conn: DataConnection) {
  editingConnection.value = conn
  form.value = {
    name: conn.name,
    type: conn.type,
    host: conn.host,
    port: conn.port,
    database: conn.database,
    username: conn.username,
    password: '',
    sslEnabled: conn.sslEnabled
  }
  formError.value = ''
  showModal.value = true
}

async function submitForm() {
  if (!form.value.name || !form.value.host || !form.value.database || !form.value.username) {
    formError.value = 'Please fill in all required fields.'
    return
  }
  if (!editingConnection.value && !form.value.password) {
    formError.value = 'Password is required.'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    if (editingConnection.value) {
      const updated = await connectionsApi.updateConnection(
        workspaceId.value,
        editingConnection.value.id,
        form.value
      )
      const idx = connections.value.findIndex((c) => c.id === updated.id)
      if (idx !== -1) connections.value[idx] = updated
    } else {
      const created = await connectionsApi.createConnection(workspaceId.value, form.value)
      connections.value.push(created)
    }
    showModal.value = false
  } catch {
    formError.value = 'Failed to save connection.'
  } finally {
    saving.value = false
  }
}

async function handleDelete(conn: DataConnection) {
  if (!confirm(`Delete connection "${conn.name}"?`)) return
  try {
    await connectionsApi.deleteConnection(workspaceId.value, conn.id)
    connections.value = connections.value.filter((c) => c.id !== conn.id)
  } catch {
    alert('Failed to delete connection.')
  }
}

async function handleTest(conn: DataConnection) {
  testing.value = conn.id
  try {
    const result = await connectionsApi.testConnection(workspaceId.value, conn.id)
    testResults.value[conn.id] = result
  } catch {
    testResults.value[conn.id] = { success: false, message: 'Connection test failed.' }
  } finally {
    testing.value = null
  }
}

function typeLabel(t: ConnectionType) {
  return t.charAt(0) + t.slice(1).toLowerCase()
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
      <span>Connections</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Data Connections</h1>
        <p class="text-gray-500 text-sm mt-1">Manage source and target database connections</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">＋ Add Connection</button>
    </div>

    <div v-if="loading" class="loading-overlay">
      <span class="spinner spinner-lg" />
    </div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="connections.length === 0" class="empty-state">
      <div class="empty-state-icon">🔌</div>
      <h3>No connections yet</h3>
      <p>Add a data connection to start configuring data masking.</p>
      <button class="btn btn-primary mt-4" @click="openCreate">＋ Add Connection</button>
    </div>

    <div v-else class="card">
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Type</th>
              <th>Host</th>
              <th>Database</th>
              <th>SSL</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="conn in connections" :key="conn.id">
              <td class="font-medium">{{ conn.name }}</td>
              <td>
                <span class="badge badge-indigo">{{ typeLabel(conn.type) }}</span>
              </td>
              <td class="text-gray-600">{{ conn.host }}:{{ conn.port }}</td>
              <td class="text-gray-600">{{ conn.database }}</td>
              <td>
                <span v-if="conn.sslEnabled" class="badge badge-green">Yes</span>
                <span v-else class="badge badge-gray">No</span>
              </td>
              <td>
                <span
                  v-if="testResults[conn.id]"
                  class="badge"
                  :class="testResults[conn.id].success ? 'badge-green' : 'badge-red'"
                >
                  {{ testResults[conn.id].success ? 'OK' : 'Error' }}
                </span>
                <span v-else class="text-gray-400 text-sm">—</span>
              </td>
              <td>
                <div class="flex gap-2">
                  <button
                    class="btn btn-success btn-sm"
                    :disabled="testing === conn.id"
                    @click="handleTest(conn)"
                  >
                    <span v-if="testing === conn.id" class="spinner" style="width:.8rem;height:.8rem;border-width:2px;" />
                    Test
                  </button>
                  <button class="btn btn-secondary btn-sm" @click="openEdit(conn)">Edit</button>
                  <button class="btn btn-danger btn-sm" @click="handleDelete(conn)">Delete</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Test result messages -->
    <div v-if="Object.keys(testResults).length" class="mt-4">
      <div
        v-for="(result, connId) in testResults"
        :key="connId"
        class="alert"
        :class="result.success ? 'alert-success' : 'alert-error'"
      >
        <strong>{{ connections.find(c => c.id === Number(connId))?.name }}:</strong>
        {{ result.message }}
      </div>
    </div>

    <!-- Modal -->
    <AppModal
      v-if="showModal"
      :title="editingConnection ? 'Edit Connection' : 'Add Connection'"
      size="lg"
      @close="showModal = false"
    >
      <form @submit.prevent="submitForm">
        <div v-if="formError" class="alert alert-error">{{ formError }}</div>
        <div class="grid grid-cols-2">
          <div class="form-group">
            <label class="form-label">Name *</label>
            <input v-model="form.name" type="text" class="form-control" placeholder="Production DB" required />
          </div>
          <div class="form-group">
            <label class="form-label">Type *</label>
            <select v-model="form.type" class="form-control" @change="onTypeChange">
              <option v-for="t in connectionTypes" :key="t" :value="t">{{ typeLabel(t) }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Host *</label>
            <input v-model="form.host" type="text" class="form-control" placeholder="localhost" required />
          </div>
          <div class="form-group">
            <label class="form-label">Port *</label>
            <input v-model.number="form.port" type="number" class="form-control" required />
          </div>
          <div class="form-group">
            <label class="form-label">Database *</label>
            <input v-model="form.database" type="text" class="form-control" placeholder="mydb" required />
          </div>
          <div class="form-group">
            <label class="form-label">Username *</label>
            <input v-model="form.username" type="text" class="form-control" placeholder="admin" required />
          </div>
          <div class="form-group">
            <label class="form-label">
              Password {{ editingConnection ? '(leave blank to keep current)' : '*' }}
            </label>
            <input v-model="form.password" type="password" class="form-control" autocomplete="new-password" />
          </div>
          <div class="form-group" style="display:flex;align-items:center;gap:.75rem;padding-top:1.75rem;">
            <input
              id="ssl"
              v-model="form.sslEnabled"
              type="checkbox"
              style="width:1rem;height:1rem;cursor:pointer;"
            />
            <label for="ssl" class="form-label" style="margin:0;cursor:pointer;">Enable SSL</label>
          </div>
        </div>
      </form>
      <template #footer>
        <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
        <button class="btn btn-primary" :disabled="saving" @click="submitForm">
          <span v-if="saving" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          {{ editingConnection ? 'Save Changes' : 'Add Connection' }}
        </button>
      </template>
    </AppModal>
  </div>
</template>
