import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import WorkspacesView from '@/views/WorkspacesView.vue'
import WorkspaceDetailView from '@/views/WorkspaceDetailView.vue'
import ConnectionsView from '@/views/ConnectionsView.vue'
import TablesView from '@/views/TablesView.vue'
import JobsView from '@/views/JobsView.vue'
import ActionsView from '@/views/ActionsView.vue'
import SensitivityRulesView from '@/views/SensitivityRulesView.vue'

const SAML_AUTH_ENDPOINT = '/saml2/authenticate/default'
const samlEnabled = import.meta.env.VITE_SAML_ENABLED === 'true'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: () => {
        const auth = useAuthStore()
        return auth.isAuthenticated ? '/workspaces' : '/login'
      }
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: { public: true }
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
      meta: { public: true }
    },
    {
      path: '/workspaces',
      name: 'workspaces',
      component: WorkspacesView,
      meta: { requiresAuth: true }
    },
    {
      path: '/workspaces/:id',
      name: 'workspace-detail',
      component: WorkspaceDetailView,
      meta: { requiresAuth: true }
    },
    {
      path: '/workspaces/:id/connections',
      name: 'connections',
      component: ConnectionsView,
      meta: { requiresAuth: true }
    },
    {
      path: '/workspaces/:id/tables',
      name: 'tables',
      component: TablesView,
      meta: { requiresAuth: true }
    },
    {
      path: '/workspaces/:id/jobs',
      name: 'jobs',
      component: JobsView,
      meta: { requiresAuth: true }
    },
    {
      path: '/workspaces/:id/actions',
      name: 'actions',
      component: ActionsView,
      meta: { requiresAuth: true }
    },
    {
      path: '/settings/sensitivity-rules',
      name: 'sensitivity-rules',
      component: SensitivityRulesView,
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    // When SAML SSO is enabled, redirect the browser directly to the IdP instead
    // of showing the local login form.
    if (samlEnabled) {
      window.location.href = SAML_AUTH_ENDPOINT
      return false
    }
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.public && auth.isAuthenticated) {
    return { name: 'workspaces' }
  }

  return true
})

export default router

