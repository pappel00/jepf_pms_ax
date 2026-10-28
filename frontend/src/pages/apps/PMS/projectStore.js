/**
 * PMS 프로젝트(사업) 스토어 — 서버(`/api/v1/pms/projects`)에서 읽고 저장한다. 수행 팀·승인된 단계전환·수행 종료 정보·운영 정보(운영 팀 포함)를 함께 읽어 화면 형태로 조립한다.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsOperationApi, pmsProjectsApi, pmsTeamsApi, pmsTransitionsApi } from '../../../api/entities'
import { contactCodeOfDbId, contactDbId, customerCodeOfDbId, customerDbId, ensureCustomersLoaded } from './customerStore'
import { ensurePmsUsersLoaded } from './pmsUsers'
import { teamFromResponse, teamToSpec } from './teamCodec'

/**
 * 사업 상태 — 영업(수주 전) 파이프라인 + 수행 + 종료.
 * 수주 이후(수행 중/완료)는 PMS의 WBS·진척 관리와 이어진다.
 * defaultProbability: 상태 변경 시 확률도 기본 제안값(사용자가 직접 고친 값이 우선).
 */
export const STATUSES = [
  { name: '공고 전', stage: '영업', defaultProbability: 10, desc: '사전 영업 단계. 공고가 나오기 전 발주 정보를 수집하는 중' },
  { name: '공고 중', stage: '영업', defaultProbability: 20, desc: '입찰 공고 게시. 참여 여부 판단 및 RFP 분석' },
  { name: '제안 중', stage: '영업', defaultProbability: 30, desc: '제안서 작성·검토 중 (제안 마감일 전)' },
  { name: '평가 중', stage: '영업', defaultProbability: 50, desc: '제안서 제출 완료, 발표·평가·결과 대기' },
  { name: '협상 중', stage: '영업', defaultProbability: 80, desc: '우선협상대상자 선정, 가격·계약 조건 협상' },
  { name: '수주', stage: '수주', defaultProbability: 100, desc: '계약 체결 완료, 착수 준비' },
  { name: '수행 중', stage: '수행', defaultProbability: 100, desc: '착수 후 진행 중 — WBS·진척 관리 대상' },
  { name: '수행 종료', stage: '수행', defaultProbability: 100, desc: '준공/검수 완료 — 운영단계 전환 대기' },
  { name: '운영 중', stage: '운영', defaultProbability: 100, desc: '운영단계 전환 완료 — 하자보수·유지보수 운영' },
  { name: '실패', stage: '종료', defaultProbability: 0, desc: '경쟁 탈락 등으로 수주 실패 (사유 기록)' },
  { name: '보류/취소', stage: '종료', defaultProbability: 0, desc: '발주사 사정으로 보류·취소 (사유 기록)' },
]

export const STATUS_NAMES = STATUSES.map((s) => s.name)
export const defaultProbabilityOf = (status) => STATUSES.find((s) => s.name === status)?.defaultProbability ?? 0
export const statusIndex = (status) => STATUS_NAMES.indexOf(status)

/** 파이프라인(수주 전 진행 건) 집계 대상 상태. */
export const PIPELINE_STATUSES = ['공고 전', '공고 중', '제안 중', '평가 중', '협상 중']
/** 수주 금액 집계 대상 상태. */
export const WON_STATUSES = ['수주', '수행 중', '수행 종료', '운영 중']

/** 실패/보류 사유 코드 — 백엔드에서는 공통코드(PMS_FAIL_REASON)로 관리한다. 상세 내용은 별도 텍스트로 남긴다. */
export const FAIL_REASON_CODES = ['가격 경쟁력 열세', '기술·제안 평가 열세', '경쟁사 선정', '발주사 사정(예산·일정 변경)', '발주 취소·보류', '내부 사정(수행 불가)', '기타']

export const BUSINESS_TYPES = ['SI 구축', 'SM 운영/유지보수', '컨설팅', '솔루션 공급', '기타']
export const BID_TYPES = ['공개 경쟁입찰', '제한 경쟁입찰', '협상에 의한 계약', '수의계약', '내부/그룹사']

