/**
 * 운영 관리(운영 업무 · 운영 이슈 · 운영 영업) 스토어 — 서버(`/api/v1/pms/ops-tasks` · `ops-incidents` · `ops-opps`)에서 읽고 저장한다.
 *
 * 운영 중인 프로젝트(운영단계전환을 마친 프로젝트)별로 운영 업무, 장애·문의 등 운영 이슈, 운영 중 발생하는 영업 기회를 관리한다.
 * 운영 이슈의 SLA 시한은 프로젝트의 운영 정보(SLA 응답·해결 목표 시간)에서 계산한다(저장하지 않음).
 * 반복 업무를 완료하면 다음 주기 업무를 서버가 자동 생성한다. 화면 id 는 코드(OT-/OI-/OS-###), dbId 가 서버 PK 다.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsOpsIncidentsApi, pmsOpsOppsApi, pmsOpsTasksApi } from '../../../api/entities'
import { contactCodeOfDbId, contactDbId, ensureCustomersLoaded } from './customerStore'
import { ensurePmsUsersLoaded, userIdByName } from './pmsUsers'
import { getProjects, useProjects } from './projectStore'

export const NOW = '2026-10-13 10:00' // 목업 기준 현재 시각(pmsStore.TODAY와 같은 날)

/* ---------- 공통 ---------- */
export const useOpsProjects = () => useProjects().filter((p) => p.status === '운영 중' && p.operation)

export const opsMembers = (p) => [...new Set([p?.operation?.manager?.name, ...(p?.operation?.team ?? []).map((m) => m.name)].filter(Boolean))]

const make = (initial) => {
  let state = initial
  const listeners = new Set()
  return {
    get: () => state,
    set: (next) => {
      state = next
      listeners.forEach((l) => l())
    },
    use: () =>
      useSyncExternalStore(
        (l) => {
          listeners.add(l)
          return () => listeners.delete(l)
        },
        () => state,
      ),
  }
}
const tasks = make([])
const incidents = make([])
const opps = make([])
let loaded = false

const projectDbId = (code) => getProjects().find((p) => p.id === code)?.dbId
/** 서버 PK → 화면 코드(교차 참조 변환용). 세 목록을 함께 읽은 뒤 채워진다. */
let taskCodes = {}
let incidentCodes = {}
let oppCodes = {}
const toApiDate = (s) => (s ? s.replace(' ', 'T') : null)
const dt = (s) => (s ? s.slice(0, 16).replace('T', ' ') : '')

/** 운영 업무·이슈·영업 기회를 한꺼번에 읽는다(서로 참조하므로 같이 변환). */
let loading = null
export function loadOps() {
  if (loading) return loading
  loading = Promise.all([ensureCustomersLoaded(), ensurePmsUsersLoaded()])
    .then(() => Promise.all([pmsOpsTasksApi.list(), pmsOpsIncidentsApi.list(), pmsOpsOppsApi.list()]))
    .then(([t, i, o]) => {
      taskCodes = Object.fromEntries((t.data || []).map((x) => [x.id, x.taskCode]))
      incidentCodes = Object.fromEntries((i.data || []).map((x) => [x.id, x.incidentCode]))
      oppCodes = Object.fromEntries((o.data || []).map((x) => [x.id, x.oppCode]))
      tasks.set(
        (t.data || []).map((x) => ({
          id: x.taskCode, dbId: x.id, projectId: x.projectCode, title: x.title, category: x.categoryCode, assignee: x.assigneeName ?? '', due: x.dueDate ?? '',
          status: x.statusCode, priority: x.priorityCode, repeat: x.repeatCode ?? '없음', repeatEnd: x.repeatEndDate ?? '', prevTaskId: taskCodes[x.prevTaskId] ?? '',
          autoCreated: x.autoCreated, memo: x.memo ?? '', incidentId: incidentCodes[x.incidentId] ?? '',
        })),
      )
      incidents.set(
        (i.data || []).map((x) => ({
          id: x.incidentCode, dbId: x.id, projectId: x.projectCode, type: x.typeCode, title: x.title, priority: x.priorityCode, status: x.statusCode,
          reporter: x.reporterName, assignee: x.assigneeName ?? '', receivedAt: dt(x.receivedAt), respondedAt: dt(x.respondedAt), resolvedAt: dt(x.resolvedAt),
          content: x.content ?? '', action: x.actionText ?? '', opportunityId: oppCodes[x.opportunityId] ?? '',
        })),
      )
      opps.set(
        (o.data || []).map((x) => ({
          id: x.oppCode, dbId: x.id, projectId: x.projectCode, kind: x.kindCode, title: x.title, stage: x.stageCode, amount: x.amount ?? null,
          probability: Number(x.probabilityPct) || 0, owner: x.ownerName ?? '', expectedDate: x.expectedDate ?? '', nextAction: x.nextAction ?? '',
          nextDate: x.nextDate ?? '', memo: x.memo ?? '', sourceIncidentId: incidentCodes[x.sourceIncidentId] ?? '', contactId: contactCodeOfDbId(x.contactId),
          sourceNextId: x.sourceNextOppId ?? '',
        })),
      )
      loaded = true
    })
    .catch((e) => {
      console.error('[opsStore] 운영 데이터 조회 실패:', e.message)
      loaded = true
    })
    .finally(() => {
      loading = null
    })
  return loading
}
/** 운영 영업 기회 서버 PK → 코드(차기 사업 기회의 「이관 완료」 표시용). */
export const oppCodeOfDbId = (dbId) => oppCodes[dbId] ?? ''

