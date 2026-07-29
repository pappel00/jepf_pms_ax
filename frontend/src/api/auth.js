import { publicFetch } from './http'

export function login({ companyCode, loginId, password }) {
  return publicFetch('/api/v1/auth/login', {
    method: 'POST',
    body: { companyCode, loginId, password },
  })
}

export function register({ companyCode, loginId, password, email, userName }) {
  return publicFetch('/api/v1/auth/register', {
    method: 'POST',
    body: { companyCode, loginId, password, email, userName },
  })
}

export function logout(refreshToken) {
  return publicFetch('/api/v1/auth/logout', {
    method: 'POST',
    body: { refreshToken },
  })
}
