<script setup lang="ts">
import { computed } from 'vue'
import type { JobStatus } from '@/types'

const props = defineProps<{
  status: JobStatus | string
}>()

const config = computed(() => {
  const map: Record<string, { label: string; cls: string }> = {
    PENDING:   { label: 'Pending',   cls: 'badge-yellow' },
    RUNNING:   { label: 'Running',   cls: 'badge-blue'   },
    COMPLETED: { label: 'Completed', cls: 'badge-green'  },
    FAILED:    { label: 'Failed',    cls: 'badge-red'    },
    CANCELLED: { label: 'Cancelled', cls: 'badge-gray'   }
  }
  return map[props.status] ?? { label: props.status, cls: 'badge-gray' }
})
</script>

<template>
  <span class="badge" :class="config.cls">{{ config.label }}</span>
</template>
