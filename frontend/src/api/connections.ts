import apiClient from './client'
import type {
  DataConnection,
  DataConnectionRequest,
  ConnectionTestResult
} from '@/types'

export async function listConnections(workspaceId: number): Promise<DataConnection[]> {
  const { data } = await apiClient.get<DataConnection[]>(
    `/workspaces/${workspaceId}/connections`
  )
  return data
}

export async function getConnection(
  workspaceId: number,
  connectionId: number
): Promise<DataConnection> {
  const { data } = await apiClient.get<DataConnection>(
    `/workspaces/${workspaceId}/connections/${connectionId}`
  )
  return data
}

export async function createConnection(
  workspaceId: number,
  payload: DataConnectionRequest
): Promise<DataConnection> {
  const { data } = await apiClient.post<DataConnection>(
    `/workspaces/${workspaceId}/connections`,
    payload
  )
  return data
}

export async function updateConnection(
  workspaceId: number,
  connectionId: number,
  payload: DataConnectionRequest
): Promise<DataConnection> {
  const { data } = await apiClient.put<DataConnection>(
    `/workspaces/${workspaceId}/connections/${connectionId}`,
    payload
  )
  return data
}

export async function deleteConnection(
  workspaceId: number,
  connectionId: number
): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/connections/${connectionId}`)
}

export async function testConnection(
  workspaceId: number,
  connectionId: number
): Promise<ConnectionTestResult> {
  const { data } = await apiClient.post<ConnectionTestResult>(
    `/workspaces/${workspaceId}/connections/${connectionId}/test`
  )
  return data
}
