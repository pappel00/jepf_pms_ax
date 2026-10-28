import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './pms.css'
import { useAuth } from '../../../context/AuthContext'
import { ProcessButton } from './ProcessModal'
import { transitionProcess } from './processModels'
import { buildTailoringFor } from './docStore'
import { useIssues } from './issueStore'
import { canApprove as hasApproveRole } from './participation'
import { approveTransition, cancelTransition, pendingTransitionOf, rejectTransition, requestTransition, rollbackBlocker, rollbackProjectTransition, useRollbackLog, useTransitionRequests } from './transitionStore'
import { teamToSpec } from './teamCodec'
import { usePmsUsers } from './pmsUsers'
import { itemEvalSummary, useEvals } from './evalStore'
import {
  CHAIN,
  ME,
  TODAY,
  buildProjectWbs,
  isLeaf,
  useTasks,
} from './pmsStore'
import { formatKoreanWon, useProjects } from './projectStore'
import { REQ_CATEGORIES, categoryName, useRfps } from './rfpStore'
import TeamEditor from './TeamEditor'
import { ASSIGN_MODES, buildPools, defaultManager, defaultTeam } from './teamConfig'

const PHASE_DESC = {
  분석: '요구사항 분석 · 정의 업무와 분석 산출물',
  설계: '아키텍처·화면·인터페이스·DB 설계 업무와 설계 산출물',
  개발: '구현 · 단위테스트 업무와 개발 산출물',
  테스트: '통합·성능·인수 테스트 업무와 테스트 산출물',
}
/**
 * 단계전환 — 수주가 확정된 프로젝트를 수행 단계로 넘기면서 기초데이터를 만든다.
 *  1. 프로젝트 단계(분석/설계/개발/테스트) 선택  2. RFP 항목 선택  3. 프로젝트 관리자(1명) + 투입 인력(역할·투입 단계) 선택
 *  → 문서 테일러링 관리와 WBS/진척 관리의 기초데이터를 생성하고 프로젝트를 「수행 중」으로 전환한다.
 */
