import axios from 'axios'

const STORAGE_KEY = 'ml_auth'

export const loadAuth = () => {
  try { return JSON.parse(localStorage.getItem(STORAGE_KEY)) } catch { return null }
}
export const saveAuth = (a) => localStorage.setItem(STORAGE_KEY, JSON.stringify(a))
export const clearAuth = () => localStorage.removeItem(STORAGE_KEY)

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  // sort=a,asc&sort=b,desc  (Spring Pageable multi-sort format)
  paramsSerializer: { indexes: null },
})

api.interceptors.request.use((cfg) => {
  const auth = loadAuth()
  if (auth?.token) cfg.headers.Authorization = `Bearer ${auth.token}`
  return cfg
})

api.interceptors.response.use(
  (r) => r,
  (err) => {
    if (err.response?.status === 401 && !err.config.url.includes('/auth/login')) {
      clearAuth()
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export const errMsg = (e) =>
  e?.response?.data?.message || e?.response?.data?.error || (typeof e?.response?.data === 'string' ? e.response.data : null) || e?.message || 'Something went wrong'

// Strip empty values so they are not sent as blank query params
export const clean = (o) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== '' && v != null && !(Array.isArray(v) && !v.length)))

// ---- Endpoints (adjust here if your backend paths differ) ----
export const Auth = {
  login: (email, password) => api.post('/api/v1/auth/login', { email, password }).then((r) => r.data),
  logout: () => api.post('/api/v1/auth/logout').catch(() => {}),
}
export const Files = {
  upload: (file, onProgress) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post('/api/v1/admin/files/upload', fd, {
      onUploadProgress: (e) => e.total && onProgress?.(Math.round((e.loaded / e.total) * 100)),
      timeout: 0,
    }).then((r) => r.data)
  },
}
// API 1: GET /api/v1/admin/files        -> Spring Page<UploadTaskResponseDto> (upload history)
// API 2: GET /api/v1/admin/files/{id}   -> UploadTaskResponseDto (single task / status)
export const Tasks = {
  list: (params) => api.get('/api/v1/admin/files', { params: clean(params) }).then((r) => r.data),
  get: (id) => api.get(`/api/v1/admin/files/${id}`).then((r) => r.data),
}
export const Stocks = {
  list: (params) => api.get('/api/v1/stocks', { params: clean(params) }).then((r) => r.data),
  // Expected: see README of this folder (analytics contract)
  analytics: (params) => api.get('/api/v1/stocks/analytics', { params: clean(params) }).then((r) => r.data),
}
export const Users = {
  list: () => api.get('/api/v1/admin/users').then((r) => r.data),
  create: (b) => api.post('/api/v1/admin/users', b).then((r) => r.data),
  changeRole: (id, role) => api.patch(`/api/v1/admin/users/${id}/role`, { role }).then((r) => r.data),
  changeStatus: (id, enabled) => api.patch(`/api/v1/admin/users/${id}/status`, { enabled }).then((r) => r.data),
  remove: (id) => api.delete(`/api/v1/admin/users/${id}`),
}
export const IngestionErrors = {
  byTask: (taskId, params) =>
    api.get(`/api/v1/admin/ingestion-errors/tasks/${taskId}`, { params: clean(params) }).then((r) => r.data),
}
export default api