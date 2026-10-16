/**
 * 차기 사업 기회 관리 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 수행 중에 나온 현재 요구사항 범위 밖의 추가 요구·후속 단계·확산 가능성을 모아 다음 사업 기회로 관리한다.
 * 운영단계전환 때 선택한 기회는 「운영 영업 관리」의 영업 기회로 이관된다(고객사 담당자 정보 포함).
 */
import { useSyncExternalStore } from 'react'
import { saveOpp } from './opsStore'

export const NEXT_KINDS = ['추가 기능·범위 외 요구', '후속 단계(차기 사업)', '고도화·개선', '타 부서·계열사 확산', '인프라·증설', '운영·유지보수 전환', '컨설팅·교육', '기타']
export const NEXT_SOURCES = ['고객 요청', '현장 발견', '수행 이슈', '영업 제안', '기타']
export const NEXT_STAGES = ['발굴', '검토 중', '제안 준비', '제안 완료', '보류/실패', '이관 완료']
export const BUDGET_STATUSES = ['확보', '검토 중', '미정']
export const PRIORITIES = ['높음', '보통', '낮음']
export const isOpenNext = (o) => !['보류/실패', '이관 완료'].includes(o.stage)

// 운영 영업 관리의 유형으로 옮길 때의 대응, 단계별 확률도
const OPS_KIND = {
  '추가 기능·범위 외 요구': '유상 개선', '후속 단계(차기 사업)': '신규 제안', '고도화·개선': '추가 개발', '타 부서·계열사 확산': '증설·확장',
  '인프라·증설': '증설·확장', '운영·유지보수 전환': '재계약·갱신', '컨설팅·교육': '교육·컨설팅', 기타: '신규 제안',
}
const STAGE_PROB = { 발굴: 10, '검토 중': 20, '제안 준비': 30, '제안 완료': 50 }
const quarterEnd = (q) => {
  const m = /^(\d{4})-Q([1-4])$/.exec(q ?? '')
  return m ? `${m[1]}-${['03-31', '06-30', '09-30', '12-31'][Number(m[2]) - 1]}` : ''
}

const N = (id, projectId, title, kind, source, content, contactId, decisionMakerId, budget, estMM, amount, quarter, stage, priority, owner, issueId, nextAction, nextDate, scopeReason = '', memo = '') => ({
  id, projectId, title, kind, source, content, contactId, decisionMakerId, budget, estMM, amount, quarter, stage, priority, owner, issueId, nextAction, nextDate, scopeReason, memo, opsOppId: '',
})
let list = [
  N('NB-001', 'PRJ-2026-001', '프로젝트 단위 권한 모델 고도화', '고도화·개선', '수행 이슈', '조직·직급 외에 프로젝트 단위 권한 추가를 요청. 현 범위에서는 v2 모델까지만 반영하기로 합의했고, 세분화 권한과 감사 로그는 차기 사업으로 논의.', 'CTC-001', 'CTC-009', '검토 중', 12, 180000000, '2027-Q1', '검토 중', '높음', '관리자', 'ISS-005', '고객 정보화팀 내부 예산 검토 일정 확인', '2026-10-21', '요구사항 정의서 범위(권한 모델 v2)를 초과', ''),
  N('NB-002', 'PRJ-2026-001', '모바일 앱(결재·알림) 개발', '후속 단계(차기 사업)', '고객 요청', '포털 오픈 후 모바일 앱을 별도 사업으로 추진하고 싶다는 의사. 1차 범위에는 반응형 웹까지만 포함.', 'CTC-001', 'CTC-009', '미정', 18, 320000000, '2027-Q2', '발굴', '높음', '관리자', '', '킥오프 보고 때 모바일 로드맵 사전 설명', '2026-11-05', '', ''),
  N('NB-003', 'PRJ-2026-001', '계열사 3개 사 포털 확산', '타 부서·계열사 확산', '현장 발견', '그룹 IT 담당 임원이 데모를 보고 계열사 확산 가능성을 언급.', 'CTC-001', '', '미정', 30, 600000000, '2027-Q3', '발굴', '보통', '관리자', '', '그룹 IT 담당 임원 면담 요청', '2026-12-01', '', '아직 공식 요청 아님'),
  N('NB-004', 'PRJ-2026-010', '협업툴 모바일·외부 협력사 계정 확장', '인프라·증설', '고객 요청', '외부 협력사 사용자 500명 확대와 모바일 접속 요구. 라이선스·인프라 증설 포함.', 'CTC-006', 'CTC-008', '확보', 6, 90000000, '2026-Q4', '제안 준비', '높음', '관리자', '', '증설 견적서 초안 송부', '2026-10-20', '', ''),
  N('NB-005', 'PRJ-2026-010', '협업툴 1년 유지보수 계약', '운영·유지보수 전환', '영업 제안', '하자보수 종료 후 유상 유지보수로 전환 제안.', 'CTC-006', 'CTC-008', '검토 중', 6, 72000000, '2027-Q3', '발굴', '보통', '관리자', '', '유지보수 범위·SLA 제안 정리', '2026-11-15', '', ''),
]
const listeners = new Set()
const emit = (next) => {
  list = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}
export const useNextOpps = () => useSyncExternalStore(subscribe, () => list)

export const emptyNext = (projectId, owner) => ({
  id: '', projectId, title: '', kind: NEXT_KINDS[0], source: NEXT_SOURCES[0], content: '', contactId: '', decisionMakerId: '', budget: '미정', estMM: null, amount: null,
  quarter: '', stage: '발굴', priority: '보통', owner, issueId: '', nextAction: '', nextDate: '', scopeReason: '', memo: '', opsOppId: '',
})
export function saveNext(o) {
  if (o.id) return emit(list.map((x) => (x.id === o.id ? o : x)))
  const max = list.reduce((m, x) => Math.max(m, Number(x.id.split('-')[1])), 0)
  emit([{ ...o, id: `NB-${String(max + 1).padStart(3, '0')}` }, ...list])
}
export const deleteNext = (id) => emit(list.filter((o) => o.id !== id))

/**
 * 운영단계전환 시 선택한 차기 사업 기회를 운영 영업 기회로 이관한다 — 담당 고객사 담당자(요청자)·금액·예상 시기를 그대로 가져가고
 * 수행 쪽 기회는 「이관 완료」로 바꿔 운영 영업 기회 번호를 남긴다.
 */
export function transferToOps(projectId, ids) {
  const moved = list.filter((o) => o.projectId === projectId && ids.includes(o.id) && isOpenNext(o))
  const result = {}
  moved.forEach((o) => {
    result[o.id] = saveOpp({
      id: '', projectId, kind: OPS_KIND[o.kind] ?? '신규 제안', title: o.title, stage: '발굴', amount: o.amount, probability: STAGE_PROB[o.stage] ?? 10,
      owner: o.owner, expectedDate: quarterEnd(o.quarter), nextAction: o.nextAction, nextDate: o.nextDate, contactId: o.contactId,
      memo: [`수행 단계 차기 사업 기회 ${o.id}에서 이관`, o.content].filter(Boolean).join(' — '), sourceIncidentId: '', sourceNextId: o.id,
    })
  })
  emit(list.map((o) => (result[o.id] ? { ...o, stage: '이관 완료', opsOppId: result[o.id] } : o)))
  return moved.length
}
