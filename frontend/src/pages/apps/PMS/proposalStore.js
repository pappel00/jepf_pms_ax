/**
 * 제안서 관리 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 구조: 프로젝트 1 : N 제안서(RFP별) → 제안서 1 : N 버전(이력) / RFP 항목 대응표 / 제출 체크리스트.
 * RFP 항목 대응표는 RFP 관리의 항목(rfpStore)과 제안서 목차(절)를 연결해 "RFP를 빠짐없이 다뤘는지"를 점검한다.
 */
import { useSyncExternalStore } from 'react'

export const PROPOSAL_STATUSES = ['작성 중', '내부 검토', '제출 완료', '결과 확정']
/** 항목 대응 방식. */
export const RESPONSES = ['수용', '일부 수용', '대안 제시', '미수용']

export const DEFAULT_CHECKLIST = ['제안서 본문', '제안서 요약본', '가격(견적) 제안서', '사업자등록증·인증서 사본', '위임장·서약서', '발표 자료(PT)']

const itemId = (rfpId, idx) => `${rfpId}-I${String(idx).padStart(3, '0')}`
const check = (names, doneCount) => names.map((name, i) => ({ id: `C${i + 1}`, name, done: i < doneCount }))

const ECM_MAP = [
  ['3.1.1', '수용', '문서 등록·수정·삭제 및 변경 이력 관리 기능을 표준 기능으로 제공', '이설계'],
  ['3.1.2', '수용', '자동 버전 채번 및 이전 버전 복원', '이설계'],
  ['3.2.1', '수용', '폴더·문서 단위 조직/직급/개인 권한 체계 제안', '이설계'],
  ['3.3.1', '수용', '기존 통합검색과 색인 연계 인터페이스 제공', '김개발'],
  ['3.4.1', '일부 수용', '결재 첨부 이관은 배치 방식으로 제공 (실시간은 2단계)', '김개발'],
  ['3.4.2', '대안 제시', '발주사 DRM 제품 확인 후 연계, 1차는 표준 API 방식 제안', '김개발'],
  ['4.1.1', '수용', '동시접속 1,000명 기준 부하 테스트 결과 제시', '이설계'],
  ['4.1.2', '일부 수용', '500MB 이하 업로드, 분할 업로드 방식 적용', '김개발'],
  ['5.1.1', '수용', '접근/다운로드 로그 1년 이상 보관 및 조회', '이설계'],
  ['5.2.1', '수용', '검증 암호 모듈 적용', '김개발'],
]

