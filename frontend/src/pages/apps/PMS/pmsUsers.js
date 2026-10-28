/**
 * PMS 사용자 디렉터리 — 로그인 사용자(이름·소속 구분·인력 등급)를 /api/v1/pms/user-profiles 에서 한 번 읽어 캐시한다.
 * 화면의 투입 인력은 이름으로 다루고 서버는 사용자 ID로 다루므로, 그 변환(teamCodec)과 현재 로그인 사용자 이름 조회에 쓴다.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { getStoredAuth } from '../../../api/http'
import { pmsUserProfilesApi } from '../../../api/entities'

let state = { users: [], loaded: false }
const listeners = new Set()
const emit = (next) => {
  state = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}

let loading = null
export function loadPmsUsers() {
  if (loading) return loading
  loading = pmsUserProfilesApi
    .list()
    .then((res) => emit({ users: res.data || [], loaded: true }))
    .catch(() => emit({ ...state, loaded: true }))
    .finally(() => {
      loading = null
    })
  return loading
}

export function ensurePmsUsersLoaded() {
  return state.loaded ? Promise.resolve() : loadPmsUsers()
}

export function usePmsUsers() {
  useEffect(() => {
    ensurePmsUsersLoaded()
  }, [])
  return useSyncExternalStore(subscribe, () => state).users
}

export const getUsers = () => state.users
export const userIdByName = (name) => state.users.find((u) => u.userName === name)?.userId
export const userNameById = (id) => state.users.find((u) => u.userId === id)?.userName ?? ''

/** 현재 로그인한 사용자의 이름(사용자 디렉터리가 아직 없으면 빈 문자열). */
export const currentUserName = () => userNameById(getStoredAuth()?.userId)