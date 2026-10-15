/**
 * 투입 관리(인력관리) 계산/실적 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 투입 계획의 원천은 프로젝트의 수행 관리 정보(단계전환에서 지정한 투입 인력 = 프로젝트 멤버)이다.
 * 여기서는 그 계획을 월별로 펼쳐 인력별 투입률·과투입을 계산하고, 월별 **실적 M/M**만 입력받는다.
 */
import { useSyncExternalStore } from 'react'
import { MEMBERS } from './pmsStore'

const parse = (s) => new Date(`${s}T00:00:00Z`)
const iso = (d) => d.toISOString().slice(0, 10)

// ---------- 설정(변경 가능) ----------
// overLimit: 월 투입률이 이 값(%)을 넘으면 과투입으로 본다. idleLimit: 이 값(%) 미만이면 여유 인력으로 본다.
let settings = { overLimit: 105, idleLimit: 50 }
const settingListeners = new Set()

function subscribeSettings(listener) {
  settingListeners.add(listener)
  return () => settingListeners.delete(listener)
}

export function useStaffingSettings() {
  return useSyncExternalStore(subscribeSettings, () => settings)
}

export function setStaffingSettings(patch) {
  settings = { ...settings, ...patch }
  settingListeners.forEach((l) => l())
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
  if (!a.from || !a.to || a.to < a.from) return {}
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
let actuals = {
  [actKey('PRJ-2026-001', '이설계', '2026-09')]: 1,
  [actKey('PRJ-2026-001', '이설계', '2026-10')]: 0.9,
  [actKey('PRJ-2026-001', '김개발', '2026-10')]: 0.8,
  [actKey('PRJ-2026-001', '박협력', '2026-10')]: 0.6,
  [actKey('PRJ-2026-009', '김개발', '2026-09')]: 0.5,
  [actKey('PRJ-2026-009', '김개발', '2026-10')]: 0.5,
  [actKey('PRJ-2026-009', '정QA', '2026-10')]: 0.4,
}
const listeners = new Set()

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function useActuals() {
  return useSyncExternalStore(subscribe, () => actuals)
}

export const actualOf = (all, projectId, name, month) => all[actKey(projectId, name, month)]

export function setActual(projectId, name, month, value) {
  const k = actKey(projectId, name, month)
  const next = { ...actuals }
  if (value === '' || value === null || Number.isNaN(Number(value))) delete next[k]
  else next[k] = Math.max(0, Number(value))
  actuals = next
  listeners.forEach((l) => l())
}

export const sumActual = (all, projectId, name) =>
  Object.entries(all)
    .filter(([k]) => k.startsWith(`${projectId}|${name}|`))
    .reduce((s, [, v]) => s + v, 0)
