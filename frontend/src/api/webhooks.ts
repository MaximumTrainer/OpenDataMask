import apiClient from './client'

export enum WebhookTriggerType {
  DATA_GENERATION = 'DATA_GENERATION',
  SCHEMA_CHANGE = 'SCHEMA_CHANGE'
}

export enum WebhookType {
  CUSTOM = 'CUSTOM',
  GITHUB_WORKFLOW = 'GITHUB_WORKFLOW'
}

export interface Webhook {
  id: number
  workspaceId: number
  name: string
  enabled: boolean
  triggerType: WebhookTriggerType
  triggerEvents: string[]
  webhookType: WebhookType
  url: string | null
  bypassSsl: boolean
  headersJson: string | null
  bodyTemplate: string | null
  githubOwner: string | null
  githubRepo: string | null
  githubWorkflow: string | null
  githubInputsJson: string | null
  createdAt: string
}

export interface WebhookRequest {
  name: string
  enabled?: boolean
  triggerType: WebhookTriggerType
  triggerEvents?: string[]
  webhookType?: WebhookType
  url?: string | null
  bypassSsl?: boolean
  headersJson?: string | null
  bodyTemplate?: string | null
  githubOwner?: string | null
  githubRepo?: string | null
  githubPat?: string | null
  githubWorkflow?: string | null
  githubInputsJson?: string | null
}

export async function listWebhooks(workspaceId: number): Promise<Webhook[]> {
  const { data } = await apiClient.get<Webhook[]>(`/workspaces/${workspaceId}/webhooks`)
  return data
}

export async function createWebhook(workspaceId: number, payload: WebhookRequest): Promise<Webhook> {
  const { data } = await apiClient.post<Webhook>(`/workspaces/${workspaceId}/webhooks`, payload)
  return data
}

export async function updateWebhook(
  workspaceId: number,
  webhookId: number,
  payload: WebhookRequest
): Promise<Webhook> {
  const { data } = await apiClient.put<Webhook>(
    `/workspaces/${workspaceId}/webhooks/${webhookId}`,
    payload
  )
  return data
}

export async function deleteWebhook(workspaceId: number, webhookId: number): Promise<void> {
  await apiClient.delete(`/workspaces/${workspaceId}/webhooks/${webhookId}`)
}

export async function testWebhook(workspaceId: number, webhookId: number): Promise<void> {
  await apiClient.post(`/workspaces/${workspaceId}/webhooks/${webhookId}/test`)
}
