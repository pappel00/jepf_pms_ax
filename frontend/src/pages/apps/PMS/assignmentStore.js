/**
 * 투입 관리(인력관리) 계산/실적 스토어 — 설정(과투입 기준)과 월별 실적 M/M 은 서버에 저장한다.
 *
 * 투입 계획의 원천은 프로젝트의 수행 관리 정보(단계전환에서 지정한 투입 인력 = 프로젝트 멤버)이다.
 * 여기서는 그 계획을 월별로 펼쳐 인력별 투입률·과투입을 계산하고, 월별 **실적 M/M**만 입력받는다(프로젝트 관리자만 — 서버도 검사).
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsCostApi } from '../../../api/entities'
import { MEMBERS } from './pmsStore'
import { getProjects } from './projectStore'
import { ensurePmsUsersLoaded, userIdByName } from './pmsUsers'

const parse = (s) => new Date(`${s}T00:00:00Z`)
const iso = (d) => d.toISOString().slice(0, 10)

// ---------- 설정(변경 가능) ----------
// overLimit: 월 투입률이 이 값(%)을 넘으면 과투입으로 본다. idleLimit: 이 값(%) 미만이면 여유 인력으로 본다.
let settings = { overLimit: 105, idleLimit: 50 }
let settingsLoaded = false
const settingListeners = new Set()

export function loadStaffingSettings() {
  return pmsCostApi
    .settings()
    .then((res) => {
      const v = res.data || {}
      settings = { overLimit: Number(v['staffing.over_limit_pct'] ?? settings.overLimit), idleLimit: Number(v['staffing.idle_limit_pct'] ?? settings.idleLimit) }
      settingsLoaded = true
      settingListeners.forEach((l) => l())
    })
    .catch((e) => console.error('[assignmentStore] 설정 조회 실패:', e.message))
}

function subscribeSettings(listener) {
  settingListeners.add(listener)
  return () => settingListeners.delete(listener)
}

export function useStaffingSettings() {
  useEffect(() => {
    if (!settingsLoaded) loadStaffingSettings()
  }, [])
  return useSyncExternalStore(subscribeSettings, () => settings)
}

let settingsTimer = null
/** 입력 중에는 화면에 바로 반영하고, 잠깐 멈추면 서버에 저장한다(승인 권한이 없으면 저장이 거부되어 서버 값으로 되돌린다). */
export function setStaffingSettings(patch) {
  settings = { ...settings, ...patch }
  settingListeners.forEach((l) => l())
  clearTimeout(settingsTimer)
  settingsTimer = setTimeout(() => {
    pmsCostApi
      .saveSettings({ 'staffing.over_limit_pct': String(settings.overLimit), 'staffing.idle_limit_pct': String(settings.idleLimit) })
      .catch((e) => {
        window.alert(`설정을 저장하지 못했습니다: ${e.message}`)
        loadStaffingSettings()
      })
  }, 600)
}

/** 과투입 기준 투입률(1.0 = 100%)과 여유 인력 기준. */
export const overLimitRatio = () => settings.overLimit / 100
export const idleRatio = () => settings.idleLimit / 100

export const daysInMonth = (key) => new Date(Date.UTC(Number(key.slice(0, 4)), Number(key.slice(5, 7)), 0)).getUTCDate()

/** from~to 사이의 모든 월 키('YYYY-MM'). */
export function monthsOf(from, to) {
  if (!from || !to || to < from) return []
  const out = []
  const d = new Date(Date.UTC(Number(from.slice(0, 4)), Number(from.slice(5, 7)) - 1, 1))
  const end = parse(to)
  while (d <= end) {
    out.push(d.toISOString().slice(0, 7))
    d.setUTCMonth(d.getUTCMonth() + 1)
  }
  return out
}

export const addMonths = (key, n) => {
  const d = new Date(Date.UTC(Number(key.slice(0, 4)), Number(key.slice(5, 7)) - 1 + n, 1))
  return d.toISOString().slice(0, 7)
}

/**
 * 투입 한 건(인력 × 프로젝트)의 월별 계획 투입률.
 * M/M이 있으면 기간(월 환산)으로 나눠 월별 투입률(1.0 = 100%)로, 없으면 100%로 가정한다. 첫/마지막 달은 걸친 일수만큼 비례.
 */
export function monthlyPlan(a) {
  const base = autoMonthlyPlan(a)
  // 월별 조정(a.monthPlan: { 'YYYY-MM': 투입률(%) })이 있는 달은 자동 계산값 대신 그 값을 쓴다.
  Object.entries(a.monthPlan ?? {}).forEach(([month, pct]) => {
    if (month in base && pct !== '' && pct != null) base[month] = Math.round((Number(pct) / 100) * 100) / 100
  })
  return base
}

