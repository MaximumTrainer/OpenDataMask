import apiClient from './client'
import type { Job, JobRequest, JobLog, JobTableStats } from '@/types'

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

export async function getJobTableStats(workspaceId: number, jobId: number): Promise<JobTableStats[]> {
  const { data } = await apiClient.get<JobTableStats[]>(
    `/workspaces/${workspaceId}/jobs/${jobId}/stats`
  )
  return data
}

export function connectJobStream(
  workspaceId: number,
  jobId: number,
  onProgress: (event: { tableName?: string; status: string; rowsProcessed: number; tablesProcessed: number; tablesTotal: number; message?: string }) => void,
  onDone: () => void
): EventSource {
  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'
  const token = localStorage.getItem('token')
  const url = `${baseUrl}/workspaces/${workspaceId}/jobs/${jobId}/stream${token ? `?token=${encodeURIComponent(token)}` : ''}`
  const es = new EventSource(url)
  es.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      onProgress(data)
      if (data.status === 'COMPLETED' || data.status === 'FAILED' || data.status === 'CANCELLED') {
        es.close()
        onDone()
      }
    } catch {
      // ignore malformed events
    }
  }
  es.onerror = () => {
    es.close()
    onDone()
  }
  return es
}
