<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppModal from '@/components/AppModal.vue'
import * as mappingApi from '@/api/mapping'
import * as connectionsApi from '@/api/connections'
import type {
  DataConnection,
  TableSchemaInfo,
  ColumnSchemaInfo,
  ColumnMappingEntry,
  CustomDataMapping
} from '@/types'
import { MappingAction, MaskingStrategy, GeneratorType } from '@/types'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

// ── Step state ──
// step 1 = select connection, step 2 = select table, step 3 = configure columns
const currentStep = ref<1 | 2 | 3>(1)

// ── Connections ──
const connections = ref<DataConnection[]>([])
const selectedConnectionId = ref<number | null>(null)
const loadingConnections = ref(false)
const connectionsError = ref('')

// ── Tables ──
const tables = ref<TableSchemaInfo[]>([])
const selectedTableName = ref<string | null>(null)
const loadingSchema = ref(false)
const schemaError = ref('')

// ── Column mappings ──
const columnMappings = ref<Record<string, ColumnMappingEntry>>({})
const currentColumns = computed<ColumnSchemaInfo[]>(() => {
  if (!selectedTableName.value) return []
  return tables.value.find(t => t.tableName === selectedTableName.value)?.columns ?? []
})

// ── Saved mappings list ──
const savedMappings = ref<CustomDataMapping[]>([])
const loadingMappings = ref(false)

// ── Save state ──
const saving = ref(false)
const saveError = ref('')
const saveSuccess = ref(false)

// ── Delete state ──
const showDeleteModal = ref(false)
const mappingToDelete = ref<{ connectionId: number; tableName: string } | null>(null)
const deleting = ref(false)

const fakeGeneratorOptions = [
  GeneratorType.FULL_NAME, GeneratorType.FIRST_NAME, GeneratorType.LAST_NAME,
  GeneratorType.EMAIL, GeneratorType.PHONE, GeneratorType.ADDRESS,
  GeneratorType.STREET_ADDRESS, GeneratorType.CITY, GeneratorType.STATE,
  GeneratorType.ZIP_CODE, GeneratorType.COUNTRY, GeneratorType.SSN,
  GeneratorType.CREDIT_CARD, GeneratorType.DATE, GeneratorType.BIRTH_DATE,
  GeneratorType.UUID, GeneratorType.USERNAME, GeneratorType.PASSWORD,
  GeneratorType.IBAN, GeneratorType.SWIFT_CODE, GeneratorType.MONEY_AMOUNT,
  GeneratorType.IP_ADDRESS, GeneratorType.URL, GeneratorType.COMPANY_NAME,
  GeneratorType.JOB_TITLE, GeneratorType.LOREM
]

async function fetchData() {
  loadingConnections.value = true
  connectionsError.value = ''
  try {
    connections.value = await connectionsApi.listConnections(workspaceId.value)
  } catch {
    connectionsError.value = 'Failed to load connections.'
  } finally {
    loadingConnections.value = false
  }
  await fetchSavedMappings()
}

async function fetchSavedMappings() {
  loadingMappings.value = true
  try {
    savedMappings.value = await mappingApi.listMappings(workspaceId.value)
  } catch {
    // ignore – best effort
  } finally {
    loadingMappings.value = false
  }
}

onMounted(fetchData)

// ── Step 1: Select connection ──
function selectConnection(connId: number) {
  selectedConnectionId.value = connId
  selectedTableName.value = null
  tables.value = []
  columnMappings.value = {}
  currentStep.value = 2
  loadSchema(connId)
}

async function loadSchema(connId: number) {
  loadingSchema.value = true
  schemaError.value = ''
  try {
    const schema = await mappingApi.browseConnectionSchema(workspaceId.value, connId)
    tables.value = schema.tables
  } catch {
    schemaError.value = 'Failed to load schema from the selected connection.'
  } finally {
    loadingSchema.value = false
  }
}

