/**
 * 투입 계획 변경 요청 · 승인 · 이력 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 수행 단계 「프로젝트 관리」에서 프로젝트 관리자가 프로젝트 관리자·투입 인력(투입 계획)을 바꾸면 바로 반영하지 않고
 * 변경 요청(사유 필수)을 서버에 만든다. 승인자(센터장·admin, 요청자 본인 제외)가 승인하면 팀이 바뀌고, 반려하면 반영되지 않는다.
 * 모든 요청은 변경 전/후 스냅샷과 함께 서버에 이력으로 남는다. (단계전환에서의 최초 지정은 단계전환 승인으로 반영된다.)
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsTeamChangesApi } from '../../../api/entities'
import { collectAssignments, monthlyPlan, overLimitRatio, personMonthly } from './assignmentStore'
import { getProjects, loadProjects } from './projectStore'
import { ensurePmsUsersLoaded } from './pmsUsers'
import { teamFromSpec, teamToSpec } from './teamCodec'

export const CHANGE_STATUSES = ['승인 대기', '승인', '반려', '취소']

const phasesText = (p) => (p.length === 4 ? '전체' : p.join('·'))
const brief = (m) => `${m.name}(${m.role}, ${phasesText(m.phases)}, M/M ${m.mm || '-'}, ${m.from || ''}~${m.to || ''})`

/** 변경 전/후 투입 계획의 차이를 사람이 읽는 문장으로. */
export function diffPlan(before, after) {
  const out = []
  if (before.manager.name !== after.manager.name || before.manager.role !== after.manager.role) {
    out.push(`프로젝트 관리자: ${before.manager.name}(${before.manager.role}) → ${after.manager.name}(${after.manager.role})`)
  }
  if (Boolean(before.restricted) !== Boolean(after.restricted)) {
    out.push(`멤버 외 열람 제한: ${before.restricted ? '켜짐' : '꺼짐'} → ${after.restricted ? '켜짐' : '꺼짐'}`)
  }
  after.team.forEach((m) => {
    const old = before.team.find((x) => x.name === m.name)
    if (!old) {
      out.push(`투입 추가: ${brief(m)}`)
      return
    }
    const changes = []
    if (old.role !== m.role) changes.push(`역할 ${old.role}→${m.role}`)
    if (phasesText(old.phases) !== phasesText(m.phases)) changes.push(`단계 ${phasesText(old.phases)}→${phasesText(m.phases)}`)
    if ((Number(old.mm) || 0) !== (Number(m.mm) || 0)) changes.push(`M/M ${old.mm || '-'}→${m.mm || '-'}`)
    if (old.from !== m.from || old.to !== m.to) changes.push(`기간 ${old.from}~${old.to}→${m.from}~${m.to}`)
    if ((old.access ?? '편집') !== (m.access ?? '편집')) changes.push(`접근 권한 ${old.access ?? '편집'}→${m.access ?? '편집'}`)
    if ((old.rate ?? '') !== (m.rate ?? '')) changes.push(`투입 비율 ${old.rate ?? '-'}%→${m.rate ?? '-'}%`)
    if (JSON.stringify(old.monthPlan ?? {}) !== JSON.stringify(m.monthPlan ?? {})) changes.push(`월별 조정 ${Object.keys(old.monthPlan ?? {}).length}건→${Object.keys(m.monthPlan ?? {}).length}건`)
    if (JSON.stringify(old.gaps ?? []) !== JSON.stringify(m.gaps ?? [])) changes.push(`투입 공백 ${(old.gaps ?? []).length}건→${(m.gaps ?? []).length}건`)
    if (changes.length) out.push(`투입 변경: ${m.name} — ${changes.join(', ')}`)
  })
  before.team.filter((x) => !after.team.some((m) => m.name === x.name)).forEach((x) => out.push(`투입 제외: ${brief(x)}`))
  return out
}

/** 변경을 적용했을 때 생기는 과투입(월 투입률 초과) 목록 — 승인 협의용. */
export function computeConflicts(projects, projectId, after) {
  const next = projects.map((p) => (p.id === projectId && p.transitioned ? { ...p, transitioned: { ...p.transitioned, ...after } } : p))
  const assignments = collectAssignments(next)
  const limit = overLimitRatio()
  const out = []
  after.team.forEach((m) => {
    const monthly = personMonthly(assignments, m.name)
    const mine = assignments.find((a) => a.baseProjectId === projectId && a.kind === '수행' && a.name === m.name)
    if (!mine) return
    Object.keys(monthlyPlan(mine)).forEach((k) => {
      if (monthly[k] > limit) out.push({ name: m.name, month: k, rate: monthly[k] })
    })
  })
  return out
}

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
/** 서버 응답 → 화면 형태. id 는 변경 요청 코드(CHG-연도-###), dbId 가 서버 PK, projectId 는 프로젝트 코드. */
const fromApi = (c) => ({
  id: c.changeCode,
  dbId: c.id,
  projectId: c.projectCode,
  projectName: c.projectName,
  requester: c.requesterName ?? '',
  requestedAt: day(c.requestedAt),
  reason: c.reason,
  before: teamFromSpec(c.before),
  after: teamFromSpec(c.after),
  summary: (c.summaryText ?? '').split('\n').filter(Boolean),
  status: c.statusCode,
  decidedBy: c.decidedByName ?? '',
  decidedAt: day(c.decidedAt),
  comment: c.decisionComment ?? '',
})

let loading = null
export function loadChanges() {
  if (loading) return loading
  loading = ensurePmsUsersLoaded()
    .then(() => pmsTeamChangesApi.list())
    .then((res) => emit({ list: (res.data || []).map(fromApi), loaded: true }))
    .catch((e) => {
      console.error('[changeStore] 변경 요청 조회 실패:', e.message)
      emit({ ...state, loaded: true })
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useChanges() {
  useEffect(() => {
    if (!state.loaded) loadChanges()
  }, [])
  return useSyncExternalStore(subscribe, () => state).list
}

export const pendingOf = (all, projectId) => all.find((r) => r.projectId === projectId && r.status === '승인 대기')

/** 변경 요청 등록 — 투입 계획은 아직 바뀌지 않는다. after 는 화면 팀({ manager, team, restricted }). */
export async function submitChange({ project, after, reason }) {
  const before = { manager: project.transitioned.manager, team: project.transitioned.team, restricted: !!project.transitioned.restricted }
  const summaryText = diffPlan(before, after).join('\n')
  const conflicts = computeConflicts(getProjects(), project.id, after)
  await pmsTeamChangesApi.request(project.dbId, {
    reason,
    after: teamToSpec(after),
    summaryText,
    conflictJson: JSON.stringify(conflicts),
  })
  await loadChanges()
}

/** 승인(→ 서버가 투입 계획에 반영) 또는 반려. 처리자는 로그인 사용자다. */
export async function decideChange(id, status, { comment }) {
  const c = state.list.find((x) => x.id === id)
  if (!c) return
  if (status === '승인') await pmsTeamChangesApi.approve(c.dbId, comment)
  else await pmsTeamChangesApi.reject(c.dbId, comment)
  if (status === '승인') await loadProjects()
  await loadChanges()
}

export async function cancelChange(id) {
  const c = state.list.find((x) => x.id === id)
  if (!c) return
  await pmsTeamChangesApi.cancel(c.dbId)
  await loadChanges()
}