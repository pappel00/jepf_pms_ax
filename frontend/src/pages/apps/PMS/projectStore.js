/**
 * PMS 프로젝트(사업) 관리 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 * 영업 담당자만 실제 사용자 관리(/api/v1/users) 연동을 목업으로 보여준다(ProjectsPage 참고).
 */
import { useSyncExternalStore } from 'react'

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

export const BUSINESS_TYPES = ['SI 구축', 'SM 운영/유지보수', '컨설팅', '솔루션 공급', '기타']
export const BID_TYPES = ['공개 경쟁입찰', '제한 경쟁입찰', '협상에 의한 계약', '수의계약', '내부/그룹사']

const seed = [
  {
    id: 'PRJ-2026-001', name: '통합업무포탈 구축사업', client: '(샘플) 고객사',
    start: '2026-09-01', end: '2027-03-31', amount: 3200000000, status: '수행 중', probability: 100,
    contactName: '한고객', contactTitle: '부장', contactPhone: '010-1234-5678', contactEmail: 'client@example.com',
    salesId: null, salesName: '관리자',
    businessType: 'SI 구축', bidType: '협상에 의한 계약', noticeNo: '2026-0412', dueDate: '2026-07-10', presentDate: '2026-07-24',
    competitors: '○○시스템, △△정보', pmName: '관리자', costEstimate: 2650000000, failReason: '', memo: 'WBS 목업의 기준 프로젝트',
    // 이미 수행 중인 기준 프로젝트의 수행 관리 정보(단계전환 시 지정한 프로젝트 관리자·투입 인력과 같은 형태)
    transitioned: {
      date: '2026-09-01', rfpId: '', itemCount: 0, phases: ['분석', '설계', '개발', '테스트'],
      manager: { name: '관리자', role: 'PM' },
      team: [
        { name: '이설계', role: '분석/설계', phases: ['분석', '설계'], mm: 4, from: '2026-09-01', to: '2026-12-31', notify: '' },
        { name: '김개발', role: '개발', phases: ['개발', '테스트'], mm: 6, from: '2026-10-01', to: '2027-03-31', notify: '' },
        { name: '박협력', role: '개발', phases: ['개발'], mm: 4.5, from: '2026-10-06', to: '2027-02-26', notify: 'Mattermost' },
        { name: '정QA', role: 'QA', phases: ['테스트'], mm: 3, from: '2026-11-02', to: '2027-02-26', notify: '' },
      ],
    },
  },
  {
    id: 'PRJ-2026-002', name: '차세대 인사시스템 구축', client: '한빛전자',
    start: '2027-01-04', end: '2027-09-30', amount: 1850000000, status: '공고 중', probability: 20,
    contactName: '', contactTitle: '', contactPhone: '', contactEmail: '',
    salesId: null, salesName: '김영업',
    businessType: 'SI 구축', bidType: '공개 경쟁입찰', noticeNo: '2026-1103', dueDate: '2026-10-30', presentDate: '',
    competitors: '', pmName: '', costEstimate: null, failReason: '', memo: 'RFP 검토 중',
  },
  {
    id: 'PRJ-2026-003', name: '전사 문서중앙화(ECM) 고도화', client: '대한물산',
    start: '2026-12-01', end: '2027-05-31', amount: 920000000, status: '제안 중', probability: 30,
    contactName: '박담당', contactTitle: '과장', contactPhone: '02-555-0101', contactEmail: '',
    salesId: null, salesName: '최영업',
    businessType: 'SI 구축', bidType: '제한 경쟁입찰', noticeNo: '2026-0988', dueDate: '2026-10-21', presentDate: '2026-11-03',
    competitors: '□□소프트', pmName: '관리자', costEstimate: 740000000, failReason: '', memo: '',
  },
  {
    id: 'PRJ-2026-004', name: '모바일 앱 통합 플랫폼', client: '새솔금융',
    start: '2026-12-15', end: '2027-08-31', amount: 2400000000, status: '수주', probability: 100,
    contactName: '이팀장', contactTitle: '팀장', contactPhone: '010-9876-5432', contactEmail: 'lee@saesol.example',
    salesId: null, salesName: '관리자',
    businessType: 'SI 구축', bidType: '협상에 의한 계약', noticeNo: '2026-0871', dueDate: '2026-09-25', presentDate: '2026-10-16',
    competitors: '◇◇정보통신, ○○시스템', pmName: '이설계', costEstimate: 1980000000, failReason: '', memo: '발표 PT 준비',
  },
  {
    id: 'PRJ-2026-005', name: 'ERP 연계 인터페이스 개선', client: '푸른물류',
    start: '2026-11-16', end: '2027-02-26', amount: 560000000, status: '협상 중', probability: 80,
    contactName: '정구매', contactTitle: '대리', contactPhone: '031-777-2020', contactEmail: '',
    salesId: null, salesName: '김영업',
    businessType: 'SI 구축', bidType: '수의계약', noticeNo: '', dueDate: '', presentDate: '',
    competitors: '', pmName: '김개발', costEstimate: 430000000, failReason: '', memo: '단가 협상 중',
  },
  {
    id: 'PRJ-2026-006', name: '데이터 거버넌스 컨설팅', client: '미래제약',
    start: '2027-02-01', end: '2027-06-30', amount: 300000000, status: '공고 전', probability: 10,
    contactName: '', contactTitle: '', contactPhone: '', contactEmail: '',
    salesId: null, salesName: '최영업',
    businessType: '컨설팅', bidType: '', noticeNo: '', dueDate: '', presentDate: '',
    competitors: '', pmName: '', costEstimate: null, failReason: '', memo: '내년 상반기 발주 예정 (사전 미팅)',
  },
  {
    id: 'PRJ-2026-007', name: '포털 유지보수 (2027)', client: '대한물산',
    start: '2027-01-01', end: '2027-12-31', amount: 480000000, status: '수주', probability: 100,
    contactName: '박담당', contactTitle: '과장', contactPhone: '02-555-0101', contactEmail: '',
    salesId: null, salesName: '관리자',
    businessType: 'SM 운영/유지보수', bidType: '수의계약', noticeNo: '', dueDate: '', presentDate: '',
    competitors: '', pmName: '김개발', costEstimate: 390000000, failReason: '', memo: '계약서 날인 진행',
  },
  {
    // 인력 투입 겹침(과투입)·가용 인력 조회를 보여주기 위한 또 하나의 수행 중 프로젝트(이설계가 관리자라 내 계정으로는 읽기 전용)
    id: 'PRJ-2026-009', name: '그룹사 인트라넷 운영', client: '대한물산',
    start: '2026-09-01', end: '2026-12-31', amount: 360000000, status: '수행 중', probability: 100,
    contactName: '송운영', contactTitle: '대리', contactPhone: '02-555-0202', contactEmail: '',
    salesId: null, salesName: '최영업',
    businessType: 'SM 운영/유지보수', bidType: '수의계약', noticeNo: '', dueDate: '', presentDate: '',
    competitors: '', pmName: '', costEstimate: 290000000, failReason: '', memo: '2026년 하반기 운영 계약',
    transitioned: {
      date: '2026-08-25', rfpId: '', itemCount: 0, phases: ['개발', '테스트'],
      manager: { name: '이설계', role: 'PL' },
      team: [
        { name: '김개발', role: '개발', phases: ['개발', '테스트'], mm: 2, from: '2026-09-01', to: '2026-12-31', notify: '' },
        { name: '정QA', role: 'QA', phases: ['테스트'], mm: 1.5, from: '2026-10-01', to: '2026-12-31', notify: '' },
      ],
    },
  },
  {
    // 수행 종료(준공/검수 완료) 상태 — 운영단계전환 대상 시연용
    id: 'PRJ-2026-010', name: '사내 협업툴 구축', client: '한빛전자',
    start: '2026-03-02', end: '2026-08-28', amount: 780000000, status: '수행 종료', probability: 100,
    contactName: '유협업', contactTitle: '과장', contactPhone: '031-888-0101', contactEmail: '',
    salesId: null, salesName: '관리자',
    businessType: 'SI 구축', bidType: '협상에 의한 계약', noticeNo: '2026-0210', dueDate: '2026-01-16', presentDate: '2026-01-30',
    competitors: '', pmName: '관리자', costEstimate: 620000000, failReason: '', memo: '2026-08-28 검수 완료',
    transitioned: {
      date: '2026-02-23', rfpId: '', itemCount: 0, phases: ['분석', '설계', '개발', '테스트'],
      manager: { name: '관리자', role: 'PM' },
      team: [
        { name: '이설계', role: '분석/설계', phases: ['분석', '설계'], mm: 3, from: '2026-03-02', to: '2026-05-29', notify: '' },
        { name: '김개발', role: '개발', phases: ['개발', '테스트'], mm: 4, from: '2026-04-01', to: '2026-08-28', notify: '' },
        { name: '박협력', role: '개발', phases: ['개발'], mm: 3, from: '2026-04-01', to: '2026-07-31', notify: 'Mattermost' },
        { name: '정QA', role: 'QA', phases: ['테스트'], mm: 1.5, from: '2026-07-01', to: '2026-08-28', notify: '' },
      ],
    },
    completed: {
      date: '2026-08-28', memo: '최종 검수 완료, 미해결 이슈 없음',
      features: ['협업 워크스페이스', '프로젝트·태스크 관리', '주간 보고서', '알림·메신저 연동', '통합 검색'],
      openIssues: 0,
    },
  },
  {
    // 운영 중 프로젝트 — 운영 업무·운영 이슈·운영 영업 목업 시연용(운영단계전환을 마친 상태)
    id: 'PRJ-2026-011', name: '그룹웨어 유지보수 (2026 하반기)', client: '대한물산',
    start: '2026-01-05', end: '2026-06-30', amount: 150000000, status: '운영 중', probability: 100,
    contactName: '박담당', contactTitle: '과장', contactPhone: '02-555-0101', contactEmail: '',
    salesId: null, salesName: '관리자',
    businessType: 'SM 운영/유지보수', bidType: '수의계약', noticeNo: '', dueDate: '', presentDate: '',
    competitors: '', pmName: '관리자', costEstimate: 110000000, failReason: '', memo: '2026-06-30 검수 완료 후 유상 유지보수 전환',
    transitioned: {
      date: '2026-01-02', rfpId: '', itemCount: 0, phases: ['개발', '테스트'],
      manager: { name: '관리자', role: 'PM' },
      team: [{ name: '관리자', role: 'PL', phases: ['개발', '테스트'], mm: 3, from: '2026-01-05', to: '2026-06-30', notify: '' }],
    },
    completed: { date: '2026-06-30', memo: '검수 완료', features: ['전자결재', '통합검색', '모바일 결재'], openIssues: 0 },
    operation: {
      date: '2026-07-01', type: '유상 유지보수', startDate: '2026-07-01', endDate: '2026-12-31', contractAmount: 150000000,
      sla: { level: '표준', respond: 4, resolve: 24, hours: '평일 09~18시' },
      manager: { name: '관리자', role: '운영 PM' },
      team: [{ name: '관리자', role: '운영', phases: ['운영'], mm: 3, from: '2026-07-01', to: '2026-12-31', notify: '' }],
      features: ['전자결재', '통합검색', '모바일 결재'], docs: [], carriedIssues: [],
    },
  },
  {
    id: 'PRJ-2026-008', name: '스마트팩토리 MES 구축', client: '동서에너지',
    start: '2026-10-01', end: '2027-06-30', amount: 4100000000, status: '실패', probability: 0,
    contactName: '오책임', contactTitle: '책임', contactPhone: '052-300-1100', contactEmail: '',
    salesId: null, salesName: '김영업',
    businessType: 'SI 구축', bidType: '공개 경쟁입찰', noticeNo: '2026-0655', dueDate: '2026-08-14', presentDate: '2026-08-28',
    competitors: '◇◇정보통신', pmName: '', costEstimate: null, failReason: '가격 경쟁력 열세 (경쟁사 대비 약 12% 높음)', memo: '',
  },
]

