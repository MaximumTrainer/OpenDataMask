<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import Modal from '@/components/Modal.vue'
import * as tablesApi from '@/api/tables'
import * as connectionsApi from '@/api/connections'
import type {
  TableConfiguration,
  TableConfigurationRequest,
  ColumnGenerator,
  ColumnGeneratorRequest,
  DataConnection
} from '@/types'
import { TableMode, GeneratorType } from '@/types'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

const tables = ref<TableConfiguration[]>([])
const connections = ref<DataConnection[]>([])
const loading = ref(false)
const error = ref('')

// Table modal state
const showTableModal = ref(false)
const editingTable = ref<TableConfiguration | null>(null)
const tableForm = ref<TableConfigurationRequest>({
  connectionId: 0,
  schemaName: 'public',
  tableName: '',
  mode: TableMode.MASK
})
const tableFormError = ref('')
const savingTable = ref(false)

// Column modal state
const showColumnModal = ref(false)
const activeTable = ref<TableConfiguration | null>(null)
const editingColumn = ref<ColumnGenerator | null>(null)
const columnForm = ref<ColumnGeneratorRequest>({
  columnName: '',
  generatorType: GeneratorType.NULL,
  parameters: {}
})
const columnFormError = ref('')
const savingColumn = ref(false)

// Expanded table for column view
const expandedTableId = ref<number | null>(null)