let base = [] // 서버 데이터를 화면 형태로 바꾼 목록
let projects = []
let loaded = false
let loading = null
const listeners = new Set()

function emit() {
  projects = base
  listeners.forEach((l) => l())
}
function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/** 서버 응답 → 화면 형태. */
const fromApi = (a) => ({
  id: a.projectCode, dbId: a.id, name: a.projectName, client: a.clientName, status: a.statusCode, probability: Number(a.probabilityPct),
  customerId: customerCodeOfDbId(a.customerId), contactId: contactCodeOfDbId(a.contactId),
  contactName: a.contactName ?? '', contactTitle: a.contactTitle ?? '', contactPhone: a.contactPhone ?? '', contactEmail: a.contactEmail ?? '',
  start: a.startDate ?? '', end: a.endDate ?? '', amount: a.amount, vatIncluded: a.vatIncludedYn === 'Y',
  salesId: a.salesUserId, salesName: a.salesUserName ?? '', pmId: a.pmUserId ?? null, pmName: a.pmUserName ?? '',
  businessType: a.businessTypeCode ?? '', bidType: a.bidTypeCode ?? '', noticeNo: a.noticeNo ?? '',
  dueDate: a.dueDate ?? '', presentDate: a.presentDate ?? '', competitors: a.competitors ?? '',
  costEstimate: a.costEstimate ?? null, costSourceType: a.costSourceType ?? '', costSource: a.costSourceNote ?? '',
  failReasonCode: a.failReasonCode ?? '', failReason: a.failReasonDetail ?? '', viewRestricted: a.viewRestrictedYn === 'Y', memo: a.memo ?? '',
  execStart: a.execStartDate ?? '', execEnd: a.execEndDate ?? '', execEndMemo: a.execEndMemo ?? '',
})

/** 화면 형태 → 서버 요청. */
const toApi = (p) => ({
  projectName: p.name, statusCode: p.status, probabilityPct: Number(p.probability) || 0,
  customerId: customerDbId(p.customerId), contactId: contactDbId(p.contactId) || null,
  startDate: p.start, endDate: p.end, amount: p.amount ?? 0, vatIncludedYn: p.vatIncluded ? 'Y' : 'N',
  salesUserId: p.salesId, pmUserId: p.pmId || null,
  businessTypeCode: p.businessType || null, bidTypeCode: p.bidType || null, noticeNo: p.noticeNo || null,
  dueDate: p.dueDate || null, presentDate: p.presentDate || null, competitors: p.competitors || null,
  costEstimate: p.costEstimate ?? null, costSourceType: p.costEstimate ? p.costSourceType || 'MANUAL' : null, costSourceNote: p.costSource || null,
  failReasonCode: p.failReasonCode || null, failReasonDetail: p.failReason || null,
  viewRestrictedYn: p.viewRestricted ? 'Y' : 'N', memo: p.memo || null,
})

/**
 * 서버에서 프로젝트 목록을 다시 읽는다 — 고객사(코드 변환), 사용자 디렉터리, 수행 팀, 승인된 단계전환을 함께 읽어
 * 화면이 쓰는 transitioned({ date, rfpId, itemCount, phases, manager, team, restricted })를 조립한다.
 */
