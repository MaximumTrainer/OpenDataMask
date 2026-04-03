import apiClient from './client'

export enum SubsetLimitType {
  PERCENTAGE = 'PERCENTAGE',
  ROW_COUNT = 'ROW_COUNT',
  ALL = 'ALL'
}

export interface SubsetTableConfig {
  id: number
  workspaceId: number
  tableName: string
  limitType: SubsetLimitType
  limitValue: number
  isTargetTable: boolean
  isLookupTable: boolean
}

export interface SubsetTableConfigRequest {
  tableName: string
  limitType?: SubsetLimitType
  limitValue?: number
  isTargetTable?: boolean
  isLookupTable?: boolean
}

export async function listSubsetConfigs(workspaceId: number): Promise<SubsetTableConfig[]> {
  const { data } = await apiClient.get<SubsetTableConfig[]>(
    `/workspaces/${workspaceId}/subset-config`
  )
  return data
}

export async function createSubsetConfig(
  workspaceId: number,
  payload: SubsetTableConfigRequest
): Promise<SubsetTableConfig> {
  const { data } = await apiClient.post<SubsetTableConfig>(
    `/workspaces/${workspaceId}/subset-config`,
    payload
  )
  return data
}

export async function updateSubsetConfig(
  workspaceId: number,
  cfgId: number,
  payload: SubsetTableConfigRequest
): Promise<SubsetTableConfig> {
  const { data } = await apiClient.put<SubsetTableConfig>(
    `/workspaces/${workspaceId}/subset-config/${cfgId}`,
    payload
  )
  return data
}

export async function deleteSubsetConfig(workspaceId: number, cfgId: number): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/subset-config/${cfgId}`)
}