const tableModes = Object.values(TableMode)
const generatorTypes = Object.values(GeneratorType)

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const [t, c] = await Promise.all([
      tablesApi.listTableConfigs(workspaceId.value),
      connectionsApi.listConnections(workspaceId.value)
    ])
    tables.value = t
    connections.value = c
  } catch {
    error.value = 'Failed to load table configurations.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

// ── Table CRUD ──
function openCreateTable() {
  editingTable.value = null
  tableForm.value = {
    connectionId: connections.value[0]?.id ?? 0,
    schemaName: 'public',
    tableName: '',
    mode: TableMode.MASK
  }
  tableFormError.value = ''
  showTableModal.value = true
}

function openEditTable(t: TableConfiguration) {
  editingTable.value = t
  tableForm.value = {
    connectionId: t.connectionId,
    schemaName: t.schemaName,
    tableName: t.tableName,
    mode: t.mode,
    whereClause: t.whereClause
  }
  tableFormError.value = ''
  showTableModal.value = true
}

async function submitTableForm() {
  if (!tableForm.value.tableName || !tableForm.value.connectionId) {
    tableFormError.value = 'Table name and connection are required.'
    return
  }
  savingTable.value = true
  tableFormError.value = ''
  try {
    if (editingTable.value) {
      const updated = await tablesApi.updateTableConfig(
        workspaceId.value,
        editingTable.value.id,
        tableForm.value
      )
      const idx = tables.value.findIndex((t) => t.id === updated.id)
      if (idx !== -1) tables.value[idx] = { ...tables.value[idx], ...updated }
    } else {
      const created = await tablesApi.createTableConfig(workspaceId.value, tableForm.value)
      tables.value.push({ ...created, columnGenerators: [] })
    }
    showTableModal.value = false
  } catch {
    tableFormError.value = 'Failed to save table configuration.'
  } finally {
    savingTable.value = false
  }
}

async function deleteTable(t: TableConfiguration) {
  if (!confirm(`Delete table config "${t.schemaName}.${t.tableName}"?`)) return
  try {
    await tablesApi.deleteTableConfig(workspaceId.value, t.id)
    tables.value = tables.value.filter((x) => x.id !== t.id)
    if (expandedTableId.value === t.id) expandedTableId.value = null
  } catch {
    alert('Failed to delete table configuration.')
  }
}

// ── Column CRUD ──
async function toggleExpand(t: TableConfiguration) {
  if (expandedTableId.value === t.id) {
    expandedTableId.value = null
    return
  }
  expandedTableId.value = t.id
  // Refresh column generators
  try {
    const cols = await tablesApi.listColumnGenerators(workspaceId.value, t.id)
    const idx = tables.value.findIndex((x) => x.id === t.id)
    if (idx !== -1) tables.value[idx].columnGenerators = cols
  } catch {
    // ignore – show existing data
  }
}

function openCreateColumn(t: TableConfiguration) {
  activeTable.value = t
  editingColumn.value = null
  columnForm.value = { columnName: '', generatorType: GeneratorType.NULL, parameters: {} }
  columnFormError.value = ''
  showColumnModal.value = true
}

function openEditColumn(t: TableConfiguration, col: ColumnGenerator) {
  activeTable.value = t
  editingColumn.value = col
  columnForm.value = {
    columnName: col.columnName,
    generatorType: col.generatorType,
    parameters: { ...(col.parameters ?? {}) }
  }
  columnFormError.value = ''
  showColumnModal.value = true
}

async function submitColumnForm() {
  if (!columnForm.value.columnName || !activeTable.value) {
    columnFormError.value = 'Column name is required.'
    return
  }
  savingColumn.value = true
  columnFormError.value = ''
  try {
    const tIdx = tables.value.findIndex((t) => t.id === activeTable.value!.id)
    if (editingColumn.value) {
      const updated = await tablesApi.updateColumnGenerator(
        workspaceId.value,
        activeTable.value!.id,
        editingColumn.value.id,
        columnForm.value
      )
      const cIdx = tables.value[tIdx].columnGenerators.findIndex((c) => c.id === updated.id)
      if (cIdx !== -1) tables.value[tIdx].columnGenerators[cIdx] = updated
    } else {
      const created = await tablesApi.createColumnGenerator(
        workspaceId.value,
        activeTable.value!.id,
        columnForm.value
      )
      tables.value[tIdx].columnGenerators.push(created)
    }
    showColumnModal.value = false
  } catch {
    columnFormError.value = 'Failed to save column generator.'
  } finally {
    savingColumn.value = false
  }
}

async function deleteColumn(t: TableConfiguration, col: ColumnGenerator) {
  if (!confirm(`Delete column generator for "${col.columnName}"?`)) return
  try {
    await tablesApi.deleteColumnGenerator(workspaceId.value, t.id, col.id)
    const idx = tables.value.findIndex((x) => x.id === t.id)
    tables.value[idx].columnGenerators = tables.value[idx].columnGenerators.filter(
      (c) => c.id !== col.id
    )
  } catch {
    alert('Failed to delete column generator.')
  }
}

// ── Helpers ──
function modeBadgeClass(mode: TableMode) {
  const map: Record<TableMode, string> = {
    [TableMode.PASSTHROUGH]: 'badge-gray',
    [TableMode.MASK]:        'badge-blue',
    [TableMode.GENERATE]:    'badge-purple',
    [TableMode.SUBSET]:      'badge-indigo',
    [TableMode.SKIP]:        'badge-yellow'
  }
  return map[mode] ?? 'badge-gray'
}

function connectionName(id: number) {
  return connections.value.find((c) => c.id === id)?.name ?? `#${id}`
}

function paramString(params?: Record<string, string>) {
  if (!params || Object.keys(params).length === 0) return '—'
  return Object.entries(params).map(([k, v]) => `${k}=${v}`).join(', ')
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
      <span>Tables</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Table Configurations</h1>
        <p class="text-gray-500 text-sm mt-1">Define masking rules for each table</p>
      </div>
      <button class="btn btn-primary" @click="openCreateTable">＋ Add Table</button>
    </div>

    <div v-if="loading" class="loading-overlay">
      <span class="spinner spinner-lg" />
    </div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="tables.length === 0" class="empty-state">
      <div class="empty-state-icon">📊</div>
      <h3>No table configurations</h3>
      <p>Add table configurations to define how each table is masked.</p>
      <button class="btn btn-primary mt-4" @click="openCreateTable">＋ Add Table</button>
    </div>

    <div v-else class="tables-list">
      <div v-for="table in tables" :key="table.id" class="table-item card">
        <!-- Table header row -->
        <div class="table-item-header">
          <div class="table-info">
            <span class="table-icon">📊</span>
            <div>
              <div class="flex items-center gap-2">
                <span class="font-semibold">{{ table.schemaName }}.{{ table.tableName }}</span>
                <span class="badge" :class="modeBadgeClass(table.mode)">{{ table.mode }}</span>
              </div>
              <div class="text-sm text-gray-500">
                Connection: {{ connectionName(table.connectionId) }}
                <span v-if="table.whereClause" class="ml-2">| WHERE: {{ table.whereClause }}</span>
              </div>
            </div>
          </div>
          <div class="flex gap-2 items-center">
            <button class="btn btn-secondary btn-sm" @click="toggleExpand(table)">
              {{ expandedTableId === table.id ? '▲ Hide' : '▼ Columns' }}
              ({{ table.columnGenerators?.length ?? 0 }})
            </button>
            <button class="btn btn-secondary btn-sm" @click="openEditTable(table)">Edit</button>
            <button class="btn btn-danger btn-sm" @click="deleteTable(table)">Delete</button>
          </div>
        </div>

        <!-- Column generators expanded -->
        <div v-if="expandedTableId === table.id" class="columns-section">
          <div class="columns-header">
            <span class="font-medium text-sm text-gray-600">Column Generators</span>
            <button class="btn btn-primary btn-sm" @click="openCreateColumn(table)">
              ＋ Add Column
            </button>
          </div>

          <div v-if="!table.columnGenerators?.length" class="text-sm text-gray-400 py-3">
            No column generators configured. Click "Add Column" to add one.
          </div>

          <div v-else class="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Column</th>
                  <th>Generator</th>
                  <th>Parameters</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="col in table.columnGenerators" :key="col.id">
                  <td class="font-medium font-mono">{{ col.columnName }}</td>
                  <td>
                    <span class="badge badge-indigo">{{ col.generatorType }}</span>
                  </td>
                  <td class="text-sm text-gray-500">{{ paramString(col.parameters) }}</td>
                  <td>
                    <div class="flex gap-2">
                      <button class="btn btn-secondary btn-sm" @click="openEditColumn(table, col)">Edit</button>
                      <button class="btn btn-danger btn-sm" @click="deleteColumn(table, col)">Delete</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Table Config Modal -->
    <Modal
      v-if="showTableModal"
      :title="editingTable ? 'Edit Table Configuration' : 'Add Table Configuration'"
      @close="showTableModal = false"
    >
      <form @submit.prevent="submitTableForm">
        <div v-if="tableFormError" class="alert alert-error">{{ tableFormError }}</div>
        <div class="form-group">
          <label class="form-label">Connection *</label>
          <select v-model.number="tableForm.connectionId" class="form-control">
            <option v-for="c in connections" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
          <p v-if="connections.length === 0" class="form-error">
            No connections available. Please add a connection first.
          </p>
        </div>
        <div class="form-group">
          <label class="form-label">Schema</label>
          <input v-model="tableForm.schemaName" type="text" class="form-control" placeholder="public" />
        </div>
        <div class="form-group">
          <label class="form-label">Table Name *</label>
          <input v-model="tableForm.tableName" type="text" class="form-control" placeholder="users" required />
        </div>
        <div class="form-group">
          <label class="form-label">Mode</label>
          <select v-model="tableForm.mode" class="form-control">
            <option v-for="m in tableModes" :key="m" :value="m">{{ m }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">WHERE Clause (optional)</label>
          <input
            v-model="tableForm.whereClause"
            type="text"
            class="form-control"
            placeholder="e.g. created_at > '2020-01-01'"
          />
        </div>
      </form>
      <template #footer>
        <button class="btn btn-secondary" @click="showTableModal = false">Cancel</button>
        <button class="btn btn-primary" :disabled="savingTable || connections.length === 0" @click="submitTableForm">
          <span v-if="savingTable" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          {{ editingTable ? 'Save Changes' : 'Add Table' }}
        </button>
      </template>
    </Modal>

    <!-- Column Generator Modal -->
    <Modal
      v-if="showColumnModal"
      :title="editingColumn ? 'Edit Column Generator' : 'Add Column Generator'"
      @close="showColumnModal = false"
    >
      <form @submit.prevent="submitColumnForm">
        <div v-if="columnFormError" class="alert alert-error">{{ columnFormError }}</div>
        <div class="form-group">
          <label class="form-label">Column Name *</label>
          <input
            v-model="columnForm.columnName"
            type="text"
            class="form-control"
            placeholder="email"
            :readonly="!!editingColumn"
            required
          />
        </div>
        <div class="form-group">
          <label class="form-label">Generator Type</label>
          <select v-model="columnForm.generatorType" class="form-control">
            <option v-for="g in generatorTypes" :key="g" :value="g">{{ g }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Parameters (JSON key=value pairs)</label>
          <div class="params-editor">
            <div
              v-for="(_, key) in columnForm.parameters"
              :key="key"
              class="param-row"
            >
              <span class="param-key">{{ key }}</span>
              <input
                v-model="columnForm.parameters![key]"
                type="text"
                class="form-control"
                :placeholder="`Value for ${key}`"
              />
              <button
                type="button"
                class="btn btn-danger btn-sm"
                @click="delete columnForm.parameters![key]"
              >✕</button>
            </div>
            <button
              type="button"
              class="btn btn-secondary btn-sm mt-2"
              @click="() => {
                const k = prompt('Parameter name:')
                if (k) columnForm.parameters = { ...columnForm.parameters, [k]: '' }
              }"
            >
              ＋ Add Parameter
            </button>
          </div>
        </div>
      </form>
      <template #footer>
        <button class="btn btn-secondary" @click="showColumnModal = false">Cancel</button>
        <button class="btn btn-primary" :disabled="savingColumn" @click="submitColumnForm">
          <span v-if="savingColumn" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          {{ editingColumn ? 'Save Changes' : 'Add Column' }}
        </button>
      </template>
    </Modal>
  </div>
</template>

<style scoped>
.tables-list { display: flex; flex-direction: column; gap: 1rem; }

.table-item-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}
.table-info {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
}
.table-icon { font-size: 1.5rem; flex-shrink: 0; margin-top: 0.1rem; }

.columns-section {
  margin-top: 1.25rem;
  border-top: 1px solid #e5e7eb;
  padding-top: 1rem;
}
.columns-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.params-editor { display: flex; flex-direction: column; gap: 0.5rem; }
.param-row {
  display: grid;
  grid-template-columns: 8rem 1fr auto;
  align-items: center;
  gap: 0.5rem;
}
.param-key {
  font-family: monospace;
  font-size: 0.85rem;
  color: #374151;
  font-weight: 500;
}
</style>
