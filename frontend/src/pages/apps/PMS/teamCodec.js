/**
 * 화면의 팀(프로젝트 관리자 + 투입 인력, 이름 기준)과 서버의 팀 스펙(사용자 ID 기준) 사이 변환.
 *  - 화면 형태: { manager: { name, role }, team: [{ name, role, phases, mm, rate, from, to, auto, access, notify, gaps, monthPlan }], restricted }
 *  - 서버 요청 형태(Spec): { manager: { userId, roleCode }, team: [{ userId, roleCode, phases, mm, ratePct, fromDate, toDate, autoEnd, accessLevel, notifyChannel, gaps, monthPlan }], restricted }
 *  - 서버 응답 형태(TeamResponse): Spec 과 같지만 userName 이 함께 온다.
 */
import { userIdByName, userNameById } from './pmsUsers'

const num = (v) => (v === '' || v === null || v === undefined || Number.isNaN(Number(v)) ? null : Number(v))

/** 화면 팀 → 서버 요청 Spec. 이름에 해당하는 사용자가 없으면 예외. */
export function teamToSpec({ manager, team, restricted }) {
  const idOf = (name) => {
    const id = userIdByName(name)
    if (!id) throw new Error(`사용자 목록에 없는 이름입니다: ${name}`)
    return id
  }
  return {
    manager: { userId: idOf(manager.name), roleCode: manager.role },
    team: team.map((m) => ({
      userId: idOf(m.name),
      roleCode: m.role,
      phases: m.phases ?? [],
      mm: num(m.mm),
      ratePct: num(m.rate),
      fromDate: m.from || null,
      toDate: m.to || null,
      autoEnd: !!m.auto,
      accessLevel: m.access || '편집',
      notifyChannel: m.notify || null,
      gaps: (m.gaps ?? []).filter((g) => g.from && g.to).map((g) => ({ fromDate: g.from, toDate: g.to })),
      monthPlan: m.monthPlan ?? {},
    })),
    restricted: !!restricted,
  }
}

const memberFromApi = (m, name) => ({
  name,
  role: m.roleCode,
  phases: m.phases ?? [],
  mm: m.mm ?? '',
  rate: m.ratePct ?? undefined,
  from: m.fromDate ?? '',
  to: m.toDate ?? '',
  auto: !!m.autoEnd,
  access: m.accessLevel ?? '편집',
  notify: m.notifyChannel ?? '',
  gaps: (m.gaps ?? []).map((g) => ({ from: g.fromDate, to: g.toDate })),
  monthPlan: m.monthPlan ?? {},
})

/** 서버 TeamResponse(이름 포함) → 화면 팀. */
export function teamFromResponse(resp) {
  if (!resp?.manager) return null
  return {
    manager: { name: resp.manager.userName, role: resp.manager.roleCode },
    team: (resp.team ?? []).map((m) => memberFromApi(m, m.userName)),
    restricted: !!resp.restricted,
  }
}

/** 서버 Spec(사용자 ID만 있음; 변경 요청 스냅샷·전환 선택값) → 화면 팀. 사용자 디렉터리가 먼저 로드되어 있어야 한다. */
export function teamFromSpec(spec) {
  if (!spec?.manager) return null
  return {
    manager: { name: userNameById(spec.manager.userId), role: spec.manager.roleCode },
    team: (spec.team ?? []).map((m) => memberFromApi(m, userNameById(m.userId))),
    restricted: !!spec.restricted,
  }
}