export function loadProjects() {
  if (loading) return loading
  loading = Promise.all([ensureCustomersLoaded(), ensurePmsUsersLoaded()])
    .then(() =>
      Promise.all([
        pmsProjectsApi.list('?size=1000&sort=projectCode'),
        pmsTeamsApi.list('EXEC').catch(() => ({ data: [] })),
        pmsTransitionsApi.list('?status=승인&kind=EXEC').catch(() => ({ data: [] })),
        pmsTeamsApi.list('OPS').catch(() => ({ data: [] })),
        pmsOperationApi.list().catch(() => ({ data: [] })),
        pmsOperationApi.features().catch(() => ({ data: [] })),
      ]),
    )
    .then(([projRes, teamRes, trRes, opsTeamRes, opRes, featRes]) => {
      const teamOf = Object.fromEntries((teamRes.data || []).map((x) => [x.projectId, teamFromResponse(x)]))
      const opsTeamOf = Object.fromEntries((opsTeamRes.data || []).map((x) => [x.projectId, teamFromResponse(x)]))
      const opOf = Object.fromEntries((opRes.data || []).map((x) => [x.projectId, x]))
      const featuresOf = {}
      ;(featRes.data || []).forEach((f) => (featuresOf[f.projectId] ??= []).push(f))
      // 프로젝트별 가장 최근 승인 건
      const approvedOf = {}
      ;(trRes.data || []).forEach((tr) => {
        if (!approvedOf[tr.projectId] || tr.requestedAt > approvedOf[tr.projectId].requestedAt) approvedOf[tr.projectId] = tr
      })
      base = (projRes.data || []).map((a) => {
        const p = fromApi(a)
        const team = teamOf[a.id]
        if (team && a.statusCode !== '수주') {
          const tr = approvedOf[a.id]
          p.transitioned = {
            date: a.execStartDate ?? (tr?.decidedAt ?? '').slice(0, 10),
            rfpId: tr?.rfpId ?? '',
            itemCount: tr?.itemCount ?? 0,
            phases: tr?.selection?.phases ?? ['분석', '설계', '개발', '테스트'],
            manager: team.manager,
            team: team.team,
            restricted: team.restricted,
          }
        }
        if (a.execEndDate) {
          p.completed = { date: a.execEndDate, memo: a.execEndMemo ?? '', features: (featuresOf[a.id] ?? []).map((f) => f.featureName), openIssues: 0 }
        }
        const op = opOf[a.id]
        if (op) p.operation = operationFromApi(op, opsTeamOf[a.id], featuresOf[a.id] ?? [])
        return p
      })
      loaded = true
      emit()
    })
    .catch((e) => {
      console.error('[projectStore] 프로젝트 목록 조회 실패:', e.message)
      loaded = true
      emit()
    })
    .finally(() => {
      loading = null
    })
  return loading
}

/** 훅 없이 현재 프로젝트 목록을 읽는다(다른 스토어가 계산에 쓴다). */
export const getProjects = () => projects
/** 서버 PK → 프로젝트 코드(화면 id). */
export const projectCodeOfDbId = (dbId) => base.find((p) => p.dbId === dbId)?.id ?? ''
export function useProjects() {
  useEffect(() => {
    if (!loaded) loadProjects()
  }, [])
  return useSyncExternalStore(subscribe, () => projects)
}

/** 저장(신규/수정). 실패하면 예외 — 서버 검증 메시지(errors)는 error.errors 로 온다. */
export async function saveProject(project) {
  await ensureCustomersLoaded()
  const body = toApi(project)
  const res = project.dbId ? await pmsProjectsApi.update(project.dbId, body) : await pmsProjectsApi.create(body)
  await loadProjects()
  return res.data
}

export async function deleteProject(id) {
  const p = base.find((x) => x.id === id)
  if (!p) return
  await pmsProjectsApi.remove(p.dbId)
  await loadProjects()
}

/** 상태를 화면에 즉시 반영하고 서버에도 저장한다(실패하면 서버 값으로 되돌리기 위해 다시 불러온다). */
function setStatus(id, status, memo) {
  const p = base.find((x) => x.id === id)
  if (!p) return
  base = base.map((x) => (x.id === id ? { ...x, status } : x))
  emit()
  pmsProjectsApi
    .changeStatus(p.dbId, { statusCode: status, memo: memo ?? null })
    .catch((e) => console.error('[projectStore] 상태 저장 실패:', e.message))
    .finally(() => loadProjects())
}