const useLoaded = (store) => () => {
  useEffect(() => {
    if (!loaded) loadOps()
  }, [])
  return store.use()
}

const guard = async (fn, message) => {
  try {
    return await fn()
  } catch (e) {
    window.alert(`${message}: ${e.message}`)
    return null
  } finally {
    await loadOps()
  }
}

/* ---------- 운영 업무 ---------- */
export const TASK_CATEGORIES = ['정기 점검', '패치·배포', '장애 조치', '문의 대응', '요청 작업', '보고', '기타']
export const TASK_STATUSES = ['대기', '진행 중', '완료', '보류']
export const REPEATS = ['없음', '매주', '매월', '분기']
export const PRIORITIES = ['긴급', '높음', '보통', '낮음']

export const useOpsTasks = useLoaded(tasks)
export const emptyTask = (projectId, assignee) => ({ id: '', projectId, title: '', category: '요청 작업', assignee, due: '', status: '대기', priority: '보통', repeat: '없음', repeatEnd: '', prevTaskId: '', autoCreated: false, memo: '', incidentId: '' })

const taskBody = (t) => ({
  title: t.title,
  categoryCode: t.category,
  assigneeUserId: userIdByName(t.assignee),
  dueDate: t.due,
  statusCode: t.status,
  priorityCode: t.priority,
  repeatCode: t.repeat || '없음',
  repeatEndDate: t.repeatEnd || null,
  memo: t.memo || null,
  incidentId: incidents.get().find((i) => i.id === t.incidentId)?.dbId ?? null,
})

/** 서버가 반복 업무의 다음 업무를 만들었으면 { created }, 만들지 않았으면 { skipped: 사유 }, 해당 없으면 null. */
const spawnOf = (res) => {
  const d = res?.data
  if (!d) return null
  if (d.spawnedTaskCode) return { created: { id: d.spawnedTaskCode, title: d.title, due: nextDueText(d) } }
  if (d.skippedReason) return { skipped: d.skippedReason }
  return null
}
const nextDueText = (d) => {
  const [y, m, day] = d.dueDate.split('-').map(Number)
  const add = d.repeatCode === '매주' ? 7 : 0
  const dt0 = new Date(Date.UTC(y, m - 1, day + add))
  if (d.repeatCode === '매월') dt0.setUTCMonth(dt0.getUTCMonth() + 1)
  if (d.repeatCode === '분기') dt0.setUTCMonth(dt0.getUTCMonth() + 3)
  return dt0.toISOString().slice(0, 10)
}

/** 저장 — 완료로 저장하면 반복 업무의 다음 업무를 서버가 자동 생성한다. 결과(spawn)를 돌려준다. */
export async function saveTask(t) {
  const existing = tasks.get().find((x) => x.id === t.id)
  const res = await guard(() => (existing ? pmsOpsTasksApi.update(existing.dbId, taskBody(t)) : pmsOpsTasksApi.create(projectDbId(t.projectId), taskBody(t))), '운영 업무를 저장하지 못했습니다')
  return spawnOf(res)
}
export const deleteTask = (id) => guard(() => pmsOpsTasksApi.remove(tasks.get().find((t) => t.id === id).dbId), '삭제하지 못했습니다')
export async function setTaskStatus(id, status) {
  const res = await guard(() => pmsOpsTasksApi.setStatus(tasks.get().find((t) => t.id === id).dbId, status), '상태를 바꾸지 못했습니다')
  return spawnOf(res)
}

/* ---------- 운영 이슈(장애·문의·개선 요청) ---------- */
export const INCIDENT_TYPES = ['장애', '문의', '개선 요청', '기타']
export const INCIDENT_STATUSES = ['접수', '분석', '조치 중', '해결', '종결']
export const isOpenIncident = (s) => s !== '해결' && s !== '종결'

export const useOpsIncidents = useLoaded(incidents)
export const emptyIncident = (projectId, reporter) => ({ id: '', projectId, type: '문의', title: '', priority: '보통', status: '접수', reporter, assignee: '', receivedAt: NOW, respondedAt: '', resolvedAt: '', content: '', action: '', opportunityId: '' })

