export const API_BASE = '/api'
export const TOKEN_KEY = 'intellitrip_user'

export class UnauthorizedError extends Error {
  constructor() {
    super('Unauthorized')
    this.name = 'UnauthorizedError'
  }
}

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(TOKEN_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export function setStoredUser(user) {
  if (user) localStorage.setItem(TOKEN_KEY, JSON.stringify(user))
  else localStorage.removeItem(TOKEN_KEY)
}

async function request(path, options = {}) {
  const user = getStoredUser()
  const headers = new Headers(options.headers)
  if (!headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (user) headers.set('X-User-Id', user.id)

  const res = await fetch(API_BASE + path, {
    credentials: 'include',
    headers,
    method: options.method || 'GET',
    body: options.body,
  })

  if (res.status === 401) throw new UnauthorizedError()
  if (!res.ok) {
    let message = 'Request failed with status ' + res.status
    try {
      const data = await res.json()
      if (data && data.error) message = data.error
    } catch (e) {}
    throw new Error(message)
  }
  if (res.status === 204) return undefined
  return res.json()
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined }),
  put: (path, body) => request(path, { method: 'PUT', body: body ? JSON.stringify(body) : undefined }),
  patch: (path, body) => request(path, { method: 'PATCH', body: body ? JSON.stringify(body) : undefined }),
  del: (path) => request(path, { method: 'DELETE' }),
}

export async function login(email, password) {
  const data = await api.post('/auth/login', { email, password })
  setStoredUser(data)
  return data
}

export async function signup(name, email, password) {
  const data = await api.post('/auth/signup', { name, email, password })
  setStoredUser(data)
  return data
}

export function logout() {
  setStoredUser(null)
}
