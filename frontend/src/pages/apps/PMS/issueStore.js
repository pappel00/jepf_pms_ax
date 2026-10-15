/**
 * 이슈 관리 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 요구사항 문서 3.9.3: 이슈는 Project 하위 공통 엔티티이고, WBS 업무(taskId)는 선택 연결(nullable)이다.
 * 양방향 관리 — 이슈 화면에서 연결 업무를 지정/이동하고, WBS 업무에서 연결된 이슈를 조회·등록·연결한다.
 */
import { useSyncExternalStore } from 'react'

export const ISSUE_TYPES = ['일정', '범위/요구사항', '품질', '인력/자원', '기술', '고객/협의', '기타']
export const PRIORITIES = ['긴급', '높음', '보통', '낮음']
export const ISSUE_STATUSES = ['오픈', '진행 중', '해결', '종결']
export const isOpenStatus = (s) => s === '오픈' || s === '진행 중'
export const isHighPriority = (p) => p === '긴급' || p === '높음'

const h = (date, author, text) => ({ date, author, text })

const seed = [
  {
    id: 'ISS-001', projectId: 'PRJ-2026-001', title: '게시판 분석 지연 — 발주사 요구사항 회신 지연', type: '고객/협의', priority: '높음', status: '진행 중',
    reporter: '관리자', assignee: '관리자', registeredDate: '2026-09-24', dueDate: '2026-10-16', resolvedDate: '',
    taskId: 'REQ-002-AN', impactDays: 5,
    content: '발주사 현업 회신이 예정보다 늦어 게시판 분석 업무 완료가 지연되고 있다. 후속 설계·개발·테스트 일정에 영향.',
    resolution: '', history: [h('2026-09-24', '관리자', '이슈 등록'), h('2026-10-05', '관리자', '발주사 담당자에게 회신 독촉 — 10/16까지 회신 약속'), h('2026-10-08', '관리자', '상태 변경: 오픈 → 진행 중')],
  },
  {
    id: 'ISS-002', projectId: 'PRJ-2026-001', title: '전자결재 연동 방식 결정 필요 (기존 결재 시스템 API 제약)', type: '기술', priority: '긴급', status: '오픈',
    reporter: '김개발', assignee: '김개발', registeredDate: '2026-10-12', dueDate: '2026-10-15', resolvedDate: '',
    taskId: 'REQ-001-DV', impactDays: 3,
    content: '기존 결재 시스템이 실시간 API를 제공하지 않아 배치 연동으로 갈지 별도 어댑터를 둘지 결정이 필요하다.',
    resolution: '', history: [h('2026-10-12', '김개발', '이슈 등록')],
  },
  {
    id: 'ISS-003', projectId: 'PRJ-2026-001', title: 'SSO 연계 테스트 계정 미발급', type: '기술', priority: '높음', status: '진행 중',
    reporter: '김개발', assignee: '김개발', registeredDate: '2026-10-08', dueDate: '2026-10-14', resolvedDate: '',
    taskId: 'REQ-008-DV', impactDays: 2,
    content: '발주사 SSO 서버의 연계 테스트 계정이 발급되지 않아 연계 개발 검증이 막혀 있다.',
    resolution: '', history: [h('2026-10-08', '김개발', '이슈 등록'), h('2026-10-10', '관리자', '발주사 보안 담당에게 발급 요청 메일 발송')],
  },
  {
    id: 'ISS-004', projectId: 'PRJ-2026-001', title: '협력사 투입 인력 일정 조율', type: '인력/자원', priority: '보통', status: '오픈',
    reporter: '관리자', assignee: '관리자', registeredDate: '2026-10-09', dueDate: '2026-10-20', resolvedDate: '',
    taskId: 'REQ-005-DV', impactDays: 0,
    content: '협력사 개발 인력 1명이 10월 말까지 타 프로젝트와 병행 투입 예정 — 개발 일정 영향 검토 필요.',
    resolution: '', history: [h('2026-10-09', '관리자', '이슈 등록')],
  },
  {
    id: 'ISS-005', projectId: 'PRJ-2026-001', title: '문서 중앙화 권한 모델 변경 요청', type: '범위/요구사항', priority: '보통', status: '해결',
    reporter: '이설계', assignee: '이설계', registeredDate: '2026-10-01', dueDate: '2026-10-12', resolvedDate: '2026-10-12',
    taskId: 'REQ-007-DS', impactDays: 1,
    content: '발주사가 조직·직급 외에 프로젝트 단위 권한을 추가로 요청했다.',
    resolution: '권한 모델 v2에 프로젝트 단위 권한을 반영하기로 발주사와 합의(10/12 회의).',
    history: [h('2026-10-01', '이설계', '이슈 등록'), h('2026-10-12', '이설계', '상태 변경: 진행 중 → 해결')],
  },
  {
    id: 'ISS-009', projectId: 'PRJ-2026-001', title: '조직도 연동 데이터 불일치 오류', type: '품질', priority: '높음', status: '해결',
    reporter: '김개발', assignee: '김개발', registeredDate: '2026-10-08', dueDate: '2026-10-14', resolvedDate: '2026-10-13',
    taskId: 'REQ-004-DV', impactDays: 1,
    content: '인사 시스템에서 내려받은 조직도와 그룹웨어 조직도의 부서 코드가 일부 달라 연동 시 누락이 발생했다.',
    resolution: '부서 코드 매핑 테이블을 추가하고 연동 배치를 재실행해 정합성 확인 완료.',
    history: [h('2026-10-08', '김개발', '이슈 등록'), h('2026-10-13', '김개발', '상태 변경: 진행 중 → 해결')],
  },
  {
    id: 'ISS-006', projectId: 'PRJ-2026-001', title: '화면 설계서 검토 의견 반영 지연', type: '일정', priority: '낮음', status: '종결',
    reporter: '관리자', assignee: '이설계', registeredDate: '2026-09-28', dueDate: '2026-10-05', resolvedDate: '2026-10-02',
    taskId: 'RV-DS', impactDays: 0,
    content: '설계 단계말 검토 전 화면 설계서 의견 반영이 지연되었다.',
    resolution: '의견 반영 완료 후 재검토, 일정 영향 없음.', history: [h('2026-09-28', '관리자', '이슈 등록'), h('2026-10-02', '이설계', '상태 변경: 진행 중 → 해결'), h('2026-10-05', '관리자', '상태 변경: 해결 → 종결')],
  },
  {
    id: 'ISS-007', projectId: 'PRJ-2026-001', title: '개발 서버 증설 지연', type: '기타', priority: '높음', status: '오픈',
    reporter: '김개발', assignee: '김개발', registeredDate: '2026-10-05', dueDate: '2026-10-12', resolvedDate: '',
    taskId: null, impactDays: 0,
    content: '발주사 인프라 팀의 개발 서버 증설이 지연되어 병렬 개발 환경이 부족하다. (특정 업무에 한정되지 않는 이슈)',
    resolution: '', history: [h('2026-10-05', '김개발', '이슈 등록')],
  },
  {
    id: 'ISS-008', projectId: 'PRJ-2026-001', title: '발주사 담당자 변경에 따른 보고 체계 재정의', type: '고객/협의', priority: '보통', status: '진행 중',
    reporter: '관리자', assignee: '관리자', registeredDate: '2026-10-07', dueDate: '2026-10-23', resolvedDate: '',
    taskId: null, impactDays: 0,
    content: '발주사 PM이 교체되어 주간·월간 보고 체계와 승인 절차를 다시 합의해야 한다.',
    resolution: '', history: [h('2026-10-07', '관리자', '이슈 등록')],
  },
  {
    id: 'ISS-010', projectId: 'PRJ-2026-009', title: '운영 서버 정기 패치 일정 협의', type: '일정', priority: '보통', status: '진행 중',
    reporter: '김개발', assignee: '김개발', registeredDate: '2026-10-08', dueDate: '2026-10-21', resolvedDate: '',
    taskId: null, impactDays: 0,
    content: '발주사 정기 점검 일정과 패치 일정이 겹쳐 작업 시간대 조정이 필요하다.',
    resolution: '', history: [h('2026-10-08', '김개발', '이슈 등록')],
  },
]

