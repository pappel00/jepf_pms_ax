/**
 * 이슈 해결 시 일정 조정 제안 스토어 — 서버(`/api/v1/pms/schedule-proposals`)에서 읽는다.
 *
 * 제안은 이슈를 「해결」로 바꿀 때 서버가 만든다(일정 영향 + 연결 업무가 있을 때).
 *  - 연결 업무(지연 업무): 종료일을 영향 일수(영업일)만큼 늦춘다.
 *  - 후행 업무(선행 체인 predId로 이어진 업무): 시작·종료일을 영향 일수만큼 민다.
 *  - 진척 100%인 업무는 제외. 승인하면 서버가 WBS 일정에 반영하고, 반려/무시하면 바꾸지 않는다.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsScheduleProposalsApi } from '../../../api/entities'
import { loadTasks } from './pmsStore'

let state = { list: [], loaded: false }
const listeners = new Set()
const emit = (next) => {
  state = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}

const day = (s) => (s ?? '').slice(0, 10)
const fromApi = (p) => ({
  id: p.proposalCode,
  dbId: p.id,
  projectId: p.projectCode,
  issueId: p.issueCode,
  issueTitle: p.issueTitle,
  status: p.statusCode,
  shiftDays: p.shiftDays,
  basis: p.basisText ?? '',
  createdAt: day(p.createdAt),
  items: (p.items || []).map((i) => ({
    taskId: i.taskId, name: i.taskName, role: i.role, oldStart: i.oldStart, oldEnd: i.oldEnd, newStart: i.newStart, newEnd: i.newEnd, applied: i.applied,
  })),
  decidedBy: p.decidedByName ?? '',
  decidedAt: day(p.decidedAt),
  comment: p.decisionComment ?? '',
})

let loading = null
export function loadProposals() {
  if (loading) return loading
  loading = pmsScheduleProposalsApi
    .list()
    .then((res) => emit({ list: (res.data || []).map(fromApi), loaded: true }))
    .catch((e) => {
      console.error('[scheduleStore] 일정 조정 제안 조회 실패:', e.message)
      emit({ ...state, loaded: true })
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useScheduleProposals() {
  useEffect(() => {
    if (!state.loaded) loadProposals()
  }, [])
  return useSyncExternalStore(subscribe, () => state).list
}
export const getProposals = () => state.list
export const pendingProposalOf = (all, issueId) => all.find((p) => p.issueId === issueId && p.status === '제안')

/** 승인(서버가 WBS 일정 반영) / 반려(사유 필수) / 무시. 처리자는 로그인 사용자다. */
export async function decideProposal(id, status, { comment }) {
  const p = state.list.find((x) => x.id === id)
  if (!p || p.status !== '제안') return
  try {
    if (status === '승인') await pmsScheduleProposalsApi.approve(p.dbId)
    else if (status === '반려') await pmsScheduleProposalsApi.reject(p.dbId, comment)
    else await pmsScheduleProposalsApi.ignore(p.dbId, comment)
  } catch (e) {
    window.alert(`처리하지 못했습니다: ${e.message}`)
  }
  await loadProposals()
  if (status === '승인') await loadTasks()
}
