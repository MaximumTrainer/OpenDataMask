import apiClient from './client'
import type {
  CustomSensitivityRule,
  CustomSensitivityRuleRequest,
  CustomRulePreviewRequest,
  CustomRulePreviewResult
} from '@/types'

export async function listSensitivityRules(): Promise<CustomSensitivityRule[]> {
  const { data } = await apiClient.get<CustomSensitivityRule[]>('/sensitivity-rules')
  return data
}

export async function getSensitivityRule(id: number): Promise<CustomSensitivityRule> {
  const { data } = await apiClient.get<CustomSensitivityRule>(`/sensitivity-rules/${id}`)
  return data
}

export async function createSensitivityRule(
  payload: CustomSensitivityRuleRequest
): Promise<CustomSensitivityRule> {
  const { data } = await apiClient.post<CustomSensitivityRule>('/sensitivity-rules', payload)
  return data
}

export async function updateSensitivityRule(
  id: number,
  payload: CustomSensitivityRuleRequest
): Promise<CustomSensitivityRule> {
  const { data } = await apiClient.put<CustomSensitivityRule>(`/sensitivity-rules/${id}`, payload)
  return data
}

export async function deleteSensitivityRule(id: number): Promise<void> {
  await apiClient.delete(`/sensitivity-rules/${id}`)
}

export async function previewSensitivityRule(
  payload: CustomRulePreviewRequest
): Promise<CustomRulePreviewResult[]> {
  const { data } = await apiClient.post<CustomRulePreviewResult[]>(
    '/sensitivity-rules/preview',
    payload
  )
  return data
}