export async function saveIncident(i) {
  const existing = incidents.get().find((x) => x.id === i.id)
  const next = { ...i }
  // 처리 상태에 맞춰 응답/해결 시각을 자동 기록(목업 기준 현재 시각)
  if (!next.respondedAt && next.status !== '접수') next.respondedAt = NOW
  if (!next.resolvedAt && !isOpenIncident(next.status)) next.resolvedAt = NOW
  if (existing && isOpenIncident(next.status)) next.resolvedAt = ''
  const body = {
    typeCode: next.type,
    title: next.title,
    content: next.content || null,
    priorityCode: next.priority,
    statusCode: next.status,
    reporterName: next.reporter,
    assigneeUserId: userIdByName(next.assignee) ?? null,
    receivedAt: toApiDate(next.receivedAt),
    respondedAt: toApiDate(next.respondedAt),
    resolvedAt: toApiDate(next.resolvedAt),
    actionText: next.action || null,
  }
  await guard(() => (existing ? pmsOpsIncidentsApi.update(existing.dbId, body) : pmsOpsIncidentsApi.create(projectDbId(next.projectId), body)), '운영 이슈를 저장하지 못했습니다')
}
export const deleteIncident = (id) => guard(() => pmsOpsIncidentsApi.remove(incidents.get().find((i) => i.id === id).dbId), '삭제하지 못했습니다')
/** 운영 이슈에서 만든 운영 영업 기회를 연결한다(opportunityId = 영업 기회 코드). */
export const linkOpportunity = (id, opportunityId) =>
  guard(() => pmsOpsIncidentsApi.linkOpportunity(incidents.get().find((i) => i.id === id).dbId, opps.get().find((o) => o.id === opportunityId)?.dbId ?? null), '연결하지 못했습니다')

const ms = (s) => new Date(s.replace(' ', 'T') + ':00').getTime()
const addHours = (s, h) => ms(s) + h * 3600000
/**
 * SLA 판정 — 응답 시한(접수 + 응답 목표 시간), 해결 시한(접수 + 해결 목표 시간).
 * 반환: { respondDue, resolveDue, respond: '충족'|'위반'|'임박'|'정상', resolve: ... } (장애·문의만 SLA 대상)
 */
export function slaOf(incident, sla) {
  if (!sla || incident.type === '개선 요청' || incident.type === '기타') return null
  const now = ms(NOW)
  const judge = (due, doneAt) => {
    if (doneAt) return ms(doneAt) <= due ? '충족' : '위반'
    if (now > due) return '위반'
    return due - now <= 3600000 * 2 ? '임박' : '정상'
  }
  const respondDue = addHours(incident.receivedAt, sla.respond)
  const resolveDue = addHours(incident.receivedAt, sla.resolve)
  return {
    respondDue, resolveDue,
    respond: judge(respondDue, incident.respondedAt),
    resolve: judge(resolveDue, incident.resolvedAt),
  }
}
export const fmtDue = (t) => new Date(t + 9 * 3600000).toISOString().slice(5, 16).replace('T', ' ')

/* ---------- 운영 영업(재계약·추가 개발·유상 개선 등) ---------- */
export const OPP_KINDS = ['재계약·갱신', '추가 개발', '유상 개선', '증설·확장', '교육·컨설팅', '신규 제안']
export const OPP_STAGES = ['발굴', '제안', '협상', '수주', '실패/보류']
export const OPP_STAGE_PROB = { 발굴: 10, 제안: 30, 협상: 70, 수주: 100, '실패/보류': 0 }

export const useOpsOpps = useLoaded(opps)
export const emptyOpp = (projectId, owner) => ({ id: '', projectId, kind: '유상 개선', title: '', stage: '발굴', amount: null, probability: 10, owner, expectedDate: '', nextAction: '', nextDate: '', memo: '', sourceIncidentId: '', contactId: '', sourceNextId: '' })

/** 저장 — 서버가 채번한 영업 기회 코드를 돌려준다(운영 이슈에서 만든 기회를 이슈에 연결할 때 쓴다). */
export async function saveOpp(o) {
  const existing = opps.get().find((x) => x.id === o.id)
  const body = {
    kindCode: o.kind,
    title: o.title,
    stageCode: o.stage,
    amount: o.amount ?? null,
    probabilityPct: o.probability ?? 0,
    ownerUserId: userIdByName(o.owner),
    expectedDate: o.expectedDate || null,
    nextAction: o.nextAction || null,
    nextDate: o.nextDate || null,
    memo: o.memo || null,
    sourceIncidentId: incidents.get().find((i) => i.id === o.sourceIncidentId)?.dbId ?? null,
    contactId: contactDbId(o.contactId) ?? null,
    sourceNextOppId: null,
  }
  const res = await guard(() => (existing ? pmsOpsOppsApi.update(existing.dbId, body) : pmsOpsOppsApi.create(projectDbId(o.projectId), body)), '영업 기회를 저장하지 못했습니다')
  return res?.data?.oppCode ?? o.id
}
export const deleteOpp = (id) => guard(() => pmsOpsOppsApi.remove(opps.get().find((o) => o.id === id).dbId), '삭제하지 못했습니다')
