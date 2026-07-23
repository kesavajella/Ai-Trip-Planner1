import { useState, useEffect } from 'react'
import { getStoredUser, setStoredUser, login as apiLogin, signup as apiSignup, logout as apiLogout } from '../api'

export function useAuth() {
  const [user, setUser] = useState(() => getStoredUser())

  useEffect(() => {
    setStoredUser(user)
  }, [user])

  const signin = async (email, password) => {
    const data = await apiLogin(email, password)
    setUser(data)
  }

  const signup = async (name, email, password) => {
    const data = await apiSignup(name, email, password)
    setUser(data)
  }

  const signout = () => {
    apiLogout()
    setUser(null)
  }

  return { user, signin, signup, signout }
}