const seed = [
  {
    id: 'PROP-2026-001', projectId: 'PRJ-2026-003', rfpId: 'RFP-2026-001', title: '전사 문서중앙화(ECM) 고도화 제안서',
    status: '작성 중', owner: '이설계', dueDate: '2026-10-21', submittedDate: '', price: 905000000, costEstimate: 740000000,
    versions: [
      { ver: 'v0.1', date: '2026-10-05', author: '이설계', summary: '목차 및 RFP 항목 대응 골격 작성', fileName: 'ECM고도화_제안서_v0.1.pptx', fileSize: '3.4MB' },
      { ver: 'v0.2', date: '2026-10-09', author: '이설계', summary: '기능 요구사항 대응 및 아키텍처 보완', fileName: 'ECM고도화_제안서_v0.2.pptx', fileSize: '5.8MB' },
    ],
    mapping: Object.fromEntries(ECM_MAP.map(([section, response, summary, owner], i) => [itemId('RFP-2026-001', i + 1), { section, response, summary, owner }])),
    checklist: check(DEFAULT_CHECKLIST, 1),
  },
  {
    id: 'PROP-2026-002', projectId: 'PRJ-2026-004', rfpId: 'RFP-2026-002', title: '모바일 앱 통합 플랫폼 구축 제안서',
    status: '제출 완료', owner: '관리자', dueDate: '2026-09-25', submittedDate: '2026-09-24', price: 2380000000, costEstimate: 1980000000,
    versions: [
      { ver: 'v0.5', date: '2026-09-15', author: '관리자', summary: '내부 검토 반영본', fileName: '모바일플랫폼_제안서_v0.5.pptx', fileSize: '7.1MB' },
      { ver: 'v1.0', date: '2026-09-24', author: '관리자', summary: '최종 제출본', fileName: '모바일플랫폼_제안서_v1.0.pdf', fileSize: '9.6MB' },
    ],
    mapping: Object.fromEntries(
      [
        ['2.1', '수용', 'SSO 연계 표준 모듈 적용'],
        ['2.2', '수용', 'FCM/APNs 기반 푸시 및 수신 이력 관리'],
        ['2.3', '일부 수용', '생체 인증은 지원 단말 한정'],
        ['2.4', '수용', '관리자 앱 배포·버전 정책 화면 제공'],
        ['3.1', '수용', '구동 시간 2초 이내 성능 설계'],
        ['4.1', '수용', '위·변조 방지 솔루션 적용'],
        ['4.2', '수용', 'OS 호환성 테스트 매트릭스 제시'],
        ['5.1', '일부 수용', '망 분리 환경 구성안 제시'],
        ['6.1', '수용', '단계별 산출물 목록 제시'],
      ].map(([section, response, summary], i) => [itemId('RFP-2026-002', i + 1), { section, response, summary, owner: '관리자' }]),
    ),
    checklist: check(DEFAULT_CHECKLIST, 6),
  },
]

let proposals = seed
const listeners = new Set()

function emit(next) {
  proposals = next
  listeners.forEach((l) => l())
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function useProposals() {
  return useSyncExternalStore(subscribe, () => proposals)
}

export function nextProposalId(all) {
  const year = new Date().getFullYear()
  const max = all
    .map((p) => p.id.match(/^PROP-(\d{4})-(\d+)$/))
    .filter((m) => m && Number(m[1]) === year)
    .reduce((s, m) => Math.max(s, Number(m[2])), 0)
  return `PROP-${year}-${String(max + 1).padStart(3, '0')}`
}

/** 다음 버전 번호: 마지막이 v1.0 이전이면 0.1씩, 이후면 소수점 한 자리씩 올린다. */
export function nextVersion(versions) {
  const last = versions[versions.length - 1]?.ver ?? 'v0.0'
  const n = Math.round(parseFloat(last.slice(1)) * 10) + 1
  return `v${Math.floor(n / 10)}.${n % 10}`
}

const patch = (id, fn) => emit(proposals.map((p) => (p.id === id ? fn(p) : p)))

export function saveProposal(p) {
  const exists = proposals.some((x) => x.id === p.id)
  emit(exists ? proposals.map((x) => (x.id === p.id ? p : x)) : [...proposals, p])
}

export function deleteProposal(id) {
  emit(proposals.filter((p) => p.id !== id))
}

export function addVersion(id, version) {
  patch(id, (p) => ({ ...p, versions: [...p.versions, version] }))
}

export function setMapping(id, itemIdValue, value) {
  patch(id, (p) => {
    const mapping = { ...p.mapping }
    if (value === null) delete mapping[itemIdValue]
    else mapping[itemIdValue] = { section: '', response: '', summary: '', owner: '', ...mapping[itemIdValue], ...value }
    return { ...p, mapping }
  })
}

export function toggleCheck(id, checkId) {
  patch(id, (p) => ({ ...p, checklist: p.checklist.map((c) => (c.id === checkId ? { ...c, done: !c.done } : c)) }))
}

export function addCheck(id, name) {
  patch(id, (p) => ({ ...p, checklist: [...p.checklist, { id: `C${Date.now()}`, name, done: false }] }))
}

export function removeCheck(id, checkId) {
  patch(id, (p) => ({ ...p, checklist: p.checklist.filter((c) => c.id !== checkId) }))
}
