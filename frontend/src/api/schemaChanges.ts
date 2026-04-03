import apiClient from './client'

export enum SchemaChangeType {
  NEW_TABLE = 'NEW_TABLE',
  DROPPED_TABLE = 'DROPPED_TABLE',
  NEW_COLUMN = 'NEW_COLUMN',
  DROPPED_COLUMN = 'DROPPED_COLUMN',
  TYPE_CHANGED = 'TYPE_CHANGED',
  NULLABILITY_CHANGED = 'NULLABILITY_CHANGED'
}

export enum SchemaChangeStatus {
  UNRESOLVED = 'UNRESOLVED',
  RESOLVED = 'RESOLVED',
  DISMISSED = 'DISMISSED'
}

export interface SchemaChange {
  id: number
  workspaceId: number
  changeType: SchemaChangeType
  tableName: string
  columnName: string | null
  oldValue: string | null
  newValue: string | null
  detectedAt: string
  status: SchemaChangeStatus
}

export interface SchemaChangesResponse {
  exposing: SchemaChange[]
  notifications: SchemaChange[]
}

export async function listSchemaChanges(workspaceId: number): Promise<SchemaChangesResponse> {
  const { data } = await apiClient.get<SchemaChangesResponse>(
    `/workspaces/${workspaceId}/schema-changes`
  )
  return data
}

export async function detectSchemaChanges(workspaceId: number): Promise<SchemaChange[]> {
  const { data } = await apiClient.post<SchemaChange[]>(
    `/workspaces/${workspaceId}/schema-changes/detect`
  )
  return data
}

export async function resolveSchemaChange(workspaceId: number, changeId: number): Promise<void> {
  await apiClient.post(`/workspaces/${workspaceId}/schema-changes/${changeId}/resolve`)
}

export async function dismissSchemaChange(workspaceId: number, changeId: number): Promise<void> {
  await apiClient.post(`/workspaces/${workspaceId}/schema-changes/${changeId}/dismiss`)
}

export async function resolveAllSchemaChanges(workspaceId: number): Promise<void> {
  await apiClient.post(`/workspaces/${workspaceId}/schema-changes/resolve-all`)
}

export async function dismissAllSchemaChanges(workspaceId: number): Promise<void> {
  await apiClient.post(`/workspaces/${workspaceId}/schema-changes/dismiss-all`)
}

export async function updateSchemaChangeSettings(
  workspaceId: number,
  schemaChangeHandling: string
): Promise<void> {
  await apiClient.patch(`/workspaces/${workspaceId}/schema-changes/settings`, {
    schemaChangeHandling
  })
}
