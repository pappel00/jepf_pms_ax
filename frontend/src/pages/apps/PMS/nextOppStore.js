/**
 * 차기 사업 기회 관리 스토어 — 서버(`/api/v1/pms/next-opps`)에서 읽고 저장한다.
 *
 * 수행 중에 나온 현재 요구사항 범위 밖의 추가 요구·후속 단계·확산 가능성을 모아 다음 사업 기회로 관리한다.
 * 운영단계전환 때 선택한 기회는 서버가 「운영 영업 관리」의 영업 기회로 이관한다(고객사 담당자 정보 포함).
 * 화면 id 는 기회 코드(NB-###), 프로젝트·고객사 담당자·수행 이슈·운영 영업 기회는 각 코드로 다룬다.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsNextOppsApi } from '../../../api/entities'
import { contactCodeOfDbId, contactDbId, ensureCustomersLoaded } from './customerStore'
import { issueCodeOfDbId, issueDbIdOf, loadIssues } from './issueStore'
import { loadOps, oppCodeOfDbId } from './opsStore'
import { ensurePmsUsersLoaded, userIdByName } from './pmsUsers'
import { getProjects } from './projectStore'

export const NEXT_KINDS = ['추가 기능·범위 외 요구', '후속 단계(차기 사업)', '고도화·개선', '타 부서·계열사 확산', '인프라·증설', '운영·유지보수 전환', '컨설팅·교육', '기타']
export const NEXT_SOURCES = ['고객 요청', '현장 발견', '수행 이슈', '영업 제안', '기타']
export const NEXT_STAGES = ['발굴', '검토 중', '제안 준비', '제안 완료', '보류/실패', '이관 완료']
export const BUDGET_STATUSES = ['확보', '검토 중', '미정']
export const PRIORITIES = ['높음', '보통', '낮음']
export const isOpenNext = (o) => !['보류/실패', '이관 완료'].includes(o.stage)

let list = []
let loaded = false
const listeners = new Set()
const emit = (next) => {
  list = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}

const fromApi = (o) => ({
  id: o.oppCode,
  dbId: o.id,
  projectId: o.projectCode,
  title: o.title,
  kind: o.kindCode,
  source: o.sourceCode,
  content: o.content ?? '',
  contactId: contactCodeOfDbId(o.requesterContactId),
  decisionMakerId: contactCodeOfDbId(o.decisionMakerContactId),
  budget: o.budgetCode ?? '미정',
  estMM: o.estMm === null || o.estMm === undefined ? null : Number(o.estMm),
  amount: o.amount ?? null,
  quarter: o.quarter ?? '',
  stage: o.stageCode,
  priority: o.priorityCode,
  owner: o.ownerName ?? '',
  issueId: issueCodeOfDbId(o.issueId),
  nextAction: o.nextAction ?? '',
  nextDate: o.nextDate ?? '',
  scopeReason: o.scopeReason ?? '',
  memo: o.memo ?? '',
  opsOppId: oppCodeOfDbId(o.opsOpportunityId),
})

let loading = null
export function loadNextOpps() {
  if (loading) return loading
  loading = Promise.all([ensureCustomersLoaded(), ensurePmsUsersLoaded(), loadOps(), loadIssues()])
    .then(() => pmsNextOppsApi.list())
    .then((res) => {
      loaded = true
      emit((res.data || []).map(fromApi))
    })
    .catch((e) => {
      console.error('[nextOppStore] 차기 사업 기회 조회 실패:', e.message)
      loaded = true
      emit(list)
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useNextOpps() {
  useEffect(() => {
    if (!loaded) loadNextOpps()
  }, [])
  return useSyncExternalStore(subscribe, () => list)
}

export const emptyNext = (projectId, owner) => ({
  id: '', projectId, title: '', kind: NEXT_KINDS[0], source: NEXT_SOURCES[0], content: '', contactId: '', decisionMakerId: '', budget: '미정', estMM: null, amount: null,
  quarter: '', stage: '발굴', priority: '보통', owner, issueId: '', nextAction: '', nextDate: '', scopeReason: '', memo: '', opsOppId: '',
})

const toApi = (o) => ({
  title: o.title,
  kindCode: o.kind,
  sourceCode: o.source,
  content: o.content || null,
  requesterContactId: contactDbId(o.contactId) ?? null,
  decisionMakerContactId: contactDbId(o.decisionMakerId) ?? null,
  budgetCode: o.budget || null,
  estMm: o.estMM ?? null,
  amount: o.amount ?? null,
  quarter: o.quarter || null,
  stageCode: o.stage,
  priorityCode: o.priority,
  ownerUserId: userIdByName(o.owner),
  issueId: issueDbIdOf(o.issueId) ?? null,
  nextAction: o.nextAction || null,
  nextDate: o.nextDate || null,
  scopeReason: o.scopeReason || null,
  memo: o.memo || null,
})

export async function saveNext(o) {
  const existing = list.find((x) => x.id === o.id)
  try {
    if (existing) await pmsNextOppsApi.update(existing.dbId, toApi(o))
    else await pmsNextOppsApi.create(getProjects().find((p) => p.id === o.projectId).dbId, toApi(o))
  } catch (e) {
    window.alert(`차기 사업 기회를 저장하지 못했습니다: ${e.message}`)
  }
  await loadNextOpps()
}

export async function deleteNext(id) {
  try {
    await pmsNextOppsApi.remove(list.find((o) => o.id === id).dbId)
  } catch (e) {
    window.alert(`삭제하지 못했습니다: ${e.message}`)
  }
  await loadNextOpps()
}

/** 화면의 기회 코드 → 서버 PK (운영단계전환 요청에 쓴다). */
export const nextDbIdOf = (code) => list.find((o) => o.id === code)?.dbId
