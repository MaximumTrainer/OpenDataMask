import axios from 'axios'
import router from '@/router'

const SAML_AUTH_ENDPOINT = '/saml2/authenticate/default'

/**
 * Reads the XSRF-TOKEN cookie that Spring Security writes when SAML session-based
 * authentication is active. The value must be sent back in the X-XSRF-TOKEN header
 * for every mutating (non-GET) request to satisfy CSRF protection.
 */
function getCsrfToken(): string | undefined {
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  return match ? decodeURIComponent(match[1]) : undefined
}

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true, // Required so session cookies are sent with every request
  timeout: 30_000
})

// Attach JWT token (when present) and CSRF token to every request
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  // Attach CSRF token for mutating requests when a SAML session is active
  const csrfToken = getCsrfToken()
  if (csrfToken && config.method && !['get', 'head', 'options'].includes(config.method.toLowerCase())) {
    config.headers['X-XSRF-TOKEN'] = csrfToken
  }

  return config
})

// Handle 401 globally – clear local credentials and redirect to SAML IdP
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')

      // If a SAML IdP is configured (detected by the presence of the
      // SAML auth endpoint path in our app), redirect to the IdP.
      // Otherwise fall back to the local login page.
      const samlEnabled = import.meta.env.VITE_SAML_ENABLED === 'true'
      if (samlEnabled) {
        window.location.href = SAML_AUTH_ENDPOINT
      } else {
        router.push('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient

