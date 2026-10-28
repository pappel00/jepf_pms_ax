/**
 * 수행단계전환 요청 · 승인 · 롤백 스토어 — module-pms API(/api/v1/pms/transitions) 기반.
 *
 * 단계전환 실행은 「전환 요청」(승인 대기)을 서버에 만든다. 센터장·admin 이 승인하면 서버가 프로젝트 관리자·투입 인력을
 * 확정하고 프로젝트를 「수행 중」으로 바꾸며, 이 화면이 이어서 WBS·문서 테일러링 기초데이터를 만든다(서버 테이블은 3단계 —
 * 그 전까지는 메모리 목업). 반려/취소하면 아무것도 바뀌지 않는다. 전환 후에도 진척 입력·이슈가 없으면 사유를 남기고
 * 롤백(수주 상태로 복귀)할 수 있다. 모든 처리는 서버에 이력으로 남는다.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsTransitionsApi } from '../../../api/entities'
import { clearProjectDocs, initTailoring } from './docStore'
import { addProjectTasks, loadTasks } from './pmsStore'
import { getProjects, loadProjects } from './projectStore'
import { buildTransitionArtifacts } from './transitionBuild'

export const TRANSITION_STATUSES = ['승인 대기', '승인', '반려', '취소', '롤백']

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
/** 서버 응답 → 화면 형태. id 는 전환 코드(TRN-연도-###), dbId 가 서버 PK, projectId 는 프로젝트 코드. */
const fromApi = (t) => ({
  id: t.transitionCode,
  dbId: t.id,
  projectId: t.projectCode,
  projectName: t.projectName,
  requester: t.requesterName ?? '',
  requestedAt: day(t.requestedAt),
  summary: t.summary ?? '',
  status: t.statusCode,
  decidedBy: t.approverName ?? '',
  decidedAt: day(t.decidedAt),
  comment: t.decisionComment ?? '',
  selection: t.selection ?? {},
  rfpId: t.rfpId ?? '',
  itemCount: t.itemCount ?? 0,
  rolledBackBy: t.rolledBackByName ?? '',
  rolledBackAt: day(t.rolledBackAt),
  rollbackReason: t.rollbackReason ?? '',
})

let loading = null
export function loadTransitions() {
  if (loading) return loading
  loading = pmsTransitionsApi
    .list('?kind=EXEC')
    .then((res) => emit({ list: (res.data || []).map(fromApi), loaded: true }))
    .catch((e) => {
      console.error('[transitionStore] 단계전환 요청 조회 실패:', e.message)
      emit({ ...state, loaded: true })
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useTransitionRequests() {
  useEffect(() => {
    if (!state.loaded) loadTransitions()
  }, [])
  return useSyncExternalStore(subscribe, () => state).list
}

/** 롤백 이력 — 롤백된 전환 요청에서 만든다. */
export function useRollbackLog() {
  return useTransitionRequests()
    .filter((r) => r.status === '롤백')
    .map((r) => ({ projectId: r.projectId, projectName: r.projectName, date: r.rolledBackAt, by: r.rolledBackBy, reason: r.rollbackReason, prevDate: r.decidedAt }))
}

export const pendingTransitionOf = (all, projectId) => all.find((r) => r.projectId === projectId && r.status === '승인 대기')

/** 단계전환 요청 등록 — request 는 서버 요청 본문(phases, rfpId, itemIds, itemCount, assignMode, summary, team). */
export async function requestTransition(project, request) {
  await pmsTransitionsApi.request(project.dbId, request)
  await loadTransitions()
}

/** 승인 — 서버가 팀을 확정하고 「수행 중」으로 바꾼 뒤, 이 화면이 WBS·문서 테일러링 기초데이터를 만든다. */
export async function approveTransition(id) {
  const req = state.list.find((r) => r.id === id)
  if (!req) return
  await pmsTransitionsApi.approve(req.dbId)
  await loadProjects()
  const project = getProjects().find((p) => p.id === req.projectId)
  if (project) {
    const { tasks, tailoring } = buildTransitionArtifacts(project, req.selection)
    await addProjectTasks(project, tasks)
    await initTailoring(project, tailoring)
  }
  await loadTransitions()
}

export async function rejectTransition(id, comment) {
  const req = state.list.find((r) => r.id === id)
  if (!req) return
  await pmsTransitionsApi.reject(req.dbId, comment)
  await loadTransitions()
}

export async function cancelTransition(id) {
  const req = state.list.find((r) => r.id === id)
  if (!req) return
  await pmsTransitionsApi.cancel(req.dbId)
  await loadTransitions()
}

/** 롤백 가능 여부 — 진척이 입력됐거나 이슈가 있으면 불가. 불가 사유 문자열 또는 null. */
export function rollbackBlocker(projectTasks, projectIssues) {
  const leaves = projectTasks.filter((t) => !projectTasks.some((x) => x.parentId === t.id))
  const progressed = leaves.filter((t) => (t.progress ?? 0) > 0).length
  if (progressed > 0) return `진척이 입력된 업무 ${progressed}건`
  if (projectIssues.length > 0) return `등록된 이슈 ${projectIssues.length}건`
  return null
}

/** 롤백 — 서버에서 팀을 지우고 「수주」로 되돌린 뒤, 메모리의 WBS·문서 테일러링도 지운다. */
export async function rollbackProjectTransition(project, { reason }) {
  const req = state.list.find((r) => r.projectId === project.id && r.status === '승인')
  if (!req) throw new Error('롤백할 승인된 단계전환 이력이 없습니다.')
  await pmsTransitionsApi.rollback(req.dbId, reason)
  await loadTasks()
  await clearProjectDocs()
  await loadProjects()
  await loadTransitions()
}