/** 서버 운영 정보 + 운영 팀 + 운영 인수 기능 → 화면의 operation. */
function operationFromApi(op, team, features) {
  const handover = op.handover ?? []
  return {
    date: op.transitionDate ?? '',
    type: op.opTypeCode,
    startDate: op.startDate,
    endDate: op.endDate,
    contractAmount: op.contractAmount ?? null,
    sla: { level: op.slaLevelCode, respond: op.slaRespondHours, resolve: op.slaResolveHours, hours: op.slaServiceHours ?? '' },
    manager: team?.manager ?? { name: '', role: '운영 PM' },
    team: team?.team ?? [],
    features: features.filter((f) => f.handover).map((f) => f.featureName),
    docs: handover.filter((h) => h.typeCode === 'DOC').map((h) => {
      const [phase, name, version] = h.itemName.split('|')
      return { phase, name, version }
    }),
    carriedIssues: handover.filter((h) => h.typeCode === 'ISSUE').map((h) => {
      const [title, priority] = h.itemName.split('|')
      return { id: h.refId, title, priority }
    }),
  }
}

const guard = async (fn, message) => {
  try {
    await fn()
  } catch (e) {
    window.alert(`${message}: ${e.message}`)
    throw e
  } finally {
    await loadProjects()
  }
}

/** 수행 종료 처리 — 프로젝트 관리자가 준공/검수 완료를 확정한다. 운영단계 인수 대상 기능 목록을 함께 남긴다. */
export async function endExecution(id, { date, memo, features }) {
  const p = base.find((x) => x.id === id)
  await guard(() => pmsOperationApi.endExecution(p.dbId, { date, memo: memo || null, features }), '수행 종료 처리하지 못했습니다')
}

/**
 * 운영단계전환 — 수행 종료 프로젝트를 「운영 중」으로 바꾸고 운영 기초데이터를 만든다.
 * operation: { type, startDate, endDate, contractAmount, sla, manager, team, features, docs: [{ refId, phase, name, version }],
 *              carriedIssues: [{ refId, title, priority }] }, nextOppIds: 차기 사업 기회 서버 PK(운영 영업 기회로 이관)
 */
export async function transitionToOperation(id, operation, nextOppIds = []) {
  const p = base.find((x) => x.id === id)
  await guard(() => pmsOperationApi.transition(p.dbId, operationBody(operation, nextOppIds)), '운영단계로 전환하지 못했습니다')
}

/** 운영 정보 수정(운영 관리자) — 형태·기간·계약 금액·SLA 와 운영 팀. */
export async function updateOperation(id, patch) {
  const p = base.find((x) => x.id === id)
  const cur = { ...p.operation, ...patch }
  await guard(() => pmsOperationApi.update(p.dbId, operationBody(cur, [])), '운영 정보를 저장하지 못했습니다')
}

const operationBody = (o, nextOppIds) => ({
  opTypeCode: o.type,
  startDate: o.startDate,
  endDate: o.endDate,
  contractAmount: o.contractAmount ?? null,
  slaLevelCode: o.sla.level,
  slaRespondHours: o.sla.respond,
  slaResolveHours: o.sla.resolve,
  slaServiceHours: o.sla.hours || null,
  team: teamToSpec({ manager: o.manager, team: o.team, restricted: false }),
  features: o.features ?? null,
  docs: (o.docs ?? []).map((d) => ({ refId: d.refId ?? null, phase: d.phase, name: d.name, version: d.version ?? null })),
  issues: (o.carriedIssues ?? []).map((i) => ({ refId: i.id ?? null, title: i.title, priority: i.priority ?? null })),
  nextOppIds,
})
/** 금액을 "12억 3,000만원" 식으로 읽기 쉽게. */
export function formatKoreanWon(n) {
  if (!n) return '-'
  const eok = Math.floor(n / 100000000)
  const man = Math.floor((n % 100000000) / 10000)
  const parts = []
  if (eok) parts.push(`${eok.toLocaleString()}억`)
  if (man) parts.push(`${man.toLocaleString()}만`)
  return `${parts.join(' ') || n.toLocaleString()}원`
}
