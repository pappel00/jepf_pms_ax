/**
 * 진척 스냅샷(주간) 스토어 — 서버(`/api/v1/pms/progress-snapshots`)에서 읽고 저장한다.
 *
 * 프로젝트별로 매주 월요일 기준의 전체 진척(실적)·목표를 저장해 두고, 주간·월간 보고에서
 * 기간별 증감과 추이를 보여준다. 원본 WBS의 "주 단위 진행률 컬럼"을 행 데이터로 정규화한 형태다.
 * 현재(기준일) 값은 WBS에서 계산한 실시간 값을 쓰고, 「스냅샷 저장」으로 그 시점 값을 고정한다.
 * 화면 키는 프로젝트 코드다. 자동 저장 스케줄러는 후속 과제(지금은 수동 저장).
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsReportsApi } from '../../../api/entities'
import { getProjects } from './projectStore'

const addDays = (s, n) => {
  const d = new Date(`${s}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() + n)
  return d.toISOString().slice(0, 10)
}

// 프로젝트 코드 → [{ date(해당 주 월요일), overall(실적 %), target(계획 %) }]
let snapshots = {}
let loaded = false
const listeners = new Set()
const emit = (next) => {
  snapshots = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}

let loading = null
export function loadSnapshots() {
  if (loading) return loading
  loading = pmsReportsApi
    .snapshots()
    .then((res) => {
      loaded = true
      const map = {}
      ;(res.data || []).forEach((s) => {
        ;(map[s.projectCode] ??= []).push({ date: s.snapshotDate, overall: Number(s.overallPct), target: Number(s.targetPct) })
      })
      emit(map)
    })
    .catch((e) => {
      console.error('[snapshotStore] 스냅샷 조회 실패:', e.message)
      loaded = true
      emit(snapshots)
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useSnapshots() {
  useEffect(() => {
    if (!loaded) loadSnapshots()
  }, [])
  return useSyncExternalStore(subscribe, () => snapshots)
}

/** 기준일이 속한 주의 월요일. */
export function mondayOf(date) {
  const day = new Date(`${date}T00:00:00Z`).getUTCDay()
  return addDays(date, -((day + 6) % 7))
}

/** 같은 주(월요일)의 스냅샷이 있으면 덮어쓴다. */
export async function saveSnapshot(projectId, date, overall, target) {
  const project = getProjects().find((p) => p.id === projectId)
  if (!project) return
  try {
    await pmsReportsApi.saveSnapshot(project.dbId, { snapshotDate: mondayOf(date), overallPct: overall, targetPct: target })
  } catch (e) {
    window.alert(`스냅샷을 저장하지 못했습니다: ${e.message}`)
  }
  await loadSnapshots()
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
