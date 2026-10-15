/**
 * 운영단계 공통 정의 — 운영단계전환과 운영 「프로젝트 관리」가 같이 쓴다.
 */
import { MEMBERS } from './pmsStore'
import { newMember } from './teamConfig'

/** 운영 형태. */
export const OPS_TYPES = [
  { value: '하자보수', desc: '검수 후 무상 하자보수 기간 운영' },
  { value: '유상 유지보수', desc: '유지보수 계약에 따른 유상 운영' },
  { value: '하자보수 + 유지보수', desc: '무상 하자보수 후 유상 유지보수로 이어지는 운영' },
]

/** SLA 등급별 기본 목표(응답/해결 시간, 시간 단위)와 서비스 시간. */
export const SLA_LEVELS = {
  기본: { respond: 8, resolve: 72, hours: '평일 09~18시' },
  표준: { respond: 4, resolve: 24, hours: '평일 09~18시' },
  프리미엄: { respond: 1, resolve: 8, hours: '24x7' },
}

const parse = (s) => new Date(`${s}T00:00:00Z`)
const iso = (d) => d.toISOString().slice(0, 10)
export const addDaysISO = (s, n) => {
  const d = parse(s)
  d.setUTCDate(d.getUTCDate() + n)
  return iso(d)
}
/** 시작일로부터 1년(마지막 날 = 1년 뒤 전날). */
export const oneYearAfter = (s) => {
  const d = parse(s)
  d.setUTCFullYear(d.getUTCFullYear() + 1)
  d.setUTCDate(d.getUTCDate() - 1)
  return iso(d)
}

const roleMap = { 개발: '운영', QA: 'QA', '분석/설계': 'TA', 분석: 'TA', 설계: 'TA', PL: 'PL', TA: 'TA', DBA: 'DBA', 'UI/UX': 'UI/UX', PMO: 'PMO' }

/** 수행 투입 인력 → 운영 투입 인력 기본값(개발자는 운영 역할로, 설계/분석은 TA로). */
export function carryOverTeam(execTeam, period) {
  return execTeam.map((m) => ({
    ...newMember(m.name, period, ['운영']),
    role: roleMap[m.role] ?? '운영',
    phases: ['운영'],
    mm: '',
    notify: m.notify ?? '',
  }))
}

export const defaultOpsManager = (execManager) => ({
  name: execManager?.name ?? MEMBERS[0].name,
  role: execManager?.role === 'PM' ? '운영 PM' : execManager?.role ?? '운영 PM',
})
