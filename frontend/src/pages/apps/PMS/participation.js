/**
 * 프로젝트 참여/조회 범위 — 이슈 관리 등에서 "내가 참여 중인 프로젝트"를 구하는 공통 규칙.
 *  - 센터장·admin(ROLE_PMS_MANAGER / ROLE_PMS_APPROVE / ROLE_ADMIN / ROLE_SYSTEM_ADMIN)은 수행 중인 전체 프로젝트를 본다.
 *  - 그 외에는 프로젝트 관리자·투입 인력·영업 담당·수행 PM으로 참여한 수행 중 프로젝트만 본다.
 */
const ALL_ACCESS_ROLES = ['ROLE_PMS_MANAGER', 'ROLE_PMS_APPROVE', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN']
const APPROVE_ROLES = ['ROLE_PMS_APPROVE', 'ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN']

export const canSeeAllProjects = (auth) => (auth?.roles ?? []).some((r) => ALL_ACCESS_ROLES.includes(r))

/** 단계전환·투입 변경 승인, RFP 확정 M/D — 서버도 같은 권한(PMS_APPROVE / ADMIN)을 검사한다. */
export const canApprove = (auth) => (auth?.roles ?? []).some((r) => APPROVE_ROLES.includes(r))

export function participates(project, me) {
  const exec = project.transitioned
  return (
    exec?.manager?.name === me ||
    !!exec?.team?.some((m) => m.name === me) ||
    project.salesName === me ||
    project.pmName === me
  )
}

/** 이슈 관리 대상 프로젝트: 수행 중이면서 (전체 열람 권한이거나 내가 참여한) 프로젝트. */
export function issueProjects(projects, auth, me) {
  const all = canSeeAllProjects(auth)
  return projects.filter((p) => p.status === '수행 중' && p.transitioned && (all || participates(p, me)))
}
