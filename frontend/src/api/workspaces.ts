import apiClient from './client'
import type { Workspace, WorkspaceRequest, WorkspaceStats, WorkspaceConfigDto, CustomMappingDto } from '@/types'

export async function listWorkspaces(): Promise<Workspace[]> {
  const { data } = await apiClient.get<Workspace[]>('/workspaces')
  return data
}

export async function getWorkspace(id: number): Promise<Workspace> {
  const { data } = await apiClient.get<Workspace>(`/workspaces/${id}`)
  return data
}

export async function createWorkspace(payload: WorkspaceRequest): Promise<Workspace> {
  const { data } = await apiClient.post<Workspace>('/workspaces', payload)
  return data
}

export async function updateWorkspace(id: number, payload: WorkspaceRequest): Promise<Workspace> {
  const { data } = await apiClient.put<Workspace>(`/workspaces/${id}`, payload)
  return data
}

export async function deleteWorkspace(id: number): Promise<void> {
  await apiClient.delete(`/workspaces/${id}`)
}

export async function exportWorkspace(id: number): Promise<Blob> {
  const { data } = await apiClient.get(`/workspaces/${id}/export`, { responseType: 'blob' })
  return data
}

export async function importWorkspace(
  id: number,
  config: WorkspaceConfigDto
): Promise<{ status: string; version: string }> {
  const { data } = await apiClient.post(`/workspaces/${id}/import`, config)
  return data
}

export async function importCustomMapping(
  id: number,
  mapping: CustomMappingDto
): Promise<{ status: string; project: string }> {
  const { data } = await apiClient.post(`/workspaces/${id}/import-mapping`, mapping)
  return data
}

export async function getWorkspaceStats(id: number): Promise<WorkspaceStats> {
  const { data } = await apiClient.get<WorkspaceStats>(`/workspaces/${id}/stats`)
  return data
}