export default function TransitionPage() {
  const projects = useProjects()
  const rfps = useRfps()
  const tasks = useTasks()
  useEvals()
  const navigate = useNavigate()

  const { auth } = useAuth()
  const issues = useIssues()
  const requests = useTransitionRequests()
  const rollbackLog = useRollbackLog()
  usePmsUsers() // 사용자 디렉터리 로드(이름 ↔ 사용자 ID 변환)
  const canApprove = hasApproveRole(auth) // 승인 권한(PMS_APPROVE·admin)만 승인·반려·롤백 (서버도 같은 권한을 검사)

  const candidates = projects.filter((p) => p.status === '수주' && !tasks.some((t) => t.projectId === p.id) && !pendingTransitionOf(requests, p.id))
  const history = projects.filter((p) => p.transitioned)
  const pendingRequests = requests.filter((r) => r.status === '승인 대기')
  const decidedRequests = requests.filter((r) => r.status !== '승인 대기')

  const [projectId, setProjectId] = useState(null)
  const [phases, setPhases] = useState(new Set(CHAIN))
  const [rfpId, setRfpId] = useState('')
  const [itemIds, setItemIds] = useState(new Set())
  const [manager, setManager] = useState(defaultManager()) // 프로젝트 관리자 1명 { name, role }
  const [team, setTeam] = useState(defaultTeam()) // 투입 인력 [{ name, role, phases, mm, from, to, notify }]
  const [assignMode, setAssignMode] = useState('auto')
  const [result, setResult] = useState(null)

  const project = candidates.find((p) => p.id === projectId)
  const projectRfps = project ? rfps.filter((r) => r.projectId === project.id) : []
  const rfp = projectRfps.find((r) => r.id === rfpId) ?? projectRfps[0]

  const selectProject = (p) => {
    setResult(null)
    setProjectId(p.id)
    setPhases(new Set(CHAIN))
    const r = rfps.find((x) => x.projectId === p.id)
    setRfpId(r?.id ?? '')
    // 기본 선택: 평가에서 비기능으로 판정된 항목을 제외한 나머지
    setItemIds(new Set((r?.items ?? []).filter((i) => itemEvalSummary(r, i).kind !== '비기능').map((i) => i.id)))
    setManager(defaultManager())
    setTeam(defaultTeam({ start: p.start, end: p.end })) // 투입 기간 기본값 = 프로젝트 기간
    setAssignMode('auto')
  }

  const togglePhase = (ph) =>
    setPhases((prev) => {
      const next = new Set(prev)
      next.has(ph) ? next.delete(ph) : next.add(ph)
      return next
    })
  const toggleItem = (id) =>
    setItemIds((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  const setMany = (ids, on) =>
    setItemIds((prev) => {
      const next = new Set(prev)
      ids.forEach((id) => (on ? next.add(id) : next.delete(id)))
      return next
    })

  // ---------- 미리보기(실행 전 계산) ----------
  const summaries = rfp ? Object.fromEntries(rfp.items.map((i) => [i.id, itemEvalSummary(rfp, i)])) : {}
  const chosen = rfp ? rfp.items.filter((i) => itemIds.has(i.id)) : []
  const features = chosen.map((i) => {
    const md = summaries[i.id]?.md ?? 3
    return { code: i.code, name: i.name, categoryName: `${i.category} ${categoryName(i.category)}`, md, weight: Math.max(1, md) }
  })
  const selectedPhases = CHAIN.filter((p) => phases.has(p))
  // 담당자 해석: 투입 인력의 투입 단계·역할로 단계별 담당 후보를 만든다. 관리 업무는 프로젝트 관리자가 맡는다.
  const pools = buildPools(team, selectedPhases, manager) // 투입 인력이 없는 단계는 프로젝트 관리자가 후보
  const fallbackPhases = selectedPhases.filter((ph) => pools[ph][0]?.fallback)
  const docOwners = Object.fromEntries(CHAIN.map((ph) => [ph, pools[ph]?.[0]?.name ?? manager.name]))
  const wbsOwners = { admin: manager.name, mode: assignMode, byPhase: pools }
  const preview =
    project && selectedPhases.length > 0
      ? buildProjectWbs({ projectId: project.id, startDate: project.start, phases: selectedPhases, features, owners: wbsOwners })
      : []
  const leaves = preview.filter((t) => isLeaf(preview, t))
  const rangeStart = leaves.map((t) => t.start).sort()[0]
  const rangeEnd = leaves.map((t) => t.end).sort().slice(-1)[0]
  const tailoring =
    project && selectedPhases.length > 0
      ? buildTailoringFor(project.id, { phases: selectedPhases, owners: docOwners, categories: new Set(chosen.map((i) => i.category)) })
      : []
  const docApplied = tailoring.filter((t) => t.applied === '적용')
  const docAuto = tailoring.filter((t) => !t.required && t.applied === '적용')
  const totalMd = features.reduce((s, f) => s + f.md, 0)
  const unevaluated = chosen.filter((i) => !summaries[i.id]?.count).length
  const nonFunc = chosen.filter((i) => summaries[i.id]?.kind === '비기능').length

  const execute = async () => {
    if (selectedPhases.length === 0) return window.alert('프로젝트 단계를 하나 이상 선택하세요.')
    if (!manager.name) return window.alert('프로젝트 관리자를 선택하세요.')
    if (team.some((m) => !m.name)) return window.alert('투입 인력의 이름을 모두 선택하세요.')
    if (team.some((m) => m.phases.length === 0)) return window.alert('투입 단계가 없는 투입 인력이 있습니다. 투입 단계를 선택하거나 삭제하세요.')
    if (team.some((m) => m.from && m.to && m.to < m.from)) return window.alert('투입 기간의 종료일이 시작일보다 빠른 인력이 있습니다.')
    if (!rfp && !window.confirm('등록된 RFP 항목이 없습니다. 항목 없이 단계와 표준 산출물만으로 기초데이터를 만들까요?')) return
    if (rfp && chosen.length === 0 && !window.confirm('선택한 RFP 항목이 없습니다. 항목 없이 단계와 표준 산출물만으로 기초데이터를 만들까요?')) return
    if (!window.confirm(`"${project.name}" 단계전환을 요청할까요?\n\n• WBS 업무 ${leaves.length}건 생성 예정\n• 문서 테일러링 ${docApplied.length}종 적용 예정\n\n승인자(센터장·admin)가 승인하면 실제로 반영됩니다.`)) return
    // 선택값(단계·RFP 항목·배정 방식·관리자/투입 인력)을 요청에 담아 서버에 저장한다 — 승인 시점에 이 값으로 기초데이터를 만든다.
    try {
      await requestTransition(project, {
        phases: selectedPhases,
        rfpId: rfp?.id ?? null,
        itemIds: chosen.map((i) => i.id),
        itemCount: chosen.length,
        assignMode,
        summary: `단계 ${selectedPhases.join(' → ')} · RFP 항목 ${chosen.length}건 · WBS 업무 ${leaves.length}건 · 문서 ${docApplied.length}종 · 관리자 ${manager.name}(${manager.role}), 투입 ${team.length}명`,
        team: teamToSpec({ manager, team, restricted: false }),
      })
    } catch (e) {
      return window.alert(e.message)
    }
    setResult({ id: project.id, name: project.name, requested: true })
    setProjectId(null)
  }

  const categories = rfp ? REQ_CATEGORIES.filter((c) => rfp.items.some((i) => i.category === c.code)) : []

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            수행단계전환
          </h2>
          <p className="page-description">
            수주가 확정된 프로젝트를 수행 단계로 넘깁니다. <b>프로젝트 단계</b>와 <b>RFP 항목</b>, <b>프로젝트 관리자·투입 인력</b>을 선택하면 수행 단계의
            기초데이터(문서 테일러링 관리 · WBS/진척 관리)를 자동으로 만들고 프로젝트를 「수행 중」으로 전환합니다.
          </p>
        </div>
      </div>

      {result && (
        <div className="pms-panel" style={{ background: '#f1faf3', border: '1px solid #bfe3c8' }}>
          <h3 style={{ fontSize: 15 }}>✅ 단계전환 요청 등록 — {result.name}</h3>
          <p style={{ margin: '4px 0 10px', fontSize: 13 }}>
            승인 대기 중입니다. 아래 「단계전환 승인」에서 승인자(센터장·admin)가 승인하면 WBS·문서 테일러링 기초데이터가 생성되고 프로젝트가 <b>수행 중</b>으로 바뀝니다.
          </p>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="link-btn" onClick={() => setResult(null)}>
              닫기
            </button>
          </div>
        </div>
      )}

      {/* 단계전환 승인 */}
      {(pendingRequests.length > 0 || decidedRequests.length > 0) && (
        <div className="pms-panel">
          <h3>
            단계전환 승인 <span className="count">{pendingRequests.length}</span>
            <span className="muted" style={{ fontWeight: 400 }}>
              {canApprove ? '승인 대기 건을 승인/반려할 수 있습니다' : '승인은 센터장·admin만 할 수 있습니다'}
            </span>
          </h3>
          <table className="data-table pms-wbs">
            <thead>
              <tr>
                <th>요청 번호</th>
                <th>프로젝트</th>
                <th>요청자 / 일자</th>
                <th>요청 내용</th>
                <th>상태 / 처리</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {[...pendingRequests, ...decidedRequests].map((r) => (
                <tr key={r.id}>
                  <td><b>{r.id}</b></td>
                  <td className="name-cell">{r.projectName}</td>
                  <td>{r.requester}<div className="muted">{r.requestedAt}</div></td>
                  <td className="name-cell">{r.summary}</td>
                  <td>
                    <span className={`pms-tag ${r.status === '승인' ? 'done' : r.status === '승인 대기' ? 'manual' : r.status === '반려' ? 'late' : ''}`}>{r.status}</span>
                    {r.decidedBy && <div className="muted">{r.decidedBy} · {r.decidedAt}{r.comment ? ` · ${r.comment}` : ''}</div>}
                  </td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <ProcessButton getModel={() => transitionProcess(r)} />
                    {r.status === '승인 대기' && (
                      <>
                        {canApprove && (
                          <button
                            className="link-btn"
                            onClick={() => {
                              if (!window.confirm('승인하면 투입 인력이 확정되고 WBS·문서 테일러링이 생성되며 프로젝트가 「수행 중」이 됩니다. 승인할까요?')) return
                              approveTransition(r.id).catch((e) => window.alert(e.message))
                            }}
                          >
                            승인
                          </button>
                        )}
                        {canApprove && (
                          <button
                            className="link-btn danger"
                            onClick={() => {
                              const comment = window.prompt('반려 사유를 입력하세요 (필수)')
                              if (comment && comment.trim()) rejectTransition(r.id, comment.trim()).catch((e) => window.alert(e.message))
                            }}
                          >
                            반려
                          </button>
                        )}
                        {(
                          <button className="link-btn" onClick={() => cancelTransition(r.id).catch((e) => window.alert(e.message))}>
                            요청 취소
                          </button>
                        )}
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* 전환 대상 프로젝트 */}
      <div className="pms-panel">
        <h3>
          단계전환 대상 프로젝트 <span className="count">{candidates.length}</span>
          <span className="muted" style={{ fontWeight: 400 }}>수주 확정 · 아직 단계전환하지 않은 프로젝트</span>
        </h3>
        <table className="data-table pms-wbs">
          <thead>
            <tr>
              <th></th>
              <th>코드</th>
              <th>프로젝트명</th>
              <th>발주사</th>
              <th>기간 (예정)</th>
              <th style={{ textAlign: 'right' }}>발주금액(원)</th>
              <th>영업 담당</th>
              <th>RFP / 항목</th>
              <th>RFP 평가</th>
            </tr>
          </thead>
          <tbody>
            {candidates.length === 0 && (
              <tr>
                <td className="empty-cell" colSpan={9}>
                  단계전환할 프로젝트가 없습니다. (수주 확정 프로젝트가 모두 전환되었습니다)
                </td>
              </tr>
            )}
            {candidates.map((p) => {
              const prs = rfps.filter((r) => r.projectId === p.id)
              const itemCount = prs.reduce((s, r) => s + r.items.length, 0)
              const evaluators = prs[0]?.evaluators ?? []
              return (
                <tr key={p.id} className={p.id === projectId ? 'row-selected' : ''} style={{ cursor: 'pointer' }} onClick={() => selectProject(p)}>
                  <td>
                    <input type="radio" readOnly checked={p.id === projectId} />
                  </td>
                  <td>{p.id}</td>
                  <td className="name-cell">
                    <b>{p.name}</b>
                  </td>
                  <td>{p.client}</td>
                  <td>
                    {p.start} ~ {p.end}
                  </td>
                  <td style={{ textAlign: 'right' }}>{p.amount?.toLocaleString()}</td>
                  <td>{p.salesName}</td>
                  <td>{prs.length ? `${prs.length}건 / 항목 ${itemCount}` : <span className="muted">RFP 없음</span>}</td>
                  <td>{evaluators.length ? `평가자 ${evaluators.length}명` : '-'}</td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {!project && !result && candidates.length > 0 && <p className="muted">위 목록에서 프로젝트를 선택하면 단계전환 설정이 표시됩니다.</p>}

      {project && (
        <>
          {/* 1. 프로젝트 단계 */}
          <div className="pms-panel">
            <h3>1. 프로젝트 단계 선택</h3>
            <div className="pms-phase-cards">
              {CHAIN.map((ph, i) => (
                <label key={ph} className={`pms-phase-card ${phases.has(ph) ? 'on' : ''}`}>
                  <input type="checkbox" checked={phases.has(ph)} onChange={() => togglePhase(ph)} />
                  <div>
                    <b>
                      {i + 1}. {ph}
                    </b>
                    <small>{PHASE_DESC[ph]}</small>
                  </div>
                </label>
              ))}
            </div>
            <div className="pms-field-hint">
              선택한 단계만 WBS와 문서 테일러링에 생성됩니다. 같은 기능(RFP 항목)의 업무는 선택한 단계 순서대로 선행 관계가 걸립니다. 사업관리 단계는 항상 생성됩니다.
            </div>
          </div>

          {/* 2. RFP 항목 */}
          <div className="pms-panel">
            <h3>
              2. RFP 항목 선택
              {rfp && (
                <span className="muted" style={{ fontWeight: 400 }}>
                  {rfp.id} · {rfp.title}
                </span>
              )}
            </h3>
            {projectRfps.length > 1 && (
              <select
                className="pms-cell-input"
                style={{ width: 360, marginBottom: 10 }}
                value={rfp.id}
                onChange={(e) => {
                  const r = rfps.find((x) => x.id === e.target.value)
                  setRfpId(r.id)
                  setItemIds(new Set(r.items.filter((i) => itemEvalSummary(r, i).kind !== '비기능').map((i) => i.id)))
                }}
              >
                {projectRfps.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.id} · {r.title}
                  </option>
                ))}
              </select>
            )}
            {!rfp ? (
              <div className="pms-empty">
                이 프로젝트에 등록된 RFP가 없습니다. RFP 항목 없이 단계와 표준 산출물만으로 기초데이터를 만들 수 있습니다.
              </div>
            ) : (
              <>
                <div style={{ display: 'flex', gap: 6, marginBottom: 10, flexWrap: 'wrap', alignItems: 'center' }}>
                  <button className="secondary small" onClick={() => setMany(rfp.items.map((i) => i.id), true)}>
                    전체 선택
                  </button>
                  <button className="secondary small" onClick={() => setItemIds(new Set(rfp.items.filter((i) => summaries[i.id]?.kind !== '비기능').map((i) => i.id)))}>
                    기능 항목만
                  </button>
                  <button className="secondary small" onClick={() => setItemIds(new Set(rfp.items.filter((i) => i.mandatory).map((i) => i.id)))}>
                    필수 항목만
                  </button>
                  <button className="secondary small" onClick={() => setItemIds(new Set())}>
                    전체 해제
                  </button>
                  <span style={{ marginLeft: 'auto', fontSize: 13 }}>
                    선택 <b>{chosen.length}</b>/{rfp.items.length}건 · 평가 M/D 합계 <b>{totalMd.toFixed(1)}</b>
                  </span>
                </div>
                <table className="data-table pms-wbs">
                  <thead>
                    <tr>
                      <th></th>
                      <th>코드</th>
                      <th>항목명</th>
                      <th>요구사항 (원문)</th>
                      <th>필수</th>
                      <th>평가 구분</th>
                      <th style={{ textAlign: 'right' }}>평균 M/D</th>
                      <th>난이도</th>
                      <th>평가</th>
                    </tr>
                  </thead>
                  <tbody>
                    {categories.flatMap((c) => {
                      const rows = rfp.items.filter((i) => i.category === c.code)
                      const ids = rows.map((i) => i.id)
                      const allOn = ids.every((id) => itemIds.has(id))
                      const sub = rows.filter((i) => itemIds.has(i.id)).reduce((s, i) => s + (summaries[i.id]?.md ?? 0), 0)
                      return [
                        <tr key={`h-${c.code}`} className="phase-row">
                          <td>
                            <input type="checkbox" checked={allOn} onChange={() => setMany(ids, !allOn)} />
                          </td>
                          <td colSpan={5}>
                            {c.code} · {c.name} <span className="muted">({rows.filter((i) => itemIds.has(i.id)).length}/{rows.length}건 선택)</span>
                          </td>
                          <td style={{ textAlign: 'right' }}>{sub ? sub.toFixed(1) : ''}</td>
                          <td colSpan={2} />
                        </tr>,
                        ...rows.map((i) => {
                          const s = summaries[i.id]
                          return (
                            <tr key={i.id} style={itemIds.has(i.id) ? undefined : { opacity: 0.55 }}>
                              <td>
                                <input type="checkbox" checked={itemIds.has(i.id)} onChange={() => toggleItem(i.id)} />
                              </td>
                              <td>
                                <span className="pms-tag req">{i.code}</span>
                              </td>
                              <td className="name-cell">
                                <b>{i.name}</b>
                                <div className="muted">{i.area}</div>
                              </td>
                              <td className="name-cell" style={{ maxWidth: 340, color: '#555' }}>
                                {i.content}
                              </td>
                              <td>{i.mandatory ? <span className="pms-tag late">필수</span> : <span className="pms-tag">선택</span>}</td>
                              <td>{s?.kind ? <span className={`pms-tag ${s.kind === '기능' ? 'doing' : ''}`}>{s.kind}</span> : <span className="muted">미평가</span>}</td>
                              <td style={{ textAlign: 'right' }}>{s?.md ? s.md.toFixed(1) : '-'}</td>
                              <td>{s?.difficulty ? <span className={`pms-tag ${s.difficulty === '상' ? 'late' : ''}`}>{s.difficulty}</span> : '-'}</td>
                              <td>
                                <span className={`pms-tag ${s?.count && s.count >= s.total ? 'done' : ''}`}>
                                  {s?.count ?? 0}/{rfp.evaluators.length} 제출
                                </span>
                              </td>
                            </tr>
                          )
                        }),
                      ]
                    })}
                  </tbody>
                </table>
                <div className="pms-field-hint">평가 구분·M/D·난이도는 제출된 RFP 평가를 요약한 참고값입니다(구분은 다수결, M/D는 평균, 난이도는 가장 높은 값). M/D는 업무 소요일과 가중치에 반영됩니다.</div>
              </>
            )}
          </div>

          {/* 3. 프로젝트 관리자 / 투입 인력 */}
          <div className="pms-panel">
            <h3>3. 프로젝트 관리자 · 투입 인력 선택</h3>
            <TeamEditor
              manager={manager}
              setManager={setManager}
              team={team}
              setTeam={setTeam}
              phases={phases}
              period={{ start: project.start, end: project.end }}
            />

            <div className="pms-form-section">단계별 업무 담당 배정 방식</div>
            {ASSIGN_MODES.map((a) => (
              <label key={a.value} className="pms-radio">
                <input type="radio" name="assign-mode" checked={assignMode === a.value} onChange={() => setAssignMode(a.value)} />
                <span>
                  <b>{a.label}</b>
                  <small>{a.desc}</small>
                </span>
              </label>
            ))}

            <div className="pms-form-section">단계별 담당 미리보기</div>
            <table className="data-table pms-wbs">
              <thead>
                <tr>
                  <th>단계</th>
                  <th>WBS 업무 담당 후보 (역할 우선순위 순 · 투입 M/M)</th>
                </tr>
              </thead>
              <tbody>
                {selectedPhases.map((ph) => (
                  <tr key={ph}>
                    <td>
                      <b>{ph}</b>
                    </td>
                    <td>
                      {assignMode === 'none' ? (
                        <span className="pms-tag manual">미배정 (WBS에서 직접 지정)</span>
                      ) : pools[ph].length === 0 ? (
                        <span className="pms-tag late">미배정</span>
                      ) : pools[ph][0].fallback ? (
                        <span>
                          <b>{pools[ph][0].name}</b> <span className="pms-tag manual">투입 인력 없음 → 프로젝트 관리자에게 배정</span>
                        </span>
                      ) : (
                        (assignMode === 'lead' ? pools[ph].slice(0, 1) : pools[ph]).map((p, i) => (
                          <span key={p.name} style={{ marginRight: 12 }}>
                            <b>{p.name}</b> <span className="muted">({p.role}{pools[ph].length > 1 && assignMode === 'auto' ? ` · M/M ${p.weight}` : ''})</span>
                            {i === 0 && assignMode === 'lead' ? <span className="pms-tag doing" style={{ marginLeft: 4 }}>대표</span> : null}
                          </span>
                        ))
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="pms-field-hint">
              업무당 담당자는 1명입니다. 한 단계에 여러 명이 투입되면 <b>투입 인력 전체</b>에게 투입 M/M 비율(M/M이 없으면 균등)로 나눠 배정하고, 목록은 그 단계의
              주 역할이 위에 옵니다(개발 단계는 개발자 → DBA → UI/UX → TA → PL …). PMO는 다른 인력이 있으면 업무에서 제외됩니다. 같은 업무를 여러 명이
              해야 하면 WBS에서 업무 복사를 쓰세요.
            </div>
          </div>

          {/* 생성 미리보기 */}
          <div className="pms-panel" style={{ background: '#fafbfd' }}>
            <h3>생성될 기초데이터 미리보기</h3>
            <div className="pms-summary">
              <div className="pms-stat">
                <div className="label">WBS 업무(리프)</div>
                <div className="value">{leaves.length}</div>
                <div className="muted">
                  RFP 항목 {chosen.length}건 × 단계 {selectedPhases.length}개 + 관리 업무
                  {leaves.some((t) => !t.assignee) && ` · 미배정 ${leaves.filter((t) => !t.assignee).length}건`}
                  {` · 관리자 ${manager.name}(${manager.role}), 투입 ${team.length}명`}
                </div>
              </div>
              <div className="pms-stat">
                <div className="label">WBS 예상 기간</div>
                <div className="value" style={{ fontSize: 16 }}>
                  {rangeStart ? `${rangeStart} ~ ${rangeEnd}` : '-'}
                </div>
                <div className="muted">총 M/D {totalMd.toFixed(1)} 기준</div>
              </div>
              <div className="pms-stat ok">
                <div className="label">문서 테일러링 적용</div>
                <div className="value">{docApplied.length}</div>
                <div className="muted">
                  표준 필수 + RFP 분류 연동 자동 적용 {docAuto.length}종
                </div>
              </div>
              <div className="pms-stat">
                <div className="label">자동 제외 (사유 기록)</div>
                <div className="value">{tailoring.length - docApplied.length}</div>
                <div className="muted">전환 후 문서 테일러링에서 변경 가능</div>
              </div>
            </div>
            {docAuto.length > 0 && (
              <div className="pms-field-hint" style={{ marginBottom: 6 }}>
                RFP 분류에 따라 자동 적용되는 권장 산출물: {docAuto.map((t) => t.name).join(', ')}
              </div>
            )}
            {unevaluated > 0 && <div className="pms-field-error">⚠ 평가가 제출되지 않은 항목 {unevaluated}건이 포함되어 있습니다(M/D 3.0 기본값으로 반영).</div>}
            {nonFunc > 0 && <div className="pms-field-hint">ℹ️ 비기능으로 평가된 항목 {nonFunc}건도 선택한 단계마다 업무가 생성됩니다.</div>}
            <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
              <button className="primary" style={{ flex: '0 0 auto', width: 220 }} onClick={execute}>
                단계전환 실행
              </button>
              <button className="secondary" style={{ marginTop: 0 }} onClick={() => setProjectId(null)}>
                취소
              </button>
            </div>
          </div>
        </>
      )}

      {/* 전환 이력 */}
      {history.length > 0 && (
        <div className="pms-panel">
          <h3>
            단계전환 이력 <span className="count">{history.length}</span>
          </h3>
          <table className="data-table pms-wbs">
            <thead>
              <tr>
                <th>프로젝트</th>
                <th>전환일</th>
                <th>프로젝트 관리자</th>
                <th>투입 인력</th>
                <th>단계</th>
                <th>RFP / 항목</th>
                <th>발주금액</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {history.map((p) => (
                <tr key={p.id}>
                  <td className="name-cell">
                    <b>{p.name}</b> <span className="muted">({p.id})</span>
                  </td>
                  <td>{p.transitioned.date}</td>
                  <td>
                    {p.transitioned.manager.name} <span className="pms-tag">{p.transitioned.manager.role}</span>
                  </td>
                  <td title={p.transitioned.team.map((m) => `${m.name}(${m.role}: ${m.phases.length === 4 ? '전체' : m.phases.join('·')}) M/M ${m.mm || '-'} ${m.from || ''}~${m.to || ''}`).join('\n')}>
                    {p.transitioned.team.length}명 · M/M {p.transitioned.team.reduce((s, m) => s + (Number(m.mm) || 0), 0)}
                  </td>
                  <td>{p.transitioned.phases.join(' → ')}</td>
                  <td>{p.transitioned.rfpId ? `${p.transitioned.rfpId} / ${p.transitioned.itemCount}건` : 'RFP 없음'}</td>
                  <td>{formatKoreanWon(p.amount)}</td>
                  <td>
                    <button className="link-btn" onClick={() => navigate(`/workspace/PMS/wbs?project=${p.id}`)}>
                      WBS ›
                    </button>
                    <button className="link-btn" onClick={() => navigate(`/workspace/PMS/doc-tailoring?project=${p.id}`)}>
                      문서 테일러링 ›
                    </button>
                    {p.status === '수행 중' && (() => {
                      const blocker = rollbackBlocker(tasks.filter((t) => t.projectId === p.id), issues.filter((i) => i.projectId === p.id))
                      return (
                        <button
                          className="link-btn danger"
                          disabled={!canApprove || !!blocker}
                          title={!canApprove ? '롤백은 센터장·admin만 할 수 있습니다' : blocker ? `롤백 불가: ${blocker}` : '단계전환을 취소하고 수주 상태로 되돌립니다'}
                          onClick={() => {
                            const reason = window.prompt(`"${p.name}" 단계전환을 롤백합니다. WBS·문서 테일러링이 삭제되고 「수주」로 되돌아갑니다.\n롤백 사유를 입력하세요 (필수)`)
                            if (reason && reason.trim()) rollbackProjectTransition(p, { reason: reason.trim() }).catch((e) => window.alert(e.message))
                          }}
                        >
                          롤백
                        </button>
                      )
                    })()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {rollbackLog.length > 0 && (
            <div style={{ marginTop: 10, fontSize: 12 }}>
              <b>롤백 이력</b>
              {rollbackLog.map((l, i) => (
                <div key={i} className="muted">
                  {l.date} · {l.projectName} · {l.by} · 사유: {l.reason} (전환일 {l.prevDate})
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
