import apiClient from './client'

export interface TableProtectionSummary {
  name: string
  atRisk: number
  protected: number
  notSensitive: number
}

export interface PrivacyHubSummary {
  atRiskCount: number
  protectedCount: number
  notSensitiveCount: number
  tables: TableProtectionSummary[]
  recommendationsCount: number
}

export interface PrivacyRecommendation {
  tableName: string
  columnName: string
  sensitivityType: string
  confidenceLevel: string
  recommendedGenerator: string
}

export enum PrivacyReportType {
  CURRENT_CONFIG = 'CURRENT_CONFIG',
  JOB_EXECUTION = 'JOB_EXECUTION'
}

export interface PrivacyReport {
  id: number
  workspaceId: number
  generatedAt: string
  reportJson: string
  reportType: PrivacyReportType
}

export async function getPrivacyHubSummary(workspaceId: number): Promise<PrivacyHubSummary> {
  const { data } = await apiClient.get<PrivacyHubSummary>(`/workspaces/${workspaceId}/privacy-hub`)
  return data
}

export async function getPrivacyRecommendations(workspaceId: number): Promise<PrivacyRecommendation[]> {
  const { data } = await apiClient.get<PrivacyRecommendation[]>(
    `/workspaces/${workspaceId}/privacy-hub/recommendations`
  )
  return data
}

export async function applyPrivacyRecommendations(workspaceId: number): Promise<{ applied: number }> {
  const { data } = await apiClient.post<{ applied: number }>(
    `/workspaces/${workspaceId}/privacy-hub/recommendations/apply`
  )
  return data
}

export async function getPrivacyReport(workspaceId: number): Promise<PrivacyReport> {
  const { data } = await apiClient.get<PrivacyReport>(`/workspaces/${workspaceId}/privacy-report`)
  return data
}

export async function downloadPrivacyReport(workspaceId: number): Promise<Blob> {
  const { data } = await apiClient.get(`/workspaces/${workspaceId}/privacy-report/download`, {
    responseType: 'blob'
  })
  return data
}
