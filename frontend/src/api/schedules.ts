import apiClient from './client'

export enum ScheduledJobType {
  FULL_GENERATION = 'FULL_GENERATION',
  UPSERT = 'UPSERT'
}

export interface JobSchedule {
  id: number
  workspaceId: number
  cronExpression: string
  enabled: boolean
  jobType: ScheduledJobType
  nextRunAt: string | null
  lastRunAt: string | null
  lastJobId: number | null
  createdAt: string
}

export interface JobScheduleRequest {
  cronExpression: string
  enabled?: boolean
  jobType?: ScheduledJobType
}

export interface CronValidationResult {
  valid: boolean
  nextRun?: string
  error?: string
}

export async function listSchedules(workspaceId: number): Promise<JobSchedule[]> {
  const { data } = await apiClient.get<JobSchedule[]>(`/workspaces/${workspaceId}/schedules`)
  return data
}

export async function createSchedule(
  workspaceId: number,
  payload: JobScheduleRequest
): Promise<JobSchedule> {
  const { data } = await apiClient.post<JobSchedule>(
    `/workspaces/${workspaceId}/schedules`,
    payload
  )
  return data
}

export async function updateSchedule(
  workspaceId: number,
  scheduleId: number,
  payload: JobScheduleRequest
): Promise<JobSchedule> {
  const { data } = await apiClient.put<JobSchedule>(
    `/workspaces/${workspaceId}/schedules/${scheduleId}`,
    payload
  )
  return data
}

export async function deleteSchedule(workspaceId: number, scheduleId: number): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/schedules/${scheduleId}`)
}

export async function validateCronExpression(
  workspaceId: number,
  cronExpression: string
): Promise<CronValidationResult> {
  const { data } = await apiClient.post<CronValidationResult>(
    `/workspaces/${workspaceId}/schedules/validate-cron`,
    { cronExpression }
  )
  return data
}
