/**
 * 이슈 관리 스토어 — 서버(`/api/v1/pms/issues`)에서 읽고 저장한다.
 *
 * 요구사항 문서 3.9.3: 이슈는 Project 하위 공통 엔티티이고, WBS 업무는 선택 연결이다(이슈 1건 : 업무 N건).
 * 양방향 관리 — 이슈 화면에서 연결 업무를 지정/이동하고, WBS 업무에서 연결된 이슈를 조회·등록·연결한다.
 * 화면의 이슈 id 는 이슈 코드(ISS-###), dbId 가 서버 PK 이고, taskIds 는 WBS 업무 id(서버 PK)다.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsIssuesApi } from '../../../api/entities'
import { ensurePmsUsersLoaded, userIdByName } from './pmsUsers'
import { getProjects } from './projectStore'
import { getProposals, loadProposals } from './scheduleStore'

export const ISSUE_TYPES = ['일정', '범위/요구사항', '품질', '인력/자원', '기술', '고객/협의', '기타']
export const PRIORITIES = ['긴급', '높음', '보통', '낮음']
export const ISSUE_STATUSES = ['오픈', '진행 중', '해결', '종결']
export const isOpenStatus = (s) => s === '오픈' || s === '진행 중'
export const isHighPriority = (p) => p === '긴급' || p === '높음'

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
const fromApi = (i) => ({
  id: i.issueCode,
  dbId: i.id,
  projectId: i.projectCode,
  title: i.title,
  type: i.typeCode,
  priority: i.priorityCode,
  status: i.statusCode,
  reporter: i.reporterName ?? '',
  assignee: i.assigneeName ?? '',
  registeredDate: i.registeredDate ?? '',
  dueDate: i.dueDate ?? '',
  resolvedDate: i.resolvedDate ?? '',
  taskIds: i.taskIds ?? [],
  impactDays: i.impactDays ?? 0,
  content: i.content ?? '',
  resolution: i.resolution ?? '',
  history: (i.history ?? []).map((h) => ({ date: day(h.actedAt), author: h.actorName ?? '', text: h.text })),
})

let loading = null
export function loadIssues() {
  if (loading) return loading
  loading = ensurePmsUsersLoaded()
    .then(() => pmsIssuesApi.list())
    .then((res) => emit({ list: (res.data || []).map(fromApi), loaded: true }))
    .catch((e) => {
      console.error('[issueStore] 이슈 조회 실패:', e.message)
      emit({ ...state, loaded: true })
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useIssues() {
  useEffect(() => {
    if (!state.loaded) loadIssues()
  }, [])
  return useSyncExternalStore(subscribe, () => state).list
}

export function nextIssueId(all) {
  const max = all.reduce((s, i) => Math.max(s, Number(i.id.split('-')[1]) || 0), 0)
  return `ISS-${String(max + 1).padStart(3, '0')}`
}

/** 이슈 코드(ISS-###) ↔ 서버 PK 변환 — 다른 스토어(차기 사업 기회 등)가 이슈를 참조할 때 쓴다. */
export const issueDbIdOf = (code) => state.list.find((i) => i.id === code)?.dbId
export const issueCodeOfDbId = (dbId) => state.list.find((i) => i.dbId === dbId)?.id ?? ''

export const issuesOfTask = (all, taskId) => all.filter((i) => i.taskIds.includes(taskId))

/** 업무(들)에 연결된 이슈 집계: 전체/미해결/미해결 중 긴급·높음 (이슈 1건이 여러 업무에 걸려도 1건으로 센다). */
export function issueStats(all, taskIds) {
  const set = new Set(taskIds)
  const list = all.filter((i) => i.taskIds.some((t) => set.has(t)))
  const open = list.filter((i) => isOpenStatus(i.status))
  return { total: list.length, open: open.length, high: open.filter((i) => isHighPriority(i.priority)).length }
}

const toRequest = (it) => ({
  title: it.title,
  typeCode: it.type,
  priorityCode: it.priority,
  statusCode: it.status,
  reporterUserId: userIdByName(it.reporter) ?? null,
  assigneeUserId: userIdByName(it.assignee),
  registeredDate: it.registeredDate || null,
  dueDate: it.dueDate || null,
  impactDays: Number(it.impactDays) || 0,
  content: it.content,
  resolution: it.resolution || null,
  taskIds: it.taskIds ?? [],
})

const dbIdOf = (code) => state.list.find((i) => i.id === code)?.dbId

/**
 * 저장 — 신규면 등록, 아니면 수정한다. 등록/상태 변경 이력은 서버가 남긴다. 실패하면 예외를 던진다.
 * 열린 이슈가 「해결」로 바뀌어 서버가 일정 조정 제안을 만들었으면 그 제안을 돌려준다.
 */
export async function saveIssue(issue) {
  const prev = state.list.find((i) => i.id === issue.id)
  let res
  if (prev) {
    res = await pmsIssuesApi.update(prev.dbId, toRequest(issue))
  } else {
    const project = getProjects().find((p) => p.id === issue.projectId)
    if (!project) throw new Error('프로젝트를 찾을 수 없습니다.')
    res = await pmsIssuesApi.create(project.dbId, toRequest(issue))
  }
  await loadIssues()
  const code = res.data?.newProposalCode
  if (!code) return null
  await loadProposals()
  return getProposals().find((p) => p.id === code) ?? null
}

export async function deleteIssue(id) {
  await pmsIssuesApi.remove(dbIdOf(id))
  await loadIssues()
}

/** 업무 연결 추가 — 이슈 1건에 업무 N건을 연결할 수 있다(이미 연결된 업무는 서버가 무시). */
export async function linkIssue(id, taskId) {
  try {
    await pmsIssuesApi.link(dbIdOf(id), taskId)
  } catch (e) {
    window.alert(`업무를 연결하지 못했습니다: ${e.message}`)
  }
  await loadIssues()
}

/** 업무 연결 해제 — 해당 업무 하나만 끊는다. */
export async function unlinkIssue(id, taskId) {
  try {
    await pmsIssuesApi.unlink(dbIdOf(id), taskId)
  } catch (e) {
    window.alert(`연결을 해제하지 못했습니다: ${e.message}`)
  }
  await loadIssues()
}

export async function addHistory(id, text) {
  await pmsIssuesApi.addNote(dbIdOf(id), text)
  await loadIssues()
}
