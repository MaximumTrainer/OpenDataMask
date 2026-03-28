import axios from 'axios'
import router from '@/router'

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: {
    'Content-Type': 'application/json'
  },
  timeout: 30_000
})

// Attach JWT token to every request
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Handle 401 globally – redirect to login
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default apiClient