// ── Step 2: Select table ──
async function selectTable(tableName: string) {
  selectedTableName.value = tableName
  columnMappings.value = {}

  // Pre-fill from existing saved mappings for this table
  if (selectedConnectionId.value != null) {
    try {
      const existing = await mappingApi.listMappingsForTable(
        workspaceId.value,
        selectedConnectionId.value,
        tableName
      )
      for (const m of existing) {
        columnMappings.value[m.columnName] = {
          columnName: m.columnName,
          action: m.action,
          maskingStrategy: m.maskingStrategy,
          fakeGeneratorType: m.fakeGeneratorType
        }
      }
    } catch {
      // ignore pre-fill error
    }
  }

  // Ensure every column has a default entry
  const cols = tables.value.find(t => t.tableName === tableName)?.columns ?? []
  for (const col of cols) {
    if (!columnMappings.value[col.name]) {
      columnMappings.value[col.name] = {
        columnName: col.name,
        action: MappingAction.MIGRATE_AS_IS,
        maskingStrategy: null,
        fakeGeneratorType: null
      }
    }
  }

  currentStep.value = 3
}

// ── Step 3: Configure columns ──
function setAction(colName: string, action: MappingAction) {
  const entry = columnMappings.value[colName]
  if (!entry) return
  entry.action = action
  if (action === MappingAction.MIGRATE_AS_IS) {
    entry.maskingStrategy = null
    entry.fakeGeneratorType = null
  } else if (!entry.maskingStrategy) {
    entry.maskingStrategy = MaskingStrategy.FAKE
    entry.fakeGeneratorType = GeneratorType.FULL_NAME
  }
}

function setMaskingStrategy(colName: string, strategy: MaskingStrategy) {
  const entry = columnMappings.value[colName]
  if (!entry) return
  entry.maskingStrategy = strategy
  if (strategy !== MaskingStrategy.FAKE) {
    entry.fakeGeneratorType = null
  } else if (!entry.fakeGeneratorType) {
    entry.fakeGeneratorType = GeneratorType.FULL_NAME
  }
}

async function saveMappings() {
  if (!selectedConnectionId.value || !selectedTableName.value) return
  saving.value = true
  saveError.value = ''
  saveSuccess.value = false
  try {
    const entries = Object.values(columnMappings.value)
    await mappingApi.saveBulkMappings(workspaceId.value, {
      connectionId: selectedConnectionId.value,
      tableName: selectedTableName.value,
      columnMappings: entries
    })
    saveSuccess.value = true
    await fetchSavedMappings()
    // Return to step 2 after a short delay
    setTimeout(() => {
      saveSuccess.value = false
      currentStep.value = 2
    }, 1500)
  } catch {
    saveError.value = 'Failed to save mappings.'
  } finally {
    saving.value = false
  }
}

function goBack() {
  if (currentStep.value === 3) currentStep.value = 2
  else if (currentStep.value === 2) currentStep.value = 1
}

function resetWizard() {
  currentStep.value = 1
  selectedConnectionId.value = null
  selectedTableName.value = null
  tables.value = []
  columnMappings.value = {}
  saveSuccess.value = false
  saveError.value = ''
}

// ── Delete table mappings ──
function openDeleteTableMappings(connectionId: number, tableName: string) {
  mappingToDelete.value = { connectionId, tableName }
  showDeleteModal.value = true
}

async function confirmDeleteTableMappings() {
  if (!mappingToDelete.value) return
  deleting.value = true
  try {
    // Delete each mapping for this table individually
    const toDelete = savedMappings.value.filter(
      m => m.connectionId === mappingToDelete.value!.connectionId &&
           m.tableName === mappingToDelete.value!.tableName
    )
    for (const m of toDelete) {
      await mappingApi.deleteMapping(workspaceId.value, m.id)
    }
    showDeleteModal.value = false
    await fetchSavedMappings()
  } catch {
    alert('Failed to delete mappings.')
  } finally {
    deleting.value = false
  }
}

// ── Computed helpers ──
const savedTableGroups = computed(() => {
  const groups: Record<string, { connectionId: number; tableName: string; mappings: CustomDataMapping[] }> = {}
  for (const m of savedMappings.value) {
    const key = `${m.connectionId}::${m.tableName}`
    if (!groups[key]) {
      groups[key] = { connectionId: m.connectionId, tableName: m.tableName, mappings: [] }
    }
    groups[key].mappings.push(m)
  }
  return Object.values(groups)
})

