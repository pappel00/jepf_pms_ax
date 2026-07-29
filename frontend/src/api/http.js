const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const STORAGE_KEY = 'jepf-auth'

export function getStoredAuth() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function setStoredAuth(auth) {
  if (auth) {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth))
  } else {
    localStorage.removeItem(STORAGE_KEY)
  }
}

let unauthorizedHandler = () => {}
/** 세션이 완전히 끊겼을 때(refresh도 실패) 호출된다 — App.jsx에서 로그인 화면으로 보내는 용도. */
export function setUnauthorizedHandler(fn) {
  unauthorizedHandler = fn
}

async function rawRequest(path, { method = 'GET', body, token, headers = {} } = {}) {
  const requestHeaders = { 'Content-Type': 'application/json', ...headers }
  if (token) requestHeaders['Authorization'] = `Bearer ${token}`

  const response = await fetch(`${BASE_URL}${path}`, {
    method,
    headers: requestHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  const data = await response.json().catch(() => null)
  return { response, data }
}

// 여러 API 요청이 거의 동시에 401을 받으면 각자 독립적으로 refresh를 시도하게 되는데,
// 같은 refreshToken으로 동시에 두 번 refresh하면 백엔드에서 낙관적 락 충돌이 난다
// (refresh token 회전(rotation) 로직이 같은 행을 두 번 갱신하려 하기 때문). 그래서 이미
// 진행 중인 refresh가 있으면 새로 요청하지 않고 그 Promise를 그대로 공유한다("single-flight").
let inFlightRefresh = null

async function tryRefresh() {
  if (inFlightRefresh) {
    return inFlightRefresh
  }
  inFlightRefresh = doRefresh().finally(() => {
    inFlightRefresh = null
  })
  return inFlightRefresh
}

async function doRefresh() {
  const auth = getStoredAuth()
  if (!auth?.refreshToken) return null
  const { response, data } = await rawRequest('/api/v1/auth/refresh', {
    method: 'POST',
    body: { refreshToken: auth.refreshToken },
  })
  if (response.ok && data?.success) {
    setStoredAuth(data.data)
    return data.data
  }
  setStoredAuth(null)
  return null
}

/**
 * 인증이 필요한 API 호출용 공통 함수. 저장된 accessToken을 자동으로 붙이고,
 * 401이 오면 refreshToken으로 한 번 자동 갱신을 시도한 뒤 재요청한다 — 화면에서
 * accessToken 만료를 신경 쓸 필요 없이 그냥 호출하면 된다.
 */
export async function apiFetch(path, options = {}) {
  const auth = getStoredAuth()
  let { response, data } = await rawRequest(path, { ...options, token: auth?.accessToken })

  if (response.status === 401 && auth?.refreshToken) {
    const refreshed = await tryRefresh()
    if (refreshed) {
      ;({ response, data } = await rawRequest(path, { ...options, token: refreshed.accessToken }))
    } else {
      unauthorizedHandler()
    }
  } else if (response.status === 401) {
    unauthorizedHandler()
  }

  if (!response.ok || (data && data.success === false)) {
    const message = data?.message || `요청에 실패했습니다 (HTTP ${response.status})`
    const error = new Error(message)
    error.code = data?.code
    error.errors = data?.errors
    error.status = response.status
    throw error
  }

  return data
}

/** 로그인/회원가입처럼 토큰이 아직 없는(또는 필요 없는) 호출용. */
export async function publicFetch(path, options = {}) {
  const { response, data } = await rawRequest(path, options)
  if (!response.ok || (data && data.success === false)) {
    const message = data?.message || `요청에 실패했습니다 (HTTP ${response.status})`
    const error = new Error(message)
    error.code = data?.code
    error.errors = data?.errors
    throw error
  }
  return data
}

/**
 * 실제 파일 업로드용(multipart/form-data). apiFetch와 달리 JSON.stringify를 안 하고
 * FormData를 그대로 보낸다 — Content-Type도 브라우저가 boundary 포함해서 자동으로 붙이므로
 * 직접 지정하면 안 된다. 401 처리(자동 refresh)는 apiFetch와 동일하게 지원한다.
 */
export async function uploadFile(path, formData) {
  const doUpload = async (token) => {
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const response = await fetch(`${BASE_URL}${path}`, { method: 'POST', headers, body: formData })
    const data = await response.json().catch(() => null)
    return { response, data }
  }

  const auth = getStoredAuth()
  let { response, data } = await doUpload(auth?.accessToken)

  if (response.status === 401 && auth?.refreshToken) {
    const refreshed = await tryRefresh()
    if (refreshed) {
      ;({ response, data } = await doUpload(refreshed.accessToken))
    } else {
      unauthorizedHandler()
    }
  } else if (response.status === 401) {
    unauthorizedHandler()
  }

  if (!response.ok || (data && data.success === false)) {
    const message = data?.message || `업로드에 실패했습니다 (HTTP ${response.status})`
    const error = new Error(message)
    error.code = data?.code
    error.errors = data?.errors
    throw error
  }

  return data
}

/**
 * 첨부파일 다운로드. ApiResponse JSON이 아니라 파일 바이트를 그대로 받아서(blob) 브라우저가
 * 저장 다이얼로그를 띄우게 만든다 — 인증 헤더가 필요해서 <a href> 직접 연결로는 안 되고
 * fetch로 받아온 뒤 임시 objectURL을 만들어 클릭시켜야 한다.
 */
export async function downloadFile(path, suggestedFileName) {
  const doDownload = async (token) => {
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    return fetch(`${BASE_URL}${path}`, { headers })
  }

  const auth = getStoredAuth()
  let response = await doDownload(auth?.accessToken)

  if (response.status === 401 && auth?.refreshToken) {
    const refreshed = await tryRefresh()
    if (refreshed) {
      response = await doDownload(refreshed.accessToken)
    } else {
      unauthorizedHandler()
    }
  } else if (response.status === 401) {
    unauthorizedHandler()
  }

  if (!response.ok) {
    let message = `다운로드에 실패했습니다 (HTTP ${response.status})`
    try {
      const data = await response.json()
      message = data?.message || message
    } catch {
      // 응답이 JSON이 아니면(파일 바이트 시도 중 에러 등) 기본 메시지를 쓴다.
    }
    throw new Error(message)
  }

  const blob = await response.blob()
  const objectUrl = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = suggestedFileName || 'download'
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(objectUrl)
}
