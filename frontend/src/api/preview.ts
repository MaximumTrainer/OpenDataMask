import apiClient from './client'
import type { GeneratorType } from '@/types'

export interface PreviewSample {
  originalValue: string | null
  maskedValue: string | null
}

export interface ColumnPreviewResult {
  tableName: string
  columnName: string
  generatorType: GeneratorType | null
  samples: PreviewSample[]
}

export async function getColumnPreview(
  workspaceId: number,
  tableName: string,
  columnName: string,
  sampleSize?: number
): Promise<ColumnPreviewResult> {
  const { data } = await apiClient.get<ColumnPreviewResult>(
    `/workspaces/${workspaceId}/tables/${tableName}/columns/${columnName}/preview`,
    { params: sampleSize !== undefined ? { sampleSize } : undefined }
  )
  return data
}
