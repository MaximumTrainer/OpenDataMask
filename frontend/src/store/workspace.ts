import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as workspaceApi from '@/api/workspaces'
import type { Workspace, WorkspaceRequest } from '@/types'

export const useWorkspaceStore = defineStore('workspace', () => {
  const workspaces = ref<Workspace[]>([])
  const currentWorkspace = ref<Workspace | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchWorkspaces(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      workspaces.value = await workspaceApi.listWorkspaces()
    } catch (e: unknown) {
      error.value = extractMessage(e)
    } finally {
      loading.value = false
    }
  }

  async function fetchWorkspace(id: number): Promise<void> {
    loading.value = true
    error.value = null
    try {
      currentWorkspace.value = await workspaceApi.getWorkspace(id)
    } catch (e: unknown) {
      error.value = extractMessage(e)
    } finally {
      loading.value = false
    }
  }

  async function createWorkspace(payload: WorkspaceRequest): Promise<Workspace> {
    const created = await workspaceApi.createWorkspace(payload)
    workspaces.value.push(created)
    return created
  }

  async function updateWorkspace(id: number, payload: WorkspaceRequest): Promise<Workspace> {
    const updated = await workspaceApi.updateWorkspace(id, payload)
    const idx = workspaces.value.findIndex((w) => w.id === id)
    if (idx !== -1) workspaces.value[idx] = updated
    if (currentWorkspace.value?.id === id) currentWorkspace.value = updated
    return updated
  }

  async function deleteWorkspace(id: number): Promise<void> {
    await workspaceApi.deleteWorkspace(id)
    workspaces.value = workspaces.value.filter((w) => w.id !== id)
    if (currentWorkspace.value?.id === id) currentWorkspace.value = null
  }

  function extractMessage(e: unknown): string {
    if (e instanceof Error) return e.message
    return 'An unexpected error occurred'
  }

  return {
    workspaces,
    currentWorkspace,
    loading,
    error,
    fetchWorkspaces,
    fetchWorkspace,
    createWorkspace,
    updateWorkspace,
    deleteWorkspace
  }
})
