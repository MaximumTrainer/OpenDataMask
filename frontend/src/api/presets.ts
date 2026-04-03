import apiClient from './client'
import type { GeneratorType } from '@/types'

export interface GeneratorPresetResponse {
  id: number
  name: string
  generatorType: GeneratorType
  generatorParams: string | null
  workspaceId: number | null
  isSystem: boolean
  createdAt: string
}

export interface GeneratorPresetRequest {
  name: string
  generatorType: GeneratorType
  generatorParams?: string | null
}

export async function listSystemPresets(): Promise<GeneratorPresetResponse[]> {
  const { data } = await apiClient.get<GeneratorPresetResponse[]>('/generator-presets')
  return data
}

export async function listWorkspacePresets(workspaceId: number): Promise<GeneratorPresetResponse[]> {
  const { data } = await apiClient.get<GeneratorPresetResponse[]>(
    `/workspaces/${workspaceId}/generator-presets`
  )
  return data
}

export async function createWorkspacePreset(
  workspaceId: number,
  payload: GeneratorPresetRequest
): Promise<GeneratorPresetResponse> {
  const { data } = await apiClient.post<GeneratorPresetResponse>(
    `/workspaces/${workspaceId}/generator-presets`,
    payload
  )
  return data
}

export async function updateWorkspacePreset(
  workspaceId: number,
  presetId: number,
  payload: GeneratorPresetRequest
): Promise<GeneratorPresetResponse> {
  const { data } = await apiClient.put<GeneratorPresetResponse>(
    `/workspaces/${workspaceId}/generator-presets/${presetId}`,
    payload
  )
  return data
}

export async function deleteWorkspacePreset(workspaceId: number, presetId: number): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/generator-presets/${presetId}`)
}

export async function applyPresetToColumn(
  workspaceId: number,
  tableName: string,
  columnName: string,
  presetId: number
): Promise<unknown> {
  const { data } = await apiClient.post(
    `/workspaces/${workspaceId}/tables/${tableName}/columns/${columnName}/generator/preset`,
    { presetId }
  )
  return data
}
