<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  title?: string
  size?: 'sm' | 'md' | 'lg'
}>()

const emit = defineEmits<{
  close: []
}>()

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  document.body.style.overflow = 'hidden'
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <div class="modal-backdrop" @click.self="emit('close')">
      <div
        class="modal-box"
        :class="[`modal-${props.size ?? 'md'}`]"
        role="dialog"
        aria-modal="true"
      >
        <!-- Header -->
        <div v-if="title || $slots.header" class="modal-header">
          <slot name="header">
            <h3 class="modal-title">{{ title }}</h3>
          </slot>
          <button class="modal-close" aria-label="Close" @click="emit('close')">✕</button>
        </div>

        <!-- Body -->
        <div class="modal-body">
          <slot />
        </div>

        <!-- Footer -->
        <div v-if="$slots.footer" class="modal-footer">
          <slot name="footer" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1rem;
  animation: fadeIn 0.15s ease;
}
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }

.modal-box {
  background: #fff;
  border-radius: 0.5rem;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  width: 100%;
  animation: slideUp 0.15s ease;
}
@keyframes slideUp {
  from { transform: translateY(16px); opacity: 0; }
  to   { transform: translateY(0);    opacity: 1; }
}

.modal-sm { max-width: 400px; }
.modal-md { max-width: 560px; }
.modal-lg { max-width: 800px; }

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1.5rem;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}
.modal-title { font-size: 1.125rem; font-weight: 600; color: #111827; }
.modal-close {
  background: none;
  border: none;
  font-size: 1rem;
  color: #6b7280;
  cursor: pointer;
  padding: 0.25rem;
  line-height: 1;
  border-radius: 0.25rem;
  transition: background 0.15s;
}
.modal-close:hover { background: #f3f4f6; color: #111827; }

.modal-body {
  padding: 1.5rem;
  overflow-y: auto;
  flex: 1;
}

.modal-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 0.75rem;
  padding: 1rem 1.5rem;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}
</style>
