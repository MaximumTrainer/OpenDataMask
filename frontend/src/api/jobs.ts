import apiClient from './client'
import type { Job, JobRequest, JobLog } from '@/types'

export async function listJobs(workspaceId: number): Promise<Job[]> {
  const { data } = await apiClient.get<Job[]>(`/workspaces/${workspaceId}/jobs`)
  return data
}

export async function getJob(workspaceId: number, jobId: number): Promise<Job> {
  const { data } = await apiClient.get<Job>(`/workspaces/${workspaceId}/jobs/${jobId}`)
  return data
}

export async function createJob(workspaceId: number, payload: JobRequest): Promise<Job> {
  const { data } = await apiClient.post<Job>(`/workspaces/${workspaceId}/jobs`, payload)
  return data
}

export async function cancelJob(workspaceId: number, jobId: number): Promise<Job> {
  const { data } = await apiClient.post<Job>(
    `/workspaces/${workspaceId}/jobs/${jobId}/cancel`
  )
  return data
}

export async function getJobLogs(workspaceId: number, jobId: number): Promise<JobLog[]> {
  const { data } = await apiClient.get<JobLog[]>(
    `/workspaces/${workspaceId}/jobs/${jobId}/logs`
  )
  return data
}
