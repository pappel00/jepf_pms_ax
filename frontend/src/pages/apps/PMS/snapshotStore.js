/**
 * 진척 스냅샷(주간) 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 프로젝트별로 매주 월요일 기준의 전체 진척(실적)·목표를 저장해 두고, 주간·월간 보고에서
 * 기간별 증감과 추이를 보여준다. 원본 WBS의 "주 단위 진행률 컬럼"을 행 데이터로 정규화한 형태다.
 * 현재(기준일) 값은 WBS에서 계산한 실시간 값을 쓰고, 「스냅샷 저장」으로 그 시점 값을 고정한다.
 */
import { useSyncExternalStore } from 'react'

const addDays = (s, n) => {
  const d = new Date(`${s}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() + n)
  return d.toISOString().slice(0, 10)
}

// date = 해당 주 월요일, overall = 실적 %, target = 계획 %
let snapshots = {
  'PRJ-2026-001': [
    { date: '2026-09-07', overall: 0, target: 1 },
    { date: '2026-09-14', overall: 2, target: 4 },
    { date: '2026-09-21', overall: 5, target: 8 },
    { date: '2026-09-28', overall: 9, target: 12 },
    { date: '2026-10-05', overall: 13, target: 17 },
  ],
  'PRJ-2026-007': [
    { date: '2026-10-05', overall: 20, target: 25 },
  ],
}
const listeners = new Set()
const emit = (next) => {
  snapshots = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}
export const useSnapshots = () => useSyncExternalStore(subscribe, () => snapshots)

/** 기준일이 속한 주의 월요일. */
export function mondayOf(date) {
  const day = new Date(`${date}T00:00:00Z`).getUTCDay()
  return addDays(date, -((day + 6) % 7))
}

/** 같은 주(월요일)의 스냅샷이 있으면 덮어쓴다. */
export function saveSnapshot(projectId, date, overall, target) {
  const monday = mondayOf(date)
  const list = (snapshots[projectId] ?? []).filter((s) => s.date !== monday)
  emit({ ...snapshots, [projectId]: [...list, { date: monday, overall, target }].sort((a, b) => a.date.localeCompare(b.date)) })
}

/**
 * 특정 날짜 시점의 진척. 기준일 이후(또는 당일)는 실시간 값(live), 그 이전은 그 날짜 이전 가장 가까운 스냅샷.
 * 스냅샷이 없으면 null.
 */
export function valueAt(list, date, today, live) {
  if (date >= today) return live
  const hit = [...(list ?? [])].reverse().find((s) => s.date <= date)
  return hit ? { overall: hit.overall, target: hit.target } : null
}
