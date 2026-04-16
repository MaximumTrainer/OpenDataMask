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

// SQL-specific form fields (used to build the JDBC connection string)
// Shared form fields
const form = ref<DataConnectionRequest & { host: string; port: number; sslEnabled: boolean }>({
  name: '',
  type: ConnectionType.POSTGRESQL,
  connectionString: '',
  host: 'localhost',
  port: 5432,
  database: '',
  username: '',
  password: '',
  sslEnabled: false,
  isSource: false,
  isDestination: false
})

// Types that use a direct connection string instead of host/port
const mongoTypes = new Set<ConnectionType>([ConnectionType.MONGODB, ConnectionType.MONGODB_COSMOS])

const isMongoType = computed(() => mongoTypes.has(form.value.type))

const defaultPorts: Partial<Record<ConnectionType, number>> = {
  [ConnectionType.POSTGRESQL]: 5432,
  [ConnectionType.MONGODB]: 27017,
  [ConnectionType.AZURE_SQL]: 1433,
  [ConnectionType.MONGODB_COSMOS]: 10255,
  [ConnectionType.MYSQL]: 3306
}

// Labels shown in the type selector (exclude FILE from the standard form)
const displayConnectionTypes = Object.values(ConnectionType).filter(
  (t) => t !== ConnectionType.FILE
)

function typeLabel(t: ConnectionType) {
  const labels: Record<ConnectionType, string> = {
    [ConnectionType.POSTGRESQL]: 'PostgreSQL',
    [ConnectionType.MONGODB]: 'MongoDB',
    [ConnectionType.AZURE_SQL]: 'Azure SQL',
    [ConnectionType.MONGODB_COSMOS]: 'MongoDB Cosmos',
    [ConnectionType.FILE]: 'File',
    [ConnectionType.MYSQL]: 'MySQL'
  }
  return labels[t] ?? t
}

function resetForm() {
  form.value = {
    name: '',
    type: ConnectionType.POSTGRESQL,
    connectionString: '',
    host: 'localhost',
    port: defaultPorts[ConnectionType.POSTGRESQL] ?? 5432,
    database: '',
    username: '',
    password: '',
    sslEnabled: false,
    isSource: false,
    isDestination: false
  }
}

function onTypeChange() {
  const port = defaultPorts[form.value.type]
  if (port !== undefined) {
    form.value.port = port
  }
  // Clear connection string on type switch
  form.value.connectionString = ''
}

// Build a JDBC / MongoDB URI from the form fields
function buildConnectionString(): string {
  const { type, host, port, database, sslEnabled } = form.value
  switch (type) {
    case ConnectionType.POSTGRESQL:
      return `jdbc:postgresql://${host}:${port}/${database}${sslEnabled ? '?ssl=require' : ''}`
    case ConnectionType.MYSQL:
      return `jdbc:mysql://${host}:${port}/${database}${sslEnabled ? '?useSSL=true' : ''}`
    case ConnectionType.AZURE_SQL:
      return `jdbc:sqlserver://${host}:${port};databaseName=${database};encrypt=true`
    case ConnectionType.MONGODB:
    case ConnectionType.MONGODB_COSMOS:
      // User enters the full URI directly in connectionString field
      return form.value.connectionString ?? ''
    default:
      return form.value.connectionString ?? ''
  }
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
  resetForm()
  formError.value = ''
  showModal.value = true
}

function openEdit(conn: DataConnection) {
  editingConnection.value = conn
  // Parse host/port back from stored host string for SQL types (e.g. "localhost:5432")
  let host = 'localhost'
  let port = defaultPorts[conn.type] ?? 5432
  if (conn.host && !mongoTypes.has(conn.type)) {
    const parts = conn.host.split(':')
    host = parts[0] ?? 'localhost'
    const parsedPort = parts[1] ? parseInt(parts[1], 10) : NaN
    port = isNaN(parsedPort) ? (defaultPorts[conn.type] ?? 5432) : parsedPort
  }
  form.value = {
    name: conn.name,
    type: conn.type,
    connectionString: '',   // Never pre-filled – must be re-entered if changed
    host,
    port,
    database: conn.database ?? '',
    username: conn.username ?? '',
    password: '',
    sslEnabled: false,
    isSource: conn.isSource,
    isDestination: conn.isDestination
  }
  formError.value = ''
  showModal.value = true
}