let projects = seed
const listeners = new Set()

function emit(next) {
  projects = next
  listeners.forEach((l) => l())
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function useProjects() {
  return useSyncExternalStore(subscribe, () => projects)
}

/** 프로젝트 코드 자동 채번: PRJ-{연도}-{3자리 일련번호}. */
export function nextProjectId(all) {
  const year = new Date().getFullYear()
  const max = all
    .map((p) => p.id.match(/^PRJ-(\d{4})-(\d+)$/))
    .filter((m) => m && Number(m[1]) === year)
    .reduce((s, m) => Math.max(s, Number(m[2])), 0)
  return `PRJ-${year}-${String(max + 1).padStart(3, '0')}`
}

export function saveProject(project) {
  const exists = projects.some((p) => p.id === project.id)
  emit(exists ? projects.map((p) => (p.id === project.id ? project : p)) : [...projects, project])
}

/** 단계전환 — 수주 프로젝트를 수행 중으로 바꾸고 수행 PM·전환 이력을 남긴다. */
export function transitionProject(id, { manager, team, date, rfpId, itemCount, phases }) {
  emit(
    projects.map((p) =>
      p.id === id
        ? {
            ...p,
            status: '수행 중',
            // 프로젝트 관리자의 프로젝트 내 역할이 PM이면 수행 PM으로도 기록한다(PL/PMO 관리자면 PM은 비움).
            pmName: manager.role === 'PM' ? manager.name : '',
            transitioned: { date, rfpId: rfpId ?? '', itemCount, phases, manager, team },
          }
        : p,
    ),
  )
}

/**
 * 수행 단계 「프로젝트 관리」 저장 — 프로젝트 관리자와 투입 인력(프로젝트 멤버)을 수정한다.
 * 영업단계 기본 정보는 수정하지 않는다.
 */
export function updateExecution(id, { manager, team }) {
  emit(
    projects.map((p) =>
      p.id === id
        ? { ...p, pmName: manager.role === 'PM' ? manager.name : '', transitioned: { ...p.transitioned, manager, team } }
        : p,
    ),
  )
}

/** 수행 종료 처리 — 프로젝트 관리자가 준공/검수 완료를 확정한다. 운영단계 인수 대상 기능 목록을 함께 남긴다. */
export function endExecution(id, { date, memo, features, openIssues }) {
  emit(projects.map((p) => (p.id === id ? { ...p, status: '수행 종료', end: p.end, completed: { date, memo, features, openIssues } } : p)))
}

/**
 * 운영단계전환 — 수행 종료 프로젝트를 「운영 중」으로 바꾸고 운영 기초데이터를 남긴다.
 * operation: { date, type, startDate, endDate, sla, manager, team, features, docs, carriedIssueIds }
 */
export function transitionToOperation(id, operation) {
  emit(projects.map((p) => (p.id === id ? { ...p, status: '운영 중', operation } : p)))
}

/** 운영 정보 수정(운영 관리자). */
export function updateOperation(id, patch) {
  emit(projects.map((p) => (p.id === id ? { ...p, operation: { ...p.operation, ...patch } } : p)))
}

export function deleteProject(id) {
  emit(projects.filter((p) => p.id !== id))
}

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
