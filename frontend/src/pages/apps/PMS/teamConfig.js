/**
 * 프로젝트 관리자 / 투입 인력 설정 공통 정의 — 단계전환과 수행 단계 「프로젝트 관리」가 같이 쓴다.
 * 투입 인력은 이후 프로젝트 멤버 관리·인력관리(투입관리)의 기준 데이터가 된다.
 */
import { CHAIN, MEMBERS } from './pmsStore'

/** 프로젝트 관리자의 프로젝트 내 역할 — 수행 단계 기본 정보(프로젝트 관리, 문서 테일러링, WBS)를 관리한다. */
export const MANAGER_ROLES = ['PM', 'PL', 'PMO']
/** 투입 인력의 프로젝트 내 역할. */
export const TEAM_ROLES = ['PL', '분석', '설계', '분석/설계', '개발', 'DBA', 'UI/UX', 'QA', 'TA', 'PMO', '기타']
/** 운영 단계 투입 인력의 역할. */
export const OPS_ROLES = ['운영', '장애대응', '기술지원', 'DBA', 'UI/UX', 'QA', 'TA', 'PL', 'PMO', '기타']
export const OPS_MANAGER_ROLES = ['운영 PM', 'PL', 'PMO']
export const NOTIFY_CHANNELS = ['Mattermost', '텔레그램', '이메일', '미설정']

/** 단계별 역할 우선순위 — 업무 배정 후보를 이 순서로 정렬한다(그 단계의 주 역할, 특히 개발자가 위). */
export const ROLE_PRIORITY = {
  분석: ['분석', '분석/설계', 'PL', 'TA', '기타', '설계', 'DBA', 'UI/UX', '개발', 'QA', 'PMO'],
  설계: ['설계', '분석/설계', 'TA', 'DBA', 'UI/UX', 'PL', '기타', '분석', '개발', 'QA', 'PMO'],
  개발: ['개발', 'DBA', 'UI/UX', 'TA', 'PL', '기타', '분석/설계', '설계', '분석', 'QA', 'PMO'],
  테스트: ['QA', 'PL', '기타', 'TA', '개발', 'DBA', 'UI/UX', '분석/설계', '설계', '분석', 'PMO'],
}

export const ASSIGN_MODES = [
  { value: 'auto', label: '투입 인력 전체에 분배 (권장)', desc: '그 단계에 투입된 모든 인력에게 투입 M/M 비율로 나눠 배정합니다. 목록은 그 단계의 주 역할(개발 단계는 개발자)이 위에 옵니다.' },
  { value: 'lead', label: '단계 대표 1명', desc: '그 단계 목록의 첫 번째 인력 1명에게 모두 배정합니다.' },
  { value: 'none', label: '미배정', desc: '업무를 미배정으로 만들고 WBS에서 직접 지정합니다.' },
]

export const isPartner = (name) => (MEMBERS.find((m) => m.name === name)?.org ?? '').startsWith('협력사')
export const orgOf = (name) => (isPartner(name) ? '협력사' : '자사')

const parse = (s) => new Date(`${s}T00:00:00Z`)
/** 기간(FROM~TO)을 월 단위로 환산(0.5 단위) — 투입률 100% 기준 M/M 제안값. */
export function monthsBetween(from, to) {
  if (!from || !to || to < from) return 0
  const days = (parse(to) - parse(from)) / 86400000 + 1
  return Math.round((days / 30.4) * 2) / 2
}

export const defaultManager = () => ({ name: MEMBERS.find((m) => m.role === 'PM')?.name ?? MEMBERS[0].name, role: 'PM' })

/** 신규 투입 인력 한 줄 기본값. */
export function newMember(name, period, phaseList = CHAIN) {
  return {
    name,
    role: '개발',
    phases: [...phaseList],
    mm: '',
    from: period?.start ?? '',
    to: period?.end ?? '',
    notify: name && isPartner(name) ? 'Mattermost' : '',
  }
}

export const defaultTeam = (period) =>
  [
    { name: '이설계', role: '분석/설계', phases: ['분석', '설계'] },
    { name: '김개발', role: '개발', phases: ['개발'] },
    { name: '박협력', role: '개발', phases: ['개발'] },
    { name: '정QA', role: 'QA', phases: ['테스트'] },
  ].map((m) => ({ ...newMember(m.name, period), ...m, notify: isPartner(m.name) ? 'Mattermost' : '' }))

/**
 * 투입 인력 → 단계별 업무 담당 후보(순수 함수).
 *  - 그 단계에 투입된 인력 전체, 역할 우선순위 순(개발 단계는 개발자가 위). PMO는 다른 인력이 있으면 제외.
 *  - 후보가 없으면 프로젝트 관리자를 후보로 둔다(관리자에게 배정).
 *  - weight = 투입 M/M(없으면 1).
 */
export function buildPools(team, phases, manager) {
  const pools = {}
  phases.forEach((ph) => {
    let pool = team.filter((m) => m.name && m.phases.includes(ph))
    const nonPmo = pool.filter((m) => m.role !== 'PMO')
    if (nonPmo.length > 0) pool = nonPmo
    const rank = (m) => {
      const i = ROLE_PRIORITY[ph].indexOf(m.role)
      return i < 0 ? 99 : i
    }
    pool = [...pool].sort((a, b) => rank(a) - rank(b))
    pools[ph] = pool.length
      ? pool.map((m) => ({ name: m.name, weight: Number(m.mm) > 0 ? Number(m.mm) : 1, role: m.role }))
      : manager?.name
        ? [{ name: manager.name, weight: 1, role: manager.role, fallback: true }]
        : []
  })
  return pools
}
