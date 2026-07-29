import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import * as authApi from '../api/auth'
import { getStoredAuth, setStoredAuth, setUnauthorizedHandler } from '../api/http'

const AuthContext = createContext(null)

// ⚠️ localStorage에 토큰을 저장하는 건 XSS에 취약해서 실제 운영 프론트엔드라면 권장되지
// 않는다(httpOnly 쿠키 등이 더 안전하다). 여기서는 "로그인 흐름이 실제로 동작하는지"를
// 화면으로 빠르게 검증하는 게 목적이라 가장 단순한 방식을 썼다.
export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(getStoredAuth)

  useEffect(() => {
    // http.js가 401(refresh도 실패)을 만나면 이걸 호출한다 — 세션이 끊겼으니 로그아웃 상태로.
    setUnauthorizedHandler(() => {
      setStoredAuth(null)
      setAuth(null)
    })
  }, [])

  const persist = useCallback((value) => {
    setStoredAuth(value)
    setAuth(value)
  }, [])

  const login = useCallback(
    async (form) => {
      const res = await authApi.login(form)
      persist(res.data)
      return res.data
    },
    [persist],
  )

  const register = useCallback((form) => authApi.register(form), [])

  const logout = useCallback(async () => {
    if (auth?.refreshToken) {
      try {
        await authApi.logout(auth.refreshToken)
      } catch {
        // 이미 만료/무효화된 토큰이어도 로컬 상태는 정리하고 넘어간다.
      }
    }
    persist(null)
  }, [auth, persist])

  return (
    <AuthContext.Provider value={{ auth, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.')
  }
  return ctx
}