function validateForm(): boolean {
  if (!form.value.name) {
    formError.value = 'Connection name is required.'
    return false
  }
  if (isMongoType.value) {
    // For MongoDB, connection string is required on create
    if (!editingConnection.value && !form.value.connectionString) {
      formError.value = 'Connection URI is required.'
      return false
    }
  } else {
    // For SQL types, host, database and username are required
    if (!form.value.host || !form.value.database || !form.value.username) {
      formError.value = 'Host, database, and username are required.'
      return false
    }
    if (!editingConnection.value && !form.value.password) {
      formError.value = 'Password is required.'
      return false
    }
  }
  if (!form.value.isSource && !form.value.isDestination) {
    formError.value = 'Select at least one role: Source or Destination.'
    return false
  }
  return true
}

async function submitForm() {
  if (!validateForm()) return

  const connectionString = buildConnectionString()
  const payload: DataConnectionRequest = {
    name: form.value.name,
    type: form.value.type,
    username: form.value.username || undefined,
    database: form.value.database || undefined,
    isSource: form.value.isSource,
    isDestination: form.value.isDestination
  }

  // Only include connectionString if it is non-empty (on update, blank = keep existing)
  if (connectionString) {
    payload.connectionString = connectionString
  }
  if (form.value.password) {
    payload.password = form.value.password
  }

  saving.value = true
  formError.value = ''
  try {
    if (editingConnection.value) {
      const updated = await connectionsApi.updateConnection(
        workspaceId.value,
        editingConnection.value.id,
        payload
      )
      const idx = connections.value.findIndex((c) => c.id === updated.id)
      if (idx !== -1) connections.value[idx] = updated
    } else {
      const created = await connectionsApi.createConnection(workspaceId.value, payload)
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
              <th>Roles</th>
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
              <td class="text-gray-600">{{ conn.host ?? '—' }}</td>
              <td class="text-gray-600">{{ conn.database ?? '—' }}</td>
              <td>
                <div class="flex gap-1">
                  <span v-if="conn.isSource" class="badge badge-blue">Source</span>
                  <span v-if="conn.isDestination" class="badge badge-green">Destination</span>
                </div>
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

        <!-- Name & Type row -->
        <div class="grid grid-cols-2">
          <div class="form-group">
            <label class="form-label">Name *</label>
            <input v-model="form.name" type="text" class="form-control" placeholder="Production DB" required />
          </div>
          <div class="form-group">
            <label class="form-label">Type *</label>
            <select v-model="form.type" class="form-control" @change="onTypeChange">
              <option v-for="t in displayConnectionTypes" :key="t" :value="t">{{ typeLabel(t) }}</option>
            </select>
          </div>
        </div>

        <!-- MongoDB / Cosmos: single connection URI field -->
        <template v-if="isMongoType">
          <div class="form-group">
            <label class="form-label">
              Connection URI {{ editingConnection ? '(leave blank to keep existing)' : '*' }}
            </label>
            <input
              v-model="form.connectionString"
              type="text"
              class="form-control"
              placeholder="mongodb://user:password@localhost:27017"
              autocomplete="off"
            />
            <p class="form-hint">
              Full MongoDB connection string, e.g.
              <code>mongodb://user:pass@host:27017/mydb</code> or
              <code>mongodb+srv://user:pass@cluster.mongodb.net/mydb</code>
            </p>
          </div>
          <div class="form-group">
            <label class="form-label">Database (optional)</label>
            <input v-model="form.database" type="text" class="form-control" placeholder="mydb" />
            <p class="form-hint">Leave blank to use the database specified in the URI.</p>
          </div>
        </template>

        <!-- SQL types: host / port / database / username / password / SSL -->
        <template v-else>
          <div class="grid grid-cols-2">
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
        </template>

        <!-- Source / Destination roles -->
        <div class="form-group">
          <label class="form-label">Role *</label>
          <div class="flex gap-4">
            <label class="flex items-center gap-2" style="cursor:pointer;">
              <input v-model="form.isSource" type="checkbox" style="width:1rem;height:1rem;" />
              Source (read data from this connection)
            </label>
            <label class="flex items-center gap-2" style="cursor:pointer;">
              <input v-model="form.isDestination" type="checkbox" style="width:1rem;height:1rem;" />
              Destination (write masked data to this connection)
            </label>
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

<style scoped>
.form-hint {
  font-size: 0.78rem;
  color: #6b7280;
  margin-top: 0.25rem;
}
.form-hint code {
  font-family: monospace;
  background: #f3f4f6;
  padding: 0 0.25rem;
  border-radius: 3px;
}
</style>
