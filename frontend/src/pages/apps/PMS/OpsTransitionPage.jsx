import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './pms.css'
import { DEFAULT_PROJECT_ID, docOf, tailoringOf, useDocs } from './docStore'
import { isOpenStatus, useIssues } from './issueStore'
import { OPS_TYPES, SLA_LEVELS, addDaysISO, carryOverTeam, defaultOpsManager, oneYearAfter } from './opsConfig'
import { TODAY, useTasks } from './pmsStore'
import { formatKoreanWon, transitionToOperation, useProjects } from './projectStore'
import TeamEditor from './TeamEditor'
import { OPS_MANAGER_ROLES, OPS_ROLES } from './teamConfig'

const SLA_KEYS = Object.keys(SLA_LEVELS)

/**
 * 운영단계전환 — 수행 종료(준공/검수 완료) 프로젝트를 운영 단계로 넘기면서 운영 기초데이터를 만든다.
 *  1. 운영 구분·기간·SLA  2. 인수 대상(운영 대상 기능 · 인수 산출물 · 이관 이슈)  3. 운영 관리자 · 운영 인력
 *  → 프로젝트를 「운영 중」으로 전환하고 운영 단계 「프로젝트 관리」의 기초데이터를 남긴다.
 */
export default function OpsTransitionPage() {
  const projects = useProjects()
  const docState = useDocs()
  const issues = useIssues()
  const tasks = useTasks()
  const navigate = useNavigate()

  const candidates = projects.filter((p) => p.status === '수행 종료' && !p.operation)
  const history = projects.filter((p) => p.operation)

  const [projectId, setProjectId] = useState(null)
  const [type, setType] = useState(OPS_TYPES[0].value)
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [slaLevel, setSlaLevel] = useState('표준')
  const [sla, setSla] = useState(SLA_LEVELS.표준)
  const [contractAmount, setContractAmount] = useState(null)
  const [featureSel, setFeatureSel] = useState(new Set())
  const [docSel, setDocSel] = useState(new Set())
  const [issueSel, setIssueSel] = useState(new Set())
  const [manager, setManager] = useState({ name: '', role: '운영 PM' })
  const [team, setTeam] = useState([])
  const [result, setResult] = useState(null)

  const project = candidates.find((p) => p.id === projectId)

  // 프로젝트별 인수 후보 계산
  const regDocs = (p) =>
    tailoringOf(docState, p.id).filter((t) => t.applied === '적용' && docOf(docState, p.id, t.id).versions.length > 0)
  const openIssuesOf = (p) => {
    const ids = new Set(tasks.filter((t) => t.projectId === p.id).map((t) => t.id))
    return issues.filter((i) => isOpenStatus(i.status) && i.projectId === p.id)
  }

  const select = (p) => {
    setResult(null)
    setProjectId(p.id)
    const start = addDaysISO(p.completed?.date ?? TODAY, 1)
    const end = oneYearAfter(start)
    setType(OPS_TYPES[0].value)
    setStartDate(start)
    setEndDate(end)
    setSlaLevel('표준')
    setSla(SLA_LEVELS.표준)
    setContractAmount(null)
    setFeatureSel(new Set(p.completed?.features ?? []))
    setDocSel(new Set(regDocs(p).map((t) => t.id)))
    setIssueSel(new Set(openIssuesOf(p).map((i) => i.id)))
    setManager(defaultOpsManager(p.transitioned?.manager))
    setTeam(carryOverTeam(p.transitioned?.team ?? [], { start, end }))
  }

  const toggle = (setter) => (id) =>
    setter((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })

  const features = project?.completed?.features ?? []
  const docs = project ? regDocs(project) : []
  const openIssues = project ? openIssuesOf(project) : []

  const execute = () => {
    if (!startDate || !endDate || endDate < startDate) return window.alert('운영 기간을 확인하세요(종료일이 시작일보다 빠릅니다).')
    if (!manager.name) return window.alert('운영 관리자를 선택하세요.')
    if (team.some((m) => !m.name)) return window.alert('운영 인력의 이름을 모두 선택하세요.')
    if (team.some((m) => m.from && m.to && m.to < m.from)) return window.alert('투입 기간의 종료일이 시작일보다 빠른 인력이 있습니다.')
    if (!window.confirm(`"${project.name}" 프로젝트를 운영 단계로 전환할까요?\n\n• 운영 대상 기능 ${featureSel.size}건\n• 인수 산출물 ${docSel.size}종\n• 이관 이슈 ${issueSel.size}건\n• 운영 인력 ${team.length}명`)) return
    transitionToOperation(project.id, {
      date: TODAY, type, startDate, endDate, sla: { level: slaLevel, ...sla }, contractAmount,
      manager, team,
      features: features.filter((f) => featureSel.has(f)),
      docs: docs.filter((t) => docSel.has(t.id)).map((t) => ({ phase: t.phase, name: t.name, version: docOf(docState, project.id, t.id).versions.slice(-1)[0].ver })),
      carriedIssues: openIssues.filter((i) => issueSel.has(i.id)).map((i) => ({ id: i.id, title: i.title, priority: i.priority })),
    })
    setResult({ id: project.id, name: project.name, features: featureSel.size, docs: docSel.size, issues: issueSel.size, team: team.length })
    setProjectId(null)
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            운영단계전환 <span className="pms-mock-badge">목업 · 더미 데이터</span>
          </h2>
          <p className="page-description">
            수행이 종료(준공/검수 완료)된 프로젝트를 <b>운영 단계</b>로 넘깁니다. 운영 형태·기간·SLA, 인수 대상(운영 기능 · 산출물 · 미해결 이슈), 운영 관리자·인력을 정하면
            운영 단계 「프로젝트 관리」의 기초데이터를 만들고 프로젝트를 「운영 중」으로 전환합니다.
          </p>
        </div>
      </div>

      {result && (
        <div className="pms-panel" style={{ background: '#f1faf3', border: '1px solid #bfe3c8' }}>
          <h3 style={{ fontSize: 15 }}>✅ 운영단계전환 완료 — {result.name}</h3>
          <p style={{ margin: '4px 0 10px', fontSize: 13 }}>
            프로젝트 상태가 <b>운영 중</b>으로 바뀌었습니다. 운영 대상 기능 {result.features}건 · 인수 산출물 {result.docs}종 · 이관 이슈 {result.issues}건 · 운영 인력 {result.team}명.
          </p>
          <button className="primary small" onClick={() => navigate(`/workspace/PMS/ops-project?project=${result.id}`)}>
            운영 프로젝트 관리 열기 ›
          </button>{' '}
          <button className="link-btn" onClick={() => setResult(null)}>
            닫기
          </button>
        </div>
      )}

      <div className="pms-panel">
        <h3>
          운영단계전환 대상 프로젝트 <span className="count">{candidates.length}</span>
          <span className="muted" style={{ fontWeight: 400 }}>수행 종료 상태 · 아직 운영단계로 전환하지 않은 프로젝트</span>
        </h3>
        <table className="data-table pms-wbs">
          <thead>
            <tr>
              <th></th>
              <th>코드</th>
              <th>프로젝트명</th>
              <th>발주사</th>
              <th>수행 기간</th>
              <th>수행 종료일</th>
              <th>수행 관리자</th>
              <th>운영 대상 기능</th>
              <th>인수 산출물</th>
              <th>미해결 이슈</th>
            </tr>
          </thead>
          <tbody>
            {candidates.length === 0 && (
              <tr>
                <td className="empty-cell" colSpan={10}>
                  운영단계로 전환할 프로젝트가 없습니다. (수행 단계 「프로젝트 관리」에서 수행 종료 처리하면 이곳에 나타납니다)
                </td>
              </tr>
            )}
            {candidates.map((p) => (
              <tr key={p.id} className={p.id === projectId ? 'row-selected' : ''} style={{ cursor: 'pointer' }} onClick={() => select(p)}>
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
                <td>{p.completed?.date}</td>
                <td>{p.transitioned?.manager.name}</td>
                <td>{p.completed?.features?.length ?? 0}건</td>
                <td>{regDocs(p).length}종</td>
                <td>{openIssuesOf(p).length}건</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {!project && !result && candidates.length > 0 && <p className="muted">위 목록에서 프로젝트를 선택하면 운영단계전환 설정이 표시됩니다.</p>}

      {project && (
        <>
          <div className="pms-panel">
            <h3>1. 운영 구분 · 기간 · SLA</h3>
            {OPS_TYPES.map((t) => (
              <label key={t.value} className="pms-radio">
                <input type="radio" name="ops-type" checked={type === t.value} onChange={() => setType(t.value)} />
                <span>
                  <b>{t.value}</b>
                  <small>{t.desc}</small>
                </span>
              </label>
            ))}
            <div className="pms-form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', marginTop: 10 }}>
              <div className="field pms-field">
                <label>
                  운영 시작일<span className="req-mark"> *</span>
                </label>
                <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
                <div className="pms-field-hint">기본: 수행 종료일 다음날</div>
              </div>
              <div className="field pms-field">
                <label>
                  운영 종료일<span className="req-mark"> *</span>
                </label>
                <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
                <div className="pms-field-hint">기본: 시작일로부터 1년</div>
              </div>
              <div className="field pms-field">
                <label>
                  운영 계약금액 (원)<span className="opt-mark"> (옵션)</span>
                </label>
                <input
                  inputMode="numeric"
                  value={contractAmount ? contractAmount.toLocaleString() : ''}
                  onChange={(e) => {
                    const d = e.target.value.replace(/[^\d]/g, '')
                    setContractAmount(d === '' ? null : Number(d))
                  }}
                  placeholder={type === '하자보수' ? '무상이면 비워 두세요' : '숫자만 입력'}
                />
                <div className="pms-field-hint">{contractAmount ? `≈ ${formatKoreanWon(contractAmount)}` : ''}</div>
              </div>
              <div className="field pms-field">
                <label>SLA 등급</label>
                <select
                  value={slaLevel}
                  onChange={(e) => {
                    setSlaLevel(e.target.value)
                    setSla(SLA_LEVELS[e.target.value])
                  }}
                >
                  {SLA_KEYS.map((k) => (
                    <option key={k}>{k}</option>
                  ))}
                </select>
              </div>
              <div className="field pms-field">
                <label>장애 응답 목표 (시간)</label>
                <input type="number" min="1" value={sla.respond} onChange={(e) => setSla({ ...sla, respond: Number(e.target.value) })} />
              </div>
              <div className="field pms-field">
                <label>장애 해결 목표 (시간)</label>
                <input type="number" min="1" value={sla.resolve} onChange={(e) => setSla({ ...sla, resolve: Number(e.target.value) })} />
              </div>
              <div className="field pms-field">
                <label>서비스 시간</label>
                <input value={sla.hours} onChange={(e) => setSla({ ...sla, hours: e.target.value })} />
              </div>
            </div>
          </div>

          <div className="pms-panel">
            <h3>2. 인수 대상 선택</h3>

            <div className="pms-form-section" style={{ marginTop: 4 }}>
              운영 대상 기능 <span className="muted" style={{ fontWeight: 400 }}>{featureSel.size}/{features.length}건 — 수행 단계의 WBS 기능 / RFP 항목에서 구성</span>
            </div>
            {features.length === 0 ? (
              <p className="muted">인수할 기능 목록이 없습니다.</p>
            ) : (
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                {features.map((f) => (
                  <label key={f} className={`pms-phase-card ${featureSel.has(f) ? 'on' : ''}`} style={{ padding: '6px 12px' }}>
                    <input type="checkbox" checked={featureSel.has(f)} onChange={() => toggle(setFeatureSel)(f)} />
                    <span>{f}</span>
                  </label>
                ))}
              </div>
            )}

            <div className="pms-form-section">
              인수 산출물 <span className="muted" style={{ fontWeight: 400 }}>{docSel.size}/{docs.length}종 — 수행 단계에서 등록된 최신 버전 기준</span>
            </div>
            {docs.length === 0 ? (
              <p className="muted">등록된 산출물이 없습니다.</p>
            ) : (
              <table className="data-table pms-wbs">
                <thead>
                  <tr>
                    <th></th>
                    <th>단계</th>
                    <th>문서 종류</th>
                    <th>최신 버전</th>
                  </tr>
                </thead>
                <tbody>
                  {docs.map((t) => (
                    <tr key={t.id}>
                      <td>
                        <input type="checkbox" checked={docSel.has(t.id)} onChange={() => toggle(setDocSel)(t.id)} />
                      </td>
                      <td>{t.phase}</td>
                      <td>{t.name}</td>
                      <td>{docOf(docState, project.id, t.id).versions.slice(-1)[0].ver}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}

            <div className="pms-form-section">
              미해결 이슈 이관 <span className="muted" style={{ fontWeight: 400 }}>{issueSel.size}/{openIssues.length}건 — 선택한 이슈는 운영 이슈로 이관됩니다</span>
            </div>
            {openIssues.length === 0 ? (
              <p className="muted">이관할 미해결 이슈가 없습니다.</p>
            ) : (
              openIssues.map((i) => (
                <label key={i.id} className="pms-radio">
                  <input type="checkbox" checked={issueSel.has(i.id)} onChange={() => toggle(setIssueSel)(i.id)} />
                  <span>
                    <b>{i.id}</b> {i.title} <span className={`pms-tag ${i.priority === '긴급' ? 'late' : ''}`}>{i.priority}</span>
                  </span>
                </label>
              ))
            )}
          </div>

          <div className="pms-panel">
            <h3>3. 운영 관리자 · 운영 인력 선택</h3>
            <div style={{ marginBottom: 8, display: 'flex', gap: 8, alignItems: 'center' }}>
              <button className="secondary small" onClick={() => setTeam(carryOverTeam(project.transitioned?.team ?? [], { start: startDate, end: endDate }))}>
                수행 투입 인력 불러오기
              </button>
              <span className="muted">수행 인력을 운영 역할로 옮깁니다(개발 → 운영, 분석·설계 → TA). 필요에 맞게 조정하세요.</span>
            </div>
            <TeamEditor
              manager={manager}
              setManager={setManager}
              team={team}
              setTeam={setTeam}
              phases={null}
              period={{ start: startDate, end: endDate }}
              managerRoles={OPS_MANAGER_ROLES}
              roles={OPS_ROLES}
              phaseList={['운영']}
              managerHint="운영 단계의 프로젝트 관리(운영 정보·운영 인력·인수 현황)를 관리하는 사람입니다. 수행 관리자와 달라도 됩니다."
            />
          </div>

          <div className="pms-panel" style={{ background: '#fafbfd' }}>
            <h3>생성될 운영 기초데이터 미리보기</h3>
            <div className="pms-summary">
              <div className="pms-stat">
                <div className="label">운영 기간</div>
                <div className="value" style={{ fontSize: 15 }}>
                  {startDate} ~ {endDate}
                </div>
                <div className="muted">{type}</div>
              </div>
              <div className="pms-stat">
                <div className="label">SLA</div>
                <div className="value" style={{ fontSize: 16 }}>{slaLevel}</div>
                <div className="muted">
                  응답 {sla.respond}h · 해결 {sla.resolve}h · {sla.hours}
                </div>
              </div>
              <div className="pms-stat ok">
                <div className="label">인수</div>
                <div className="value" style={{ fontSize: 16 }}>
                  기능 {featureSel.size} · 산출물 {docSel.size} · 이슈 {issueSel.size}
                </div>
              </div>
              <div className="pms-stat">
                <div className="label">운영 인력</div>
                <div className="value">{team.length}</div>
                <div className="muted">관리자 {manager.name} ({manager.role})</div>
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="primary" style={{ width: 220 }} onClick={execute}>
                운영단계전환 실행
              </button>
              <button className="secondary" style={{ marginTop: 0 }} onClick={() => setProjectId(null)}>
                취소
              </button>
            </div>
          </div>
        </>
      )}

      {history.length > 0 && (
        <div className="pms-panel">
          <h3>
            운영단계전환 이력 <span className="count">{history.length}</span>
          </h3>
          <table className="data-table pms-wbs">
            <thead>
              <tr>
                <th>프로젝트</th>
                <th>전환일</th>
                <th>운영 형태 / 기간</th>
                <th>SLA</th>
                <th>운영 관리자</th>
                <th>인수</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {history.map((p) => (
                <tr key={p.id}>
                  <td className="name-cell">
                    <b>{p.name}</b> <span className="muted">({p.id})</span>
                  </td>
                  <td>{p.operation.date}</td>
                  <td>
                    {p.operation.type}
                    <div className="muted">
                      {p.operation.startDate} ~ {p.operation.endDate}
                    </div>
                  </td>
                  <td>{p.operation.sla.level}</td>
                  <td>
                    {p.operation.manager.name} <span className="pms-tag">{p.operation.manager.role}</span>
                  </td>
                  <td>
                    기능 {p.operation.features.length} · 산출물 {p.operation.docs.length} · 이슈 {p.operation.carriedIssues.length}
                  </td>
                  <td>
                    <button className="link-btn" onClick={() => navigate(`/workspace/PMS/ops-project?project=${p.id}`)}>
                      운영 프로젝트 관리 ›
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