/** 월별 조정 없이 M/M·투입 비율·기간·공백 구간만으로 계산한 월별 계획 투입률(1.0 = 100%). */
export function autoMonthlyPlan(a) {
  if (!a.from || !a.to || a.to < a.from) return {}
  // 투입 비율(rate)이 있으면: 월 투입률 = 비율, 공백 구간(gaps)의 날짜는 투입에서 제외
  if (a.rate != null && a.rate !== '') {
    const ratio = Number(a.rate) / 100
    const gapSet = new Set()
    ;(a.gaps ?? []).forEach((g) => {
      if (!g.from || !g.to || g.to < g.from) return
      for (let d = parse(g.from); d <= parse(g.to); d.setUTCDate(d.getUTCDate() + 1)) gapSet.add(iso(d))
    })
    const perMonthDays = {}
    for (let d = parse(a.from); d <= parse(a.to); d.setUTCDate(d.getUTCDate() + 1)) {
      const day = iso(d)
      if (!gapSet.has(day)) perMonthDays[day.slice(0, 7)] = (perMonthDays[day.slice(0, 7)] ?? 0) + 1
    }
    const out = {}
    monthsOf(a.from, a.to).forEach((key) => {
      out[key] = Math.round(ratio * ((perMonthDays[key] ?? 0) / daysInMonth(key)) * 100) / 100
    })
    return out
  }
  const total = (parse(a.to) - parse(a.from)) / 86400000 + 1
  const perMonth = Number(a.mm) > 0 ? Number(a.mm) / (total / 30.4) : 1
  const out = {}
  monthsOf(a.from, a.to).forEach((key) => {
    const first = `${key}-01`
    const last = `${key}-${String(daysInMonth(key)).padStart(2, '0')}`
    const s = a.from > first ? a.from : first
    const e = a.to < last ? a.to : last
    const overlap = (parse(e) - parse(s)) / 86400000 + 1
    out[key] = Math.round(perMonth * (overlap / daysInMonth(key)) * 100) / 100
  })
  return out
}

/**
 * 투입 계획을 평탄화한다 — 수행 중 프로젝트의 수행 투입 인력 + 운영 중 프로젝트의 운영 투입 인력.
 * 운영 투입은 projectId를 `프로젝트ID#OPS`로 구분한다. managerName = 실적 M/M을 입력할 수 있는 프로젝트 관리자.
 */
export function collectAssignments(projects) {
  const list = []
  projects.forEach((p) => {
    if (p.transitioned && p.status === '수행 중') {
      p.transitioned.team.forEach((m) =>
        list.push({
          ...m, kind: '수행', projectId: p.id, baseProjectId: p.id, projectName: p.name,
          managerName: p.transitioned.manager.name, isManager: p.transitioned.manager.name === m.name, key: `${p.id}|${m.name}`,
        }),
      )
    }
    if (p.operation && p.status === '운영 중') {
      p.operation.team.forEach((m) =>
        list.push({
          ...m, kind: '운영', projectId: `${p.id}#OPS`, baseProjectId: p.id, projectName: `${p.name} (운영)`,
          managerName: p.operation.manager.name, isManager: p.operation.manager.name === m.name, key: `${p.id}#OPS|${m.name}`,
        }),
      )
    }
  })
  return list
}

/** 인력별 월 투입률 합계. */
export function personMonthly(assignments, name) {
  const out = {}
  assignments
    .filter((a) => a.name === name)
    .forEach((a) => {
      Object.entries(monthlyPlan(a)).forEach(([k, v]) => {
        out[k] = Math.round(((out[k] ?? 0) + v) * 100) / 100
      })
    })
  return out
}

export const orgOfMember = (name) => MEMBERS.find((m) => m.name === name)?.org ?? ''

// ---------- 실적 M/M ----------
const actKey = (projectId, name, month) => `${projectId}|${name}|${month}`
let actuals = {}
let actualsLoaded = false
const listeners = new Set()

export function loadActuals() {
  return ensurePmsUsersLoaded()
    .then(() => pmsCostApi.actuals())
    .then((res) => {
      actuals = Object.fromEntries((res.data || []).map((a) => [actKey(a.projectCode, a.userName, a.month), Number(a.actualMm)]))
      actualsLoaded = true
      listeners.forEach((l) => l())
    })
    .catch((e) => console.error('[assignmentStore] 실적 M/M 조회 실패:', e.message))
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function useActuals() {
  useEffect(() => {
    if (!actualsLoaded) loadActuals()
  }, [])
  return useSyncExternalStore(subscribe, () => actuals)
}

export const actualOf = (all, projectId, name, month) => all[actKey(projectId, name, month)]

const actualTimers = new Map()
/** 입력 중에는 화면에 바로 반영하고, 잠깐 멈추면 서버에 저장한다(프로젝트 관리자만 — 아니면 거부되어 서버 값으로 되돌린다). */
export function setActual(projectId, name, month, value) {
  const k = actKey(projectId, name, month)
  const next = { ...actuals }
  const clear = value === '' || value === null || Number.isNaN(Number(value))
  if (clear) delete next[k]
  else next[k] = Math.max(0, Number(value))
  actuals = next
  listeners.forEach((l) => l())
  clearTimeout(actualTimers.get(k))
  actualTimers.set(
    k,
    setTimeout(() => {
      const project = getProjects().find((p) => p.id === projectId)
      const userId = userIdByName(name)
      if (!project || !userId) return
      pmsCostApi.saveActual(project.dbId, { userId, month, actualMm: clear ? null : Math.max(0, Number(value)) }).catch((e) => {
        window.alert(`실적을 저장하지 못했습니다: ${e.message}`)
        loadActuals()
      })
    }, 600),
  )
}

export const sumActual = (all, projectId, name) =>
  Object.entries(all)
    .filter(([k]) => k.startsWith(`${projectId}|${name}|`))
    .reduce((s, [, v]) => s + v, 0)
