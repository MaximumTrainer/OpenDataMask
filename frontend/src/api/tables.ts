import apiClient from './client'
import type {
  TableConfiguration,
  TableConfigurationRequest,
  ColumnGenerator,
  ColumnGeneratorRequest
} from '@/types'

// ── Table Configurations ──────────────────────────────────────────────────

export async function listTableConfigs(workspaceId: number): Promise<TableConfiguration[]> {
  const { data } = await apiClient.get<TableConfiguration[]>(
    `/workspaces/${workspaceId}/tables`
  )
  return data
}

export async function getTableConfig(
  workspaceId: number,
  tableId: number
): Promise<TableConfiguration> {
  const { data } = await apiClient.get<TableConfiguration>(
    `/workspaces/${workspaceId}/tables/${tableId}`
  )
  return data
}

export async function createTableConfig(
  workspaceId: number,
  payload: TableConfigurationRequest
): Promise<TableConfiguration> {
  const { data } = await apiClient.post<TableConfiguration>(
    `/workspaces/${workspaceId}/tables`,
    payload
  )
  return data
}

export async function updateTableConfig(
  workspaceId: number,
  tableId: number,
  payload: TableConfigurationRequest
): Promise<TableConfiguration> {
  const { data } = await apiClient.put<TableConfiguration>(
    `/workspaces/${workspaceId}/tables/${tableId}`,
    payload
  )
  return data
}

export async function deleteTableConfig(
  workspaceId: number,
  tableId: number
): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/tables/${tableId}`)
}

// ── Column Generators ─────────────────────────────────────────────────────

export async function listColumnGenerators(
  workspaceId: number,
  tableId: number
): Promise<ColumnGenerator[]> {
  const { data } = await apiClient.get<ColumnGenerator[]>(
    `/workspaces/${workspaceId}/tables/${tableId}/generators`
  )
  return data
}

export async function createColumnGenerator(
  workspaceId: number,
  tableId: number,
  payload: ColumnGeneratorRequest
): Promise<ColumnGenerator> {
  const { data } = await apiClient.post<ColumnGenerator>(
    `/workspaces/${workspaceId}/tables/${tableId}/generators`,
    payload
  )
  return data
}

export async function updateColumnGenerator(
  workspaceId: number,
  tableId: number,
  columnId: number,
  payload: ColumnGeneratorRequest
): Promise<ColumnGenerator> {
  const { data } = await apiClient.put<ColumnGenerator>(
    `/workspaces/${workspaceId}/tables/${tableId}/generators/${columnId}`,
    payload
  )
  return data
}

export async function deleteColumnGenerator(
  workspaceId: number,
  tableId: number,
  columnId: number
): Promise<void> {
  await apiClient.delete(
    `/workspaces/${workspaceId}/tables/${tableId}/generators/${columnId}`
  )
}
