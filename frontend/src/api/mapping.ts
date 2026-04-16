import apiClient from './client'
import type {
  CustomDataMapping,
  CustomDataMappingRequest,
  BulkCustomDataMappingRequest,
  ConnectionSchemaResponse
} from '@/types'

// ── Custom Data Mappings ──────────────────────────────────────────────────

export async function listMappings(workspaceId: number): Promise<CustomDataMapping[]> {
  const { data } = await apiClient.get<CustomDataMapping[]>(
    `/workspaces/${workspaceId}/mappings`
  )
  return data
}

export async function listMappingsForTable(
  workspaceId: number,
  connectionId: number,
  tableName: string
): Promise<CustomDataMapping[]> {
  const { data } = await apiClient.get<CustomDataMapping[]>(
    `/workspaces/${workspaceId}/mappings`,
    { params: { connectionId, tableName } }
  )
  return data
}

export async function createMapping(
  workspaceId: number,
  payload: CustomDataMappingRequest
): Promise<CustomDataMapping> {
  const { data } = await apiClient.post<CustomDataMapping>(
    `/workspaces/${workspaceId}/mappings`,
    payload
  )
  return data
}

export async function updateMapping(
  workspaceId: number,
  mappingId: number,
  payload: CustomDataMappingRequest
): Promise<CustomDataMapping> {
  const { data } = await apiClient.put<CustomDataMapping>(
    `/workspaces/${workspaceId}/mappings/${mappingId}`,
    payload
  )
  return data
}

export async function deleteMapping(workspaceId: number, mappingId: number): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/mappings/${mappingId}`)
}

export async function saveBulkMappings(
  workspaceId: number,
  payload: BulkCustomDataMappingRequest
): Promise<CustomDataMapping[]> {
  const { data } = await apiClient.post<CustomDataMapping[]>(
    `/workspaces/${workspaceId}/mappings/bulk`,
    payload
  )
  return data
}

// ── Connection Schema Browsing ────────────────────────────────────────────

export async function browseConnectionSchema(
  workspaceId: number,
  connectionId: number
): Promise<ConnectionSchemaResponse> {
  const { data } = await apiClient.get<ConnectionSchemaResponse>(
    `/workspaces/${workspaceId}/connections/${connectionId}/schema`
  )
  return data
}
