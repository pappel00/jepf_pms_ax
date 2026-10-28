/**
 * 주간·월간 보고서 작성/제출 상태 스토어 — 서버(`/api/v1/pms/reports`)에서 읽고 저장한다.
 *
 * 흐름: 작성 중 → 제출 완료 → 수정 중 → 제출 완료(재제출). 보고서는 (프로젝트, 주간/월간, 기간 시작일) 단위로
 * 1건이며, 제출할 때마다 제출 시점의 요약(전체 진척·목표·특이사항)을 버전으로 남기고 모든 상태 변경은 이력에 쌓는다.
 * 화면의 키는 `프로젝트코드|week|기간시작일` 이다. 보고 본문(진행 업무·이슈 목록)은 저장하지 않고 화면이 계산한다.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsReportsApi } from '../../../api/entities'
import { ensurePmsUsersLoaded } from './pmsUsers'
import { getProjects } from './projectStore'

export const REPORT_STATUSES = ['작성 중', '제출 완료', '수정 중']

export const reportKey = (projectId, mode, periodStart) => `${projectId}|${mode}|${periodStart}`

let reports = {}
let loaded = false
const listeners = new Set()
const emit = (next) => {
  reports = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}

const day = (s) => (s ?? '').slice(0, 10)
const fromApi = (r) => [
  reportKey(r.projectCode, r.reportTypeCode.toLowerCase(), r.periodStart),
  {
    status: r.statusCode,
    memo: r.memo ?? '',
    version: r.submitVersion,
    submitted: r.submitted
      ? { date: day(r.submitted.submittedAt), author: r.submitted.authorName ?? '', overall: Number(r.submitted.overallPct), target: Number(r.submitted.targetPct), memo: r.submitted.memo ?? '' }
      : null,
    history: r.history.map((h) => ({ date: day(h.actedAt), author: h.actorName ?? '', action: h.actionCode, text: h.text })),
  },
]

let loading = null
export function loadReports() {
  if (loading) return loading
  loading = ensurePmsUsersLoaded()
    .then(() => pmsReportsApi.list())
    .then((res) => {
      loaded = true
      emit(Object.fromEntries((res.data || []).map(fromApi)))
    })
    .catch((e) => {
      console.error('[reportStore] 보고서 조회 실패:', e.message)
      loaded = true
      emit(reports)
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useReports() {
  useEffect(() => {
    if (!loaded) loadReports()
  }, [])
  return useSyncExternalStore(subscribe, () => reports)
}

const blank = { status: '작성 중', memo: '', version: 0, submitted: null, history: [] }
export const reportOf = (all, key, seedMemo = '') => all[key] ?? { ...blank, memo: seedMemo }

/** 키 → 서버 호출 인자. project 는 프로젝트 dbId, type 은 WEEK/MONTH. */
function parts(key) {
  const [code, mode, start] = key.split('|')
  return { projectId: getProjects().find((p) => p.id === code)?.dbId, type: mode.toUpperCase(), start }
}

const guard = async (fn, message) => {
  try {
    await fn()
  } catch (e) {
    window.alert(`${message}: ${e.message}`)
  }
  await loadReports()
}

// 특이사항은 입력하는 동안 화면에 바로 반영하고, 잠깐 멈추면 저장한다.
const drafts = new Map() // 키 → { timer, memo }
async function sendDraft(key) {
  const d = drafts.get(key)
  if (!d) return
  clearTimeout(d.timer)
  drafts.delete(key)
  const { projectId, type, start } = parts(key)
  try {
    await pmsReportsApi.saveDraft(projectId, type, start, d.memo)
    await loadReports()
  } catch (e) {
    window.alert(`보고서를 저장하지 못했습니다: ${e.message}`)
    await loadReports()
  }
}

/** 특이사항 저장 — 작성 중/수정 중에만 가능. 첫 저장이면 서버가 「작성」 이력을 남긴다. */
export function saveDraft(key, seedMemo, memo) {
  const cur = reportOf(reports, key, seedMemo)
  emit({ ...reports, [key]: { ...cur, memo } })
  clearTimeout(drafts.get(key)?.timer)
  drafts.set(key, { memo, timer: setTimeout(() => sendDraft(key), 600) })
}

/** 제출(최초/재제출) — 제출 시점의 요약을 버전으로 남긴다. */
export async function submitReport(key, seedMemo, summary) {
  const cur = reportOf(reports, key, seedMemo)
  clearTimeout(drafts.get(key)?.timer)
  drafts.delete(key)
  const { projectId, type, start } = parts(key)
  await guard(() => pmsReportsApi.submit(projectId, type, start, { memo: cur.memo, overallPct: summary.overall, targetPct: summary.target }), '보고서를 제출하지 못했습니다')
}

/** 제출 후 수정 시작 — 사유 필수. 수정 중에는 다시 제출해야 반영된다. */
export async function reopenReport(key, seedMemo, reason) {
  const { projectId, type, start } = parts(key)
  await guard(() => pmsReportsApi.reopen(projectId, type, start, reason), '수정을 시작하지 못했습니다')
}

/** 수정 취소 — 마지막 제출본의 특이사항으로 되돌리고 제출 완료 상태로 복귀. */
export async function cancelEdit(key) {
  const { projectId, type, start } = parts(key)
  await guard(() => pmsReportsApi.cancelEdit(projectId, type, start), '수정을 취소하지 못했습니다')
}
