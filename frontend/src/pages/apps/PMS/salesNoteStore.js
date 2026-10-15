/**
 * 영업 노트(영업 활동 기록) 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 영업 담당자가 사전영업·제안 과정에서 한 활동(방문·미팅·통화 등)과 고객 반응, 다음 할 일을 남기는 기록.
 * 담당자가 바뀌어도 프로젝트/고객사 단위로 히스토리를 이어서 볼 수 있게 하는 것이 목적이다.
 */
import { useSyncExternalStore } from 'react'

export const NOTE_TYPES = ['방문', '미팅', '전화', '이메일', '식사·접대', '제안·발표', '자료 전달', '정보 수집', '인수인계', '기타']

const N = (id, date, type, author, projectId, customerId, contactId, title, content, nextAction = '', nextDate = '', done = false) => ({
  id, date, type, author, projectId, customerId, contactId, title, content, nextAction, nextDate, done,
})

let notes = [
  N('SN-001', '2026-08-11', '방문', '관리자', 'PRJ-2026-002', 'CUS-002', '', '인사시스템 노후화 관련 첫 방문', '현 인사시스템 8년차, 유지보수 종료 예정이라 내년 상반기 차세대 전환 검토 중. 예산은 연말 확정.', '예산 확정 시점 재확인', '2026-10-20'),
  N('SN-002', '2026-09-03', '전화', '관리자', 'PRJ-2026-002', 'CUS-002', '', '예산 진행 상황 확인', '내부 보고 중이며 11월 중 공고 예정이라고 함. 경쟁사 ○○가 이미 접촉했다는 후문.', '공고 전 사전 제안 미팅 요청', '2026-10-25'),
  N('SN-003', '2026-09-18', '미팅', '이설계', 'PRJ-2026-004', 'CUS-004', 'CTC-003', '모바일 앱 통합 요구사항 청취', '이팀장 외 2명 참석. 앱 3종 통합과 SSO 요구. 보안 심의 일정이 길어 일정 여유 필요.', '요구사항 정리본 송부', '2026-10-16'),
  N('SN-004', '2026-09-30', '식사·접대', '이설계', 'PRJ-2026-004', 'CUS-004', 'CTC-003', '팀장님 식사 자리', '비공식 자리. 가격보다 운영 안정성을 중시. 기존 업체 불만: 장애 대응 지연.', '', ''),
  N('SN-005', '2026-10-07', '자료 전달', '김개발', 'PRJ-2026-005', 'CUS-005', 'CTC-004', 'ERP 연계 사례 자료 전달', '유사 물류 고객 구축 사례 소개 자료 이메일 발송. 담당자 반응 긍정적, 구매팀장 보고 예정.', '구매팀장 미팅 일정 잡기', '2026-10-12'),
  N('SN-006', '2026-10-10', '정보 수집', '관리자', '', 'CUS-006', '', '미래제약 데이터 거버넌스 니즈', '전시회에서 담당 임원 명함 교환. 데이터 품질 이슈로 컨설팅 의향 있음 (프로젝트 등록 전).', '담당자 정보 확인 후 고객사 담당자 등록', '2026-10-18'),
]
const listeners = new Set()
const emit = (next) => {
  notes = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}
export const useSalesNotes = () => useSyncExternalStore(subscribe, () => notes)

export const emptyNote = (author, today) => ({
  id: '', date: today, type: '미팅', author, projectId: '', customerId: '', contactId: '', title: '', content: '', nextAction: '', nextDate: '', done: false,
})

export function saveNote(n) {
  if (n.id) return emit(notes.map((x) => (x.id === n.id ? n : x)))
  const max = notes.reduce((m, x) => Math.max(m, Number(x.id.split('-')[1])), 0)
  emit([{ ...n, id: `SN-${String(max + 1).padStart(3, '0')}` }, ...notes])
}
export const deleteNote = (id) => emit(notes.filter((n) => n.id !== id))
export const toggleDone = (id) => emit(notes.map((n) => (n.id === id ? { ...n, done: !n.done } : n)))
