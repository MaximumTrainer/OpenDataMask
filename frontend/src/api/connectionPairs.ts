import apiClient from './client'
import type { ConnectionPair, ConnectionPairRequest } from '@/types'

export async function listConnectionPairs(workspaceId: number): Promise<ConnectionPair[]> {
  const { data } = await apiClient.get<ConnectionPair[]>(
    `/workspaces/${workspaceId}/connection-pairs`
  )
  return data
}

export async function createConnectionPair(
  workspaceId: number,
  payload: ConnectionPairRequest
): Promise<ConnectionPair> {
  const { data } = await apiClient.post<ConnectionPair>(
    `/workspaces/${workspaceId}/connection-pairs`,
    payload
  )
  return data
}

export async function updateConnectionPair(
  workspaceId: number,
  pairId: number,
  payload: ConnectionPairRequest
): Promise<ConnectionPair> {
  const { data } = await apiClient.put<ConnectionPair>(
    `/workspaces/${workspaceId}/connection-pairs/${pairId}`,
    payload
  )
  return data
}

export async function deleteConnectionPair(workspaceId: number, pairId: number): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/connection-pairs/${pairId}`)
}