let issues = seed
const listeners = new Set()

function emit(next) {
  issues = next
  listeners.forEach((l) => l())
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function useIssues() {
  return useSyncExternalStore(subscribe, () => issues)
}

export function nextIssueId(all) {
  const max = all.reduce((s, i) => Math.max(s, Number(i.id.split('-')[1]) || 0), 0)
  return `ISS-${String(max + 1).padStart(3, '0')}`
}

export const issuesOfTask = (all, taskId) => all.filter((i) => i.taskId === taskId)

/** 업무(들)에 연결된 이슈 집계: 전체/미해결/미해결 중 긴급·높음. */
export function issueStats(all, taskIds) {
  const set = new Set(taskIds)
  const list = all.filter((i) => i.taskId && set.has(i.taskId))
  const open = list.filter((i) => isOpenStatus(i.status))
  return { total: list.length, open: open.length, high: open.filter((i) => isHighPriority(i.priority)).length }
}

/** 저장 — 신규면 등록 이력을, 상태가 바뀌면 상태 변경 이력을 자동으로 남긴다. */
export function saveIssue(issue, actor, today) {
  const prev = issues.find((i) => i.id === issue.id)
  let next = { ...issue }
  const history = [...(next.history ?? [])]
  if (!prev) {
    history.push({ date: today, author: actor, text: '이슈 등록' })
  } else if (prev.status !== next.status) {
    history.push({ date: today, author: actor, text: `상태 변경: ${prev.status} → ${next.status}` })
  }
  if (!isOpenStatus(next.status) && !next.resolvedDate) next.resolvedDate = today
  if (isOpenStatus(next.status)) next.resolvedDate = ''
  next = { ...next, history }
  emit(prev ? issues.map((i) => (i.id === next.id ? next : i)) : [...issues, next])
}

export function deleteIssue(id) {
  emit(issues.filter((i) => i.id !== id))
}

export function linkIssue(id, taskId, actor, today, taskName) {
  emit(
    issues.map((i) =>
      i.id === id
        ? {
            ...i,
            taskId,
            history: [...i.history, { date: today, author: actor, text: taskId ? `업무 연결: ${taskName}` : '업무 연결 해제' }],
          }
        : i,
    ),
  )
}

export function addHistory(id, text, author, date) {
  emit(issues.map((i) => (i.id === id ? { ...i, history: [...i.history, { date, author, text }] } : i)))
}
