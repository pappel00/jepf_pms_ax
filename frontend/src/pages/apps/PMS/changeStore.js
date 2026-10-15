/**
 * 투입 계획 변경 요청 · 승인 · 이력 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 수행 단계 「프로젝트 관리」에서 프로젝트 관리자가 프로젝트 관리자·투입 인력(투입 계획)을 바꾸면 바로 반영하지 않고
 * 변경 요청(사유 필수)을 만든다. 인력 관리자가 승인하면 반영되고, 반려하면 반영되지 않는다. 모든 요청은 이력으로 남는다.
 * (단계전환에서의 최초 지정은 승인 없이 반영된다.)
 */
import { useSyncExternalStore } from 'react'
import { collectAssignments, monthlyPlan, overLimitRatio, personMonthly } from './assignmentStore'
import { updateExecution } from './projectStore'

export const CHANGE_STATUSES = ['승인 대기', '승인', '반려', '취소']

const phasesText = (p) => (p.length === 4 ? '전체' : p.join('·'))
const brief = (m) => `${m.name}(${m.role}, ${phasesText(m.phases)}, M/M ${m.mm || '-'}, ${m.from || ''}~${m.to || ''})`

/** 변경 전/후 투입 계획의 차이를 사람이 읽는 문장으로. */
export function diffPlan(before, after) {
  const out = []
  if (before.manager.name !== after.manager.name || before.manager.role !== after.manager.role) {
    out.push(`프로젝트 관리자: ${before.manager.name}(${before.manager.role}) → ${after.manager.name}(${after.manager.role})`)
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

const seedBefore = {
  manager: { name: '관리자', role: 'PM' },
  team: [
    { name: '이설계', role: '분석/설계', phases: ['분석', '설계'], mm: 4, from: '2026-09-01', to: '2026-12-31', notify: '' },
    { name: '김개발', role: '개발', phases: ['개발', '테스트'], mm: 6, from: '2026-10-01', to: '2027-03-31', notify: '' },
    { name: '정QA', role: 'QA', phases: ['테스트'], mm: 3, from: '2026-11-02', to: '2027-02-26', notify: '' },
  ],
}
const seedAfter = {
  manager: { name: '관리자', role: 'PM' },
  team: [...seedBefore.team, { name: '박협력', role: '개발', phases: ['개발'], mm: 4.5, from: '2026-10-06', to: '2027-02-26', notify: 'Mattermost' }],
}
const p9Before = {
  manager: { name: '이설계', role: 'PL' },
  team: [
    { name: '김개발', role: '개발', phases: ['개발', '테스트'], mm: 2, from: '2026-09-01', to: '2026-12-31', notify: '' },
    { name: '정QA', role: 'QA', phases: ['테스트'], mm: 1.5, from: '2026-10-01', to: '2026-12-31', notify: '' },
  ],
}
const p9After = {
  manager: p9Before.manager,
  team: [...p9Before.team, { name: '박협력', role: '개발', phases: ['개발'], mm: 1.5, from: '2026-11-02', to: '2026-12-31', notify: 'Mattermost' }],
}

let requests = [
  {
    id: 'CHG-2026-001', projectId: 'PRJ-2026-001', projectName: '통합업무포탈 구축사업', requester: '관리자', requestedAt: '2026-10-02',
    reason: '협력사 개발 인력 1명 추가 투입(개발 일정 지연 대응)', before: seedBefore, after: seedAfter,
    summary: diffPlan(seedBefore, seedAfter), status: '승인', decidedBy: '인력 관리자', decidedAt: '2026-10-05', comment: '과투입 없음, 승인',
  },
  {
    id: 'CHG-2026-002', projectId: 'PRJ-2026-009', projectName: '그룹사 인트라넷 운영', requester: '이설계', requestedAt: '2026-10-12',
    reason: '운영 요청 증가로 개발 인력 추가 필요 (11~12월)', before: p9Before, after: p9After,
    summary: diffPlan(p9Before, p9After), status: '승인 대기', decidedBy: '', decidedAt: '', comment: '',
  },
]
const listeners = new Set()

function emit(next) {
  requests = next
  listeners.forEach((l) => l())
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function useChanges() {
  return useSyncExternalStore(subscribe, () => requests)
}

export const pendingOf = (all, projectId) => all.find((r) => r.projectId === projectId && r.status === '승인 대기')

export function nextChangeId(all) {
  const max = all.reduce((s, r) => Math.max(s, Number(r.id.split('-')[2]) || 0), 0)
  return `CHG-${new Date().getFullYear()}-${String(max + 1).padStart(3, '0')}`
}

/** 변경 요청 등록 — 투입 계획은 아직 바뀌지 않는다. */
export function submitChange({ project, after, reason, requester, today }) {
  const before = { manager: project.transitioned.manager, team: project.transitioned.team }
  const req = {
    id: nextChangeId(requests), projectId: project.id, projectName: project.name, requester, requestedAt: today,
    reason, before, after, summary: diffPlan(before, after), status: '승인 대기', decidedBy: '', decidedAt: '', comment: '',
  }
  emit([req, ...requests])
  return req
}

/** 승인(→ 투입 계획에 반영) 또는 반려. */
export function decideChange(id, status, { by, comment, today }) {
  const req = requests.find((r) => r.id === id)
  if (!req || req.status !== '승인 대기') return
  if (status === '승인') updateExecution(req.projectId, req.after)
  emit(requests.map((r) => (r.id === id ? { ...r, status, decidedBy: by, decidedAt: today, comment } : r)))
}

export function cancelChange(id, { by, today }) {
  emit(requests.map((r) => (r.id === id && r.status === '승인 대기' ? { ...r, status: '취소', decidedBy: by, decidedAt: today } : r)))
}
