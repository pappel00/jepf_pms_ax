/**
 * 주간·월간 보고서 작성/제출 상태 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 흐름: 작성 중 → 제출 완료 → 수정 중 → 제출 완료(재제출). 보고서는 (프로젝트, 주간/월간, 기간 시작일) 단위로
 * 1건이며, 제출할 때마다 제출 시점의 요약(전체 진척·목표·특이사항)을 버전으로 남기고 모든 상태 변경은 이력에 쌓는다.
 */
import { useSyncExternalStore } from 'react'

export const REPORT_STATUSES = ['작성 중', '제출 완료', '수정 중']

export const reportKey = (projectId, mode, periodStart) => `${projectId}|${mode}|${periodStart}`

let reports = {
  // 지난 주 보고서는 제출 완료 상태로 시작(예시)
  'PRJ-2026-001|week|2026-10-05': {
    status: '제출 완료',
    memo: '• 설계 단계말 검토 완료, 개발 착수 준비 중.',
    version: 1,
    submitted: { date: '2026-10-09', author: '관리자', overall: 13, target: 17, memo: '• 설계 단계말 검토 완료, 개발 착수 준비 중.' },
    history: [
      { date: '2026-10-07', author: '관리자', action: '작성', text: '보고서 작성 시작' },
      { date: '2026-10-09', author: '관리자', action: '제출', text: '제출 (v1)' },
    ],
  },
}
const listeners = new Set()
const emit = (next) => {
  reports = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}
export const useReports = () => useSyncExternalStore(subscribe, () => reports)

const blank = { status: '작성 중', memo: '', version: 0, submitted: null, history: [] }
export const reportOf = (all, key, seedMemo = '') => all[key] ?? { ...blank, memo: seedMemo }

function update(key, seedMemo, fn) {
  const cur = reportOf(reports, key, seedMemo)
  emit({ ...reports, [key]: fn(cur) })
}

/** 특이사항 저장 — 작성 중/수정 중에만 가능. 첫 저장이면 「작성」 이력을 남긴다. */
export function saveDraft(key, seedMemo, memo, actor, today) {
  update(key, seedMemo, (r) => ({
    ...r,
    memo,
    history: r.history.length === 0 ? [{ date: today, author: actor, action: '작성', text: '보고서 작성 시작' }] : r.history,
  }))
}

/** 제출(최초/재제출) — 제출 시점의 요약을 버전으로 남긴다. */
export function submitReport(key, seedMemo, summary, actor, today) {
  update(key, seedMemo, (r) => {
    const version = r.version + 1
    return {
      ...r,
      status: '제출 완료',
      version,
      submitted: { date: today, author: actor, ...summary, memo: r.memo },
      history: [...r.history, { date: today, author: actor, action: '제출', text: version === 1 ? `제출 (v${version})` : `재제출 (v${version})` }],
    }
  })
}

/** 제출 후 수정 시작 — 사유 필수. 수정 중에는 다시 제출해야 반영된다. */
export function reopenReport(key, seedMemo, reason, actor, today) {
  update(key, seedMemo, (r) => ({
    ...r,
    status: '수정 중',
    history: [...r.history, { date: today, author: actor, action: '수정', text: `수정 시작: ${reason}` }],
  }))
}

/** 수정 취소 — 마지막 제출본의 특이사항으로 되돌리고 제출 완료 상태로 복귀. */
export function cancelEdit(key, seedMemo, actor, today) {
  update(key, seedMemo, (r) => ({
    ...r,
    status: '제출 완료',
    memo: r.submitted?.memo ?? r.memo,
    history: [...r.history, { date: today, author: actor, action: '수정 취소', text: `수정 취소 (제출본 v${r.version} 유지)` }],
  }))
}
