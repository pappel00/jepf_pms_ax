/**
 * 수행단계전환 승인 · 롤백 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 단계전환 실행은 바로 반영하지 않고 「전환 요청」(승인 대기)을 만든다. 요청자가 아닌 승인자(센터장·admin)가 승인하면
 * WBS·문서 테일러링 기초데이터가 생성되고 프로젝트가 「수행 중」이 된다. 반려/취소하면 아무것도 바뀌지 않는다.
 * 전환 후에도 진척 입력·이슈가 없으면 사유를 남기고 롤백(수주 상태로 복귀)할 수 있다. 모든 처리는 이력으로 남긴다.
 */
import { useSyncExternalStore } from 'react'
import { clearProjectDocs, initTailoring } from './docStore'
import { addProjectTasks, removeProjectTasks } from './pmsStore'
import { rollbackTransition, transitionProject } from './projectStore'

export const TRANSITION_STATUSES = ['승인 대기', '승인', '반려', '취소']

let requests = []
let log = [] // 롤백 이력
const listeners = new Set()
const emit = () => listeners.forEach((l) => l())
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}
export const useTransitionRequests = () => useSyncExternalStore(subscribe, () => requests)
export const useRollbackLog = () => useSyncExternalStore(subscribe, () => log)

export const pendingTransitionOf = (all, projectId) => all.find((r) => r.projectId === projectId && r.status === '승인 대기')

const nextId = () => `TRN-${new Date().getFullYear()}-${String(requests.length + 1).padStart(3, '0')}`

/**
 * 전환 요청 등록 — payload에 실행에 필요한 값(미리보기 WBS, 문서 테일러링 옵션, 전환 정보)을 담아 두고,
 * 승인 시점에 그대로 적용한다. summary는 승인 화면에 보여줄 요약.
 */
export function requestTransition({ project, requester, today, payload, summary }) {
  requests = [
    { id: nextId(), projectId: project.id, projectName: project.name, requester, requestedAt: today, payload, summary, status: '승인 대기', decidedBy: '', decidedAt: '', comment: '' },
    ...requests,
  ]
  emit()
}

export function decideTransition(id, status, { by, comment, today }) {
  const req = requests.find((r) => r.id === id)
  if (!req || req.status !== '승인 대기') return
  if (status === '승인') {
    const { tasks, tailoring, transition } = req.payload
    addProjectTasks(tasks)
    initTailoring(req.projectId, tailoring)
    transitionProject(req.projectId, { ...transition, date: today })
  }
  requests = requests.map((r) => (r.id === id ? { ...r, status, decidedBy: by, decidedAt: today, comment } : r))
  emit()
}

export function cancelTransition(id, { by, today }) {
  requests = requests.map((r) => (r.id === id && r.status === '승인 대기' ? { ...r, status: '취소', decidedBy: by, decidedAt: today } : r))
  emit()
}

/** 롤백 가능 여부 — 진척이 입력됐거나 이슈가 있으면 불가. 불가 사유 문자열 또는 null. */
export function rollbackBlocker(projectTasks, projectIssues) {
  const leaves = projectTasks.filter((t) => !projectTasks.some((x) => x.parentId === t.id))
  const progressed = leaves.filter((t) => (t.progress ?? 0) > 0).length
  if (progressed > 0) return `진척이 입력된 업무 ${progressed}건`
  if (projectIssues.length > 0) return `등록된 이슈 ${projectIssues.length}건`
  return null
}

/** 롤백 실행 — WBS·문서 테일러링을 지우고 프로젝트를 「수주」로 되돌린다. */
export function rollbackProjectTransition(project, { reason, by, today }) {
  removeProjectTasks(project.id)
  clearProjectDocs(project.id)
  rollbackTransition(project.id)
  log = [{ projectId: project.id, projectName: project.name, date: today, by, reason, prevDate: project.transitioned?.date }, ...log]
  emit()
}
