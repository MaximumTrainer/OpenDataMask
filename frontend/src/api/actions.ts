import apiClient from './client'
import type { PostJobAction, PostJobActionRequest } from '@/types'

export async function listActions(workspaceId: number): Promise<PostJobAction[]> {
  const { data } = await apiClient.get<PostJobAction[]>(`/workspaces/${workspaceId}/actions`)
  return data
}

export async function createAction(
  workspaceId: number,
  payload: PostJobActionRequest
): Promise<PostJobAction> {
  const { data } = await apiClient.post<PostJobAction>(
    `/workspaces/${workspaceId}/actions`,
    payload
  )
  return data
}

export async function updateAction(
  workspaceId: number,
  actionId: number,
  payload: PostJobActionRequest
): Promise<PostJobAction> {
  const { data } = await apiClient.put<PostJobAction>(
    `/workspaces/${workspaceId}/actions/${actionId}`,
    payload
  )
  return data
}

export async function deleteAction(workspaceId: number, actionId: number): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/actions/${actionId}`)
}
