/**
 * 단계전환 승인 시 수행 단계 기초데이터(WBS 업무, 문서 테일러링)를 만든다.
 * WBS·문서 테일러링은 아직 서버 테이블(3단계)이 없어 메모리(목업)로 생성하며, 요청에 저장된 선택값(selection)만으로 만든다 —
 * 요청한 사람의 화면 상태가 필요 없으므로 승인자의 브라우저에서 만들어도 같은 결과가 나온다(RFP·평가는 같은 목업 데이터).
 */
import { buildTailoringFor } from './docStore'
import { itemEvalSummary } from './evalStore'
import { CHAIN, buildProjectWbs } from './pmsStore'
import { getRfps } from './rfpStore'
import { teamFromSpec } from './teamCodec'
import { buildPools } from './teamConfig'

/**
 * @param project  화면 프로젝트(id = 프로젝트 코드, start = 시작 예정일)
 * @param selection 서버 전환 요청의 selection({ phases, rfpId, itemIds, assignMode, team })
 * @returns { tasks, tailoring } — addProjectTasks / initTailoring 에 그대로 넘긴다.
 */
export function buildTransitionArtifacts(project, selection) {
  const phases = CHAIN.filter((p) => (selection.phases ?? []).includes(p))
  const ui = teamFromSpec(selection.team)
  const rfp = selection.rfpId ? getRfps().find((r) => r.id === selection.rfpId) : null
  const itemIds = new Set(selection.itemIds ?? [])
  const chosen = rfp ? rfp.items.filter((i) => itemIds.has(i.id)) : []

  const features = chosen.map((i) => {
    const md = itemEvalSummary(rfp, i)?.md ?? 3
    return { code: i.code, name: i.name, categoryName: `${i.category}`, md, weight: Math.max(1, md) }
  })
  // 담당자 해석: 투입 인력의 투입 단계·역할로 단계별 담당 후보를 만든다. 관리 업무는 프로젝트 관리자가 맡는다.
  const pools = buildPools(ui.team, phases, ui.manager)
  const owners = { admin: ui.manager.name, mode: selection.assignMode ?? 'auto', byPhase: pools }
  const tasks = buildProjectWbs({ projectId: project.id, startDate: project.start, phases, features, owners })
  const docOwners = Object.fromEntries(CHAIN.map((ph) => [ph, pools[ph]?.[0]?.name ?? ui.manager.name]))
  return { tasks, tailoring: { phases, owners: docOwners, categories: new Set(chosen.map((i) => i.category)) }, chosen }
}

export { buildTailoringFor }
