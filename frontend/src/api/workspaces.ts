import apiClient from './client'
import type { Workspace, WorkspaceRequest } from '@/types'

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