function connectionName(id: number) {
  return connections.value.find(c => c.id === id)?.name ?? `Connection #${id}`
}

function actionBadgeClass(action: MappingAction) {
  return action === MappingAction.MIGRATE_AS_IS ? 'badge-gray' : 'badge-blue'
}

function strategyLabel(m: CustomDataMapping) {
  if (m.action === MappingAction.MIGRATE_AS_IS) return 'As-Is'
  if (!m.maskingStrategy) return 'MASK'
  if (m.maskingStrategy === MaskingStrategy.FAKE && m.fakeGeneratorType) {
    return `FAKE (${m.fakeGeneratorType})`
  }
  return m.maskingStrategy
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
      <span>Data Mappings</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Custom Data Mapping</h1>
        <p class="text-gray-500 text-sm mt-1">
          Define per-column actions: migrate data as-is or apply masking (FAKE, HASH, NULL)
        </p>
      </div>
      <button v-if="currentStep !== 1" class="btn btn-secondary" @click="resetWizard">
        ＋ New Mapping
      </button>
    </div>

    <!-- Wizard steps indicator -->
    <div class="steps-indicator">
      <div class="step-item" :class="{ active: currentStep === 1, done: currentStep > 1 }">
        <span class="step-num">1</span>
        <span class="step-label">Select Connection</span>
      </div>
      <div class="step-divider" />
      <div class="step-item" :class="{ active: currentStep === 2, done: currentStep > 2 }">
        <span class="step-num">2</span>
        <span class="step-label">Select Table</span>
      </div>
      <div class="step-divider" />
      <div class="step-item" :class="{ active: currentStep === 3 }">
        <span class="step-num">3</span>
        <span class="step-label">Configure Columns</span>
      </div>
    </div>

    <!-- Step 1: Select Connection -->
    <div v-if="currentStep === 1" class="wizard-panel card">
      <h2 class="panel-title">Step 1 — Select a Source Connection</h2>
      <div v-if="loadingConnections" class="loading-overlay"><span class="spinner spinner-lg" /></div>
      <div v-else-if="connectionsError" class="alert alert-error">{{ connectionsError }}</div>
      <div v-else-if="connections.length === 0" class="empty-state">
        <p>No connections found. Add a data connection first.</p>
      </div>
      <div v-else class="connections-grid">
        <button
          v-for="conn in connections"
          :key="conn.id"
          class="conn-card"
          :class="{ selected: selectedConnectionId === conn.id }"
          @click="selectConnection(conn.id)"
        >
          <span class="conn-icon">🔌</span>
          <span class="conn-name">{{ conn.name }}</span>
          <span class="badge badge-gray">{{ conn.type }}</span>
        </button>
      </div>
    </div>

    <!-- Step 2: Select Table -->
    <div v-if="currentStep === 2" class="wizard-panel card">
      <div class="panel-header">
        <h2 class="panel-title">Step 2 — Select a Table</h2>
        <button class="btn btn-secondary btn-sm" @click="goBack">← Back</button>
      </div>
      <div v-if="loadingSchema" class="loading-overlay"><span class="spinner spinner-lg" /></div>
      <div v-else-if="schemaError" class="alert alert-error">{{ schemaError }}</div>
      <div v-else-if="tables.length === 0" class="empty-state">
        <p>No tables found in this connection.</p>
      </div>
      <div v-else class="tables-grid">
        <button
          v-for="table in tables"
          :key="table.tableName"
          class="table-card"
          @click="selectTable(table.tableName)"
        >
          <span class="table-icon">📊</span>
          <span class="table-name">{{ table.tableName }}</span>
          <span class="text-sm text-gray-400">{{ table.columns.length }} columns</span>
        </button>
      </div>
    </div>

    <!-- Step 3: Configure Columns -->
    <div v-if="currentStep === 3" class="wizard-panel card">
      <div class="panel-header">
        <h2 class="panel-title">
          Step 3 — Configure Columns for
          <span class="font-mono">{{ selectedTableName }}</span>
        </h2>
        <button class="btn btn-secondary btn-sm" @click="goBack">← Back</button>
      </div>

      <div v-if="saveError" class="alert alert-error">{{ saveError }}</div>
      <div v-if="saveSuccess" class="alert alert-success">✓ Mappings saved successfully!</div>

      <div class="columns-mapping-table table-wrapper">
        <table>
          <thead>
            <tr>
              <th>Column</th>
              <th>Type</th>
              <th>Action</th>
              <th>Masking Strategy</th>
              <th>Generator (for FAKE)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="col in currentColumns" :key="col.name">
              <td class="font-mono font-medium">{{ col.name }}</td>
              <td class="text-sm text-gray-500">{{ col.type }}</td>
              <td>
                <div class="action-toggle">
                  <button
                    class="toggle-btn"
                    :class="{ active: columnMappings[col.name]?.action === 'MIGRATE_AS_IS' }"
                    @click="setAction(col.name, MappingAction.MIGRATE_AS_IS)"
                  >
                    As-Is
                  </button>
                  <button
                    class="toggle-btn"
                    :class="{ active: columnMappings[col.name]?.action === 'MASK' }"
                    @click="setAction(col.name, MappingAction.MASK)"
                  >
                    Mask
                  </button>
                </div>
              </td>
              <td>
                <select
                  v-if="columnMappings[col.name]?.action === 'MASK'"
                  :value="columnMappings[col.name]?.maskingStrategy ?? ''"
                  class="form-control form-control-sm"
                  @change="setMaskingStrategy(col.name, ($event.target as HTMLSelectElement).value as MaskingStrategy)"
                >
                  <option :value="MaskingStrategy.FAKE">FAKE – Replace with realistic data</option>
                  <option :value="MaskingStrategy.HASH">HASH – Deterministic hash</option>
                  <option :value="MaskingStrategy.NULL">NULL – Remove value</option>
                </select>
                <span v-else class="text-gray-400 text-sm">—</span>
              </td>
              <td>
                <select
                  v-if="columnMappings[col.name]?.action === 'MASK' && columnMappings[col.name]?.maskingStrategy === 'FAKE'"
                  :value="columnMappings[col.name]?.fakeGeneratorType ?? ''"
                  class="form-control form-control-sm"
                  @change="(e) => { if (columnMappings[col.name]) columnMappings[col.name].fakeGeneratorType = (e.target as HTMLSelectElement).value as any }"
                >
                  <option v-for="g in fakeGeneratorOptions" :key="g" :value="g">{{ g }}</option>
                </select>
                <span v-else class="text-gray-400 text-sm">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="save-bar">
        <button class="btn btn-primary" :disabled="saving" @click="saveMappings">
          <span v-if="saving" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          Save Mappings
        </button>
      </div>
    </div>

    <!-- Saved Mappings Summary -->
    <div v-if="currentStep === 1 && savedTableGroups.length > 0" class="saved-section">
      <h2 class="section-title">Saved Mappings</h2>
      <div v-if="loadingMappings" class="loading-overlay"><span class="spinner" /></div>
      <div v-else class="saved-list">
        <div
          v-for="group in savedTableGroups"
          :key="`${group.connectionId}::${group.tableName}`"
          class="saved-group card"
        >
          <div class="saved-group-header">
            <div>
              <span class="font-semibold">{{ group.tableName }}</span>
              <span class="text-sm text-gray-500 ml-2">
                via {{ connectionName(group.connectionId) }}
              </span>
            </div>
            <div class="flex gap-2">
              <button
                class="btn btn-secondary btn-sm"
                @click="() => { selectConnection(group.connectionId); }"
              >
                Edit
              </button>
              <button
                class="btn btn-danger btn-sm"
                @click="openDeleteTableMappings(group.connectionId, group.tableName)"
              >
                Delete
              </button>
            </div>
          </div>
          <div class="table-wrapper mt-2">
            <table>
              <thead>
                <tr>
                  <th>Column</th>
                  <th>Action</th>
                  <th>Strategy</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="m in group.mappings" :key="m.id">
                  <td class="font-mono">{{ m.columnName }}</td>
                  <td>
                    <span class="badge" :class="actionBadgeClass(m.action)">{{ m.action }}</span>
                  </td>
                  <td class="text-sm">{{ strategyLabel(m) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete confirmation modal -->
    <AppModal
      v-if="showDeleteModal"
      title="Delete Table Mappings"
      @close="showDeleteModal = false"
    >
      <p>
        Delete all mappings for
        <strong>{{ mappingToDelete?.tableName }}</strong>?
        This cannot be undone.
      </p>
      <template #footer>
        <button class="btn btn-secondary" @click="showDeleteModal = false">Cancel</button>
        <button class="btn btn-danger" :disabled="deleting" @click="confirmDeleteTableMappings">
          <span v-if="deleting" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          Delete
        </button>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
/* ── Wizard steps indicator ── */
.steps-indicator {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 1.5rem;
  padding: 1rem 1.5rem;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 0.5rem;
}
.step-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: #9ca3af;
}
.step-item.active { color: #2563eb; font-weight: 600; }
.step-item.done { color: #16a34a; }
.step-num {
  width: 1.75rem;
  height: 1.75rem;
  border-radius: 50%;
  background: #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 700;
}
.step-item.active .step-num { background: #2563eb; color: #fff; }
.step-item.done .step-num { background: #16a34a; color: #fff; }
.step-divider { flex: 1; height: 2px; background: #e5e7eb; margin: 0 0.75rem; }

/* ── Wizard panel ── */
.wizard-panel { padding: 1.5rem; }
.panel-title { font-size: 1.125rem; font-weight: 600; margin-bottom: 1rem; }
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1rem;
}

/* ── Connections grid ── */
.connections-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 0.75rem;
}
.conn-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1.25rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.5rem;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
  text-align: center;
}
.conn-card:hover { border-color: #2563eb; box-shadow: 0 0 0 3px #dbeafe; }
.conn-card.selected { border-color: #2563eb; background: #eff6ff; }
.conn-icon { font-size: 2rem; }
.conn-name { font-weight: 600; font-size: 0.95rem; }

/* ── Tables grid ── */
.tables-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 0.75rem;
}
.table-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.35rem;
  padding: 1rem;
  border: 2px solid #e5e7eb;
  border-radius: 0.5rem;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.table-card:hover { border-color: #7c3aed; box-shadow: 0 0 0 3px #ede9fe; }
.table-icon { font-size: 1.5rem; }
.table-name { font-weight: 600; }

/* ── Action toggle ── */
.action-toggle {
  display: flex;
  border: 1px solid #d1d5db;
  border-radius: 0.375rem;
  overflow: hidden;
  width: fit-content;
}
.toggle-btn {
  padding: 0.25rem 0.625rem;
  font-size: 0.8125rem;
  background: #fff;
  color: #374151;
  border: none;
  cursor: pointer;
  transition: background 0.1s, color 0.1s;
}
.toggle-btn + .toggle-btn { border-left: 1px solid #d1d5db; }
.toggle-btn.active { background: #2563eb; color: #fff; }

/* ── Column mapping table ── */
.columns-mapping-table { overflow-x: auto; }
.form-control-sm { padding: 0.25rem 0.5rem; font-size: 0.875rem; }

/* ── Save bar ── */
.save-bar { margin-top: 1.25rem; display: flex; justify-content: flex-end; }

/* ── Alert success ── */
.alert-success {
  background: #f0fdf4;
  border: 1px solid #86efac;
  color: #166534;
  border-radius: 0.375rem;
  padding: 0.625rem 1rem;
  margin-bottom: 1rem;
}

/* ── Saved section ── */
.saved-section { margin-top: 2rem; }
.section-title { font-size: 1.125rem; font-weight: 600; margin-bottom: 1rem; }
.saved-list { display: flex; flex-direction: column; gap: 1rem; }
.saved-group { padding: 1rem 1.25rem; }
.saved-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
