/**
 * 이슈 해결 시 일정 조정 제안 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 이슈를 「해결」로 바꿀 때 일정 영향(일)이 있고 연결 업무가 있으면 제안을 만든다.
 *  - 연결 업무(지연 업무): 종료일을 영향 일수(영업일)만큼 늦춘다.
 *  - 후행 업무(선행 체인 predId로 이어진 업무): 시작·종료일을 영향 일수만큼 민다.
 *  - 진척 100%인 업무는 제외. 프로젝트 관리자가 승인하면 WBS 일정에 반영하고, 반려/무시하면 바꾸지 않는다.
 */
import { useSyncExternalStore } from 'react'
import { applyTaskDates, getTasks, isLeaf, shiftWorkdays } from './pmsStore'

let proposals = []
const listeners = new Set()
const emit = (next) => {
  proposals = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}
export const useScheduleProposals = () => useSyncExternalStore(subscribe, () => proposals)
export const pendingProposalOf = (all, issueId) => all.find((p) => p.issueId === issueId && p.status === '제안')

const nextCode = () => `SCP-${new Date().getFullYear()}-${String(proposals.length + 1).padStart(3, '0')}`

/** 이슈 해결 시 호출 — 제안을 만들었으면 제안을, 아니면 null을 돌려준다. */
export function createProposalForIssue(issue, today) {
  const days = Number(issue.impactDays) || 0
  if (days <= 0 || !issue.taskIds?.length) return null
  if (pendingProposalOf(proposals, issue.id)) return null
  const all = getTasks()
  const open = (t) => (t.progress ?? 0) < 100
  const linked = all.filter((t) => issue.taskIds.includes(t.id) && isLeaf(all, t) && open(t))

  // 후행 업무: 연결 업무를 선행으로 두는 업무를 체인 끝까지 따라간다.
  const successors = []
  const seen = new Set(linked.map((t) => t.id))
  let frontier = linked.map((t) => t.id)
  while (frontier.length) {
    const nextFrontier = []
    all.filter((t) => t.predId && frontier.includes(t.predId) && !seen.has(t.id)).forEach((t) => {
      seen.add(t.id)
      if (open(t)) {
        successors.push(t)
        nextFrontier.push(t.id)
      }
    })
    frontier = nextFrontier
  }

  const items = [
    ...linked.map((t) => ({ taskId: t.id, name: t.name, role: '지연 업무', oldStart: t.start, oldEnd: t.end, newStart: t.start, newEnd: shiftWorkdays(t.end, days), applied: false })),
    ...successors.map((t) => ({ taskId: t.id, name: t.name, role: '후행 업무', oldStart: t.start, oldEnd: t.end, newStart: shiftWorkdays(t.start, days), newEnd: shiftWorkdays(t.end, days), applied: false })),
  ]
  if (items.length === 0) return null
  const proposal = {
    id: nextCode(), projectId: issue.projectId, issueId: issue.id, issueTitle: issue.title, status: '제안', shiftDays: days,
    basis: `${issue.id} 해결, 일정 영향 ${days}영업일`, createdAt: today, items, decidedBy: '', decidedAt: '', comment: '',
  }
  emit([proposal, ...proposals])
  return proposal
}

/** 승인(WBS 일정 반영) / 반려(사유 필수) / 무시. */
export function decideProposal(id, status, { by, comment, today }) {
  const p = proposals.find((x) => x.id === id)
  if (!p || p.status !== '제안') return
  if (status === '승인') applyTaskDates(p.items.map((i) => ({ id: i.taskId, start: i.newStart, end: i.newEnd })))
  emit(proposals.map((x) => (x.id === id ? { ...x, status, decidedBy: by, decidedAt: today, comment, items: x.items.map((i) => ({ ...i, applied: status === '승인' })) } : x)))
}
