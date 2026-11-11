import { createContext, useContext, useState, useCallback } from 'react'
import { Auth, loadAuth, saveAuth, clearAuth } from './api.js'

const Ctx = createContext(null)
export const useAuth = () => useContext(Ctx)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadAuth())

  const login = useCallback(async (email, password) => {
    const res = await Auth.login(email, password)
    const u = { token: res.token, email: res.email, role: res.role }
    saveAuth(u)
    setUser(u)
  }, [])

  const logout = useCallback(async () => {
    await Auth.logout()
    clearAuth()
    setUser(null)
  }, [])

  return <Ctx.Provider value={{ user, login, logout, isAdmin: user?.role === 'ADMIN' }}>{children}</Ctx.Provider>
}
