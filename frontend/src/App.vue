<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/store/auth'
import NavBar from '@/components/NavBar.vue'

const auth = useAuthStore()
onMounted(() => auth.initializeFromSession())
</script>

<template>
  <div class="layout">
    <NavBar v-if="auth.isAuthenticated" />
    <main class="main-content">
      <RouterView />
    </main>
  </div>
</template>
