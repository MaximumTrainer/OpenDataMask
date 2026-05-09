<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import AppModal from '@/components/AppModal.vue'
import * as schedulesApi from '@/api/schedules'
import { ScheduledJobType } from '@/api/schedules'
import type { JobSchedule, JobScheduleRequest, CronValidationResult } from '@/api/schedules'

const route = useRoute()
const workspaceId = computed(() => Number(route.params.id))

const schedules = ref<JobSchedule[]>([])
const loading = ref(false)
const error = ref('')
const showModal = ref(false)
const editingSchedule = ref<JobSchedule | null>(null)
const saving = ref(false)
const formError = ref('')
const deleting = ref<number | null>(null)
const cronValidation = ref<CronValidationResult | null>(null)
const validating = ref(false)

const defaultForm = (): JobScheduleRequest => ({
  cronExpression: '0 2 * * *',
  enabled: true,
  jobType: ScheduledJobType.FULL_GENERATION
})

const form = ref<JobScheduleRequest>(defaultForm())

async function fetchSchedules() {
  loading.value = true
  error.value = ''
  try {
    schedules.value = await schedulesApi.listSchedules(workspaceId.value)
  } catch {
    error.value = 'Failed to load schedules.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchSchedules)

function openCreate() {
  editingSchedule.value = null
  form.value = defaultForm()
  cronValidation.value = null
  formError.value = ''
  showModal.value = true
}

function openEdit(schedule: JobSchedule) {
  editingSchedule.value = schedule
  form.value = {
    cronExpression: schedule.cronExpression,
    enabled: schedule.enabled,
    jobType: schedule.jobType
  }
  cronValidation.value = null
  formError.value = ''
  showModal.value = true
}

async function validateCron() {
  if (!form.value.cronExpression.trim()) return
  validating.value = true
  cronValidation.value = null
  try {
    cronValidation.value = await schedulesApi.validateCronExpression(workspaceId.value, form.value.cronExpression)
  } catch {
    cronValidation.value = { valid: false, error: 'Validation failed.' }
  } finally {
    validating.value = false
  }
}

async function handleSave() {
  if (!form.value.cronExpression.trim()) {
    formError.value = 'Cron expression is required.'
    return
  }
  saving.value = true
  formError.value = ''
  try {
    if (editingSchedule.value) {
      const updated = await schedulesApi.updateSchedule(workspaceId.value, editingSchedule.value.id, form.value)
      const idx = schedules.value.findIndex((s) => s.id === editingSchedule.value!.id)
      if (idx !== -1) schedules.value[idx] = updated
    } else {
      const created = await schedulesApi.createSchedule(workspaceId.value, form.value)
      schedules.value.push(created)
    }
    showModal.value = false
  } catch {
    formError.value = 'Failed to save schedule.'
  } finally {
    saving.value = false
  }
}

async function handleDelete(schedule: JobSchedule) {
  if (!confirm(`Delete this schedule (${schedule.cronExpression})?`)) return
  deleting.value = schedule.id
  try {
    await schedulesApi.deleteSchedule(workspaceId.value, schedule.id)
    schedules.value = schedules.value.filter((s) => s.id !== schedule.id)
  } catch {
    alert('Failed to delete schedule.')
  } finally {
    deleting.value = null
  }
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
      <span>Schedules</span>
    </div>

    <div class="page-header">
      <div>
        <h1>Job Schedules</h1>
        <p class="text-gray-500 text-sm mt-1">Automate masking jobs with cron-based schedules</p>
      </div>
      <button class="btn btn-primary" @click="openCreate">+ Add Schedule</button>
    </div>

    <div v-if="loading" class="loading-overlay"><span class="spinner spinner-lg" /></div>
    <div v-else-if="error" class="alert alert-error">{{ error }}</div>

    <div v-else-if="schedules.length === 0" class="empty-state">
      <div class="empty-state-icon">🕐</div>
      <h3>No schedules configured</h3>
      <p>Add a schedule to run masking jobs automatically on a recurring basis.</p>
      <button class="btn btn-primary mt-4" @click="openCreate">+ Add Schedule</button>
    </div>

    <div v-else class="card">
      <table class="data-table">
        <thead>
          <tr>
            <th>Cron Expression</th>
            <th>Job Type</th>
            <th>Status</th>
            <th>Last Run</th>
            <th>Next Run</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in schedules" :key="s.id">
            <td class="font-mono">{{ s.cronExpression }}</td>
            <td>{{ s.jobType.replace('_', ' ') }}</td>
            <td>
              <span :class="s.enabled ? 'badge badge-green' : 'badge badge-gray'">
                {{ s.enabled ? 'Enabled' : 'Disabled' }}
              </span>
            </td>
            <td>{{ formatDate(s.lastRunAt) }}</td>
            <td>{{ formatDate(s.nextRunAt) }}</td>
            <td>
              <div class="flex gap-2">
                <button class="btn btn-secondary btn-sm" @click="openEdit(s)">Edit</button>
                <button
                  class="btn btn-danger btn-sm"
                  :disabled="deleting === s.id"
                  @click="handleDelete(s)"
                >Delete</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <AppModal v-if="showModal" :title="editingSchedule ? 'Edit Schedule' : 'Add Schedule'" @close="showModal = false">
      <form @submit.prevent="handleSave">
        <div v-if="formError" class="alert alert-error">{{ formError }}</div>

        <div class="form-group">
          <label class="form-label">Cron Expression *</label>
          <div class="flex gap-2">
            <input
              v-model="form.cronExpression"
              type="text"
              class="form-control"
              placeholder="0 2 * * *"
              @blur="validateCron"
            />
            <button type="button" class="btn btn-secondary" :disabled="validating" @click="validateCron">
              {{ validating ? '…' : 'Validate' }}
            </button>
          </div>
          <p v-if="cronValidation?.valid" class="text-sm text-green-600 mt-1">
            ✓ Valid — next run: {{ formatDate(cronValidation.nextRun ?? null) }}
          </p>
          <p v-else-if="cronValidation && !cronValidation.valid" class="text-sm text-red-600 mt-1">
            ✗ {{ cronValidation.error ?? 'Invalid expression' }}
          </p>
          <p class="text-xs text-gray-400 mt-1">Format: minute hour day-of-month month day-of-week</p>
        </div>

        <div class="form-group">
          <label class="form-label">Job Type</label>
          <select v-model="form.jobType" class="form-control">
            <option :value="ScheduledJobType.FULL_GENERATION">Full Generation</option>
            <option :value="ScheduledJobType.UPSERT">Upsert</option>
          </select>
        </div>

        <div class="form-group">
          <label class="checkbox-label">
            <input v-model="form.enabled" type="checkbox" />
            Enabled
          </label>
        </div>
      </form>
      <template #footer>
        <button class="btn btn-secondary" @click="showModal = false">Cancel</button>
        <button class="btn btn-primary" :disabled="saving" @click="handleSave">
          <span v-if="saving" class="spinner" style="width:.9rem;height:.9rem;border-width:2px;" />
          {{ editingSchedule ? 'Save Changes' : 'Add Schedule' }}
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
.badge-green { background: #dcfce7; color: #15803d; }
.badge-gray { background: #f3f4f6; color: #4b5563; }
.checkbox-label { display: flex; align-items: center; gap: 0.5rem; cursor: pointer; }
</style>
