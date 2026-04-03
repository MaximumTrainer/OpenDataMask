import apiClient from './client'

export interface ColumnComment {
  id: number
  workspaceId: number
  tableName: string
  columnName: string
  userId: number
  comment: string
  createdAt: string
}

export interface ColumnCommentRequest {
  comment: string
}

export async function listColumnComments(
  workspaceId: number,
  tableName: string,
  columnName: string
): Promise<ColumnComment[]> {
  const { data } = await apiClient.get<ColumnComment[]>(
    `/workspaces/${workspaceId}/tables/${tableName}/columns/${columnName}/comments`
  )
  return data
}

export async function createColumnComment(
  workspaceId: number,
  tableName: string,
  columnName: string,
  payload: ColumnCommentRequest
): Promise<ColumnComment> {
  const { data } = await apiClient.post<ColumnComment>(
    `/workspaces/${workspaceId}/tables/${tableName}/columns/${columnName}/comments`,
    payload
  )
  return data
}

export async function deleteColumnComment(
  workspaceId: number,
  tableName: string,
  columnName: string,
  commentId: number
): Promise<void> {
  await apiClient.delete(
    `/workspaces/${workspaceId}/tables/${tableName}/columns/${columnName}/comments/${commentId}`
  )
}
