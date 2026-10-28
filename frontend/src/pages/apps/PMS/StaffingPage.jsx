import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Modal from '../../../components/Modal'
import './pms.css'
import {
  idleRatio,
  overLimitRatio,
  setStaffingSettings,
  useStaffingSettings,
  actualOf,
  addMonths,
  collectAssignments,
  monthlyPlan,
  monthsOf,
  personMonthly,
  setActual,
  sumActual,
  useActuals,
} from './assignmentStore'
import { useAuth } from '../../../context/AuthContext'
import { CHANGE_STATUSES, computeConflicts, decideChange, useChanges } from './changeStore'
import { canApprove as hasApproveRole } from './participation'
import { usePmsUsers } from './pmsUsers'
import { ME, MEMBERS, TODAY } from './pmsStore'
import { useProjects } from './projectStore'
import { isPartner, orgOf } from './teamConfig'

const NOW = TODAY.slice(0, 7)
const mLabel = (k) => `${k.slice(2, 4)}.${k.slice(5, 7)}`
const pct = (r) => `${Math.round(r * 100)}%`
const phasesLabel = (p) => (p.length === 4 ? '전체' : p.join('·'))
const fmt1 = (n) => (Math.round(n * 10) / 10).toString()

/** 투입률에 따른 히트맵 색. */
function heat(rate) {
  if (!rate) return {}
  if (rate > overLimitRatio()) return { background: '#f6b8b2', color: '#7a1c14', fontWeight: 700 }
  if (rate >= 0.9) return { background: '#9fb8f6', color: '#1c2f73', fontWeight: 600 }
  if (rate >= 0.5) return { background: '#cddbfb', color: '#2a3c7d' }
  return { background: '#e9effd', color: '#3b4a85' }
}

/* ---------- 인력 상세(월별 계획/실적) ---------- */
function PersonDetail({ name, assignments, onClose }) {
  const actuals = useActuals()
  const navigate = useNavigate()
  const mine = assignments.filter((a) => a.name === name)
  const months = [...new Set(mine.flatMap((a) => monthsOf(a.from, a.to)))].sort()
  const total = personMonthly(assignments, name)

  return (
    <Modal title={`${name} 투입 상세`} onClose={onClose} width={1100}>
      <p className="muted" style={{ marginTop: 0 }}>
        {MEMBERS.find((m) => m.name === name)?.role} · {orgOf(name)}
        {isPartner(name) && ' · 협력사 인력은 내 업무 알림으로 진척·이슈를 처리합니다'}
      </p>
      {mine.length === 0 && <p className="muted">투입 중인 프로젝트가 없습니다.</p>}
      {mine.map((a) => {
        const plan = monthlyPlan(a)
        const planSum = Object.values(plan).reduce((s, v) => s + v, 0)
        const act = sumActual(actuals, a.projectId, a.name)
        return (
          <div key={a.key} className="pms-panel" style={{ boxShadow: 'none', border: '1px solid #e4e5ea' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 10, flexWrap: 'wrap' }}>
              <div>
                <b>{a.projectName}</b> <span className="muted">({a.projectId})</span>{' '}
                <span className="pms-tag doing">{a.role}</span> {a.isManager && <span className="pms-tag done">관리자 겸임</span>}
                <div className="muted">
                  투입 단계 {phasesLabel(a.phases)} · {a.from} ~ {a.to} · 계획 M/M {Number(a.mm) > 0 ? a.mm : '미입력(100% 가정)'}
                </div>
              </div>
              <button className="link-btn" onClick={() => navigate(`/workspace/PMS/${a.kind === '운영' ? 'ops-project' : 'exec-project'}?project=${a.baseProjectId}`)}>
                {a.kind === '운영' ? '운영 프로젝트 관리' : '프로젝트 관리'}에서 수정 ›
              </button>
            </div>
            <div style={{ overflowX: 'auto', marginTop: 8 }}>
              <table className="data-table pms-wbs">
                <thead>
                  <tr>
                    <th></th>
                    {monthsOf(a.from, a.to).map((k) => (
                      <th key={k} style={{ textAlign: 'center' }}>
                        {mLabel(k)}
                      </th>
                    ))}
                    <th style={{ textAlign: 'right' }}>합계</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>
                      <b>계획 M/M</b>
                    </td>
                    {monthsOf(a.from, a.to).map((k) => (
                      <td key={k} style={{ textAlign: 'center', ...heat(plan[k]) }}>
                        {plan[k]?.toFixed(2)}
                      </td>
                    ))}
                    <td style={{ textAlign: 'right' }}>
                      <b>{fmt1(planSum)}</b>
                    </td>
                  </tr>
                  <tr>
                    <td>
                      <b>실적 M/M</b>
                    </td>
                    {monthsOf(a.from, a.to).map((k) => (
                      <td key={k} style={{ textAlign: 'center' }}>
                        {k > NOW ? (
                          <span className="muted">-</span>
                        ) : a.managerName !== ME ? (
                          // 실적 M/M은 프로젝트 관리자만 입력한다(그 외에는 조회만)
                          <span title={`프로젝트 관리자(${a.managerName})만 입력할 수 있습니다`}>{actualOf(actuals, a.projectId, a.name, k) ?? '-'}</span>
                        ) : (
                          <input
                            type="number"
                            min="0"
                            step="0.1"
                            className="pms-cell-input pms-md"
                            style={{ width: 62 }}
                            value={actualOf(actuals, a.projectId, a.name, k) ?? ''}
                            onChange={(e) => setActual(a.projectId, a.name, k, e.target.value)}
                          />
                        )}
                      </td>
                    ))}
                    <td style={{ textAlign: 'right' }}>
                      <b>{fmt1(act)}</b>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        )
      })}
      {mine.length > 1 && (
        <div className="pms-hint">
          <b>월별 투입률 합계</b> —{' '}
          {months.map((k) => (
            <span key={k} style={{ marginRight: 10, ...(total[k] > overLimitRatio() ? { color: '#b3261e', fontWeight: 700 } : {}) }}>
              {mLabel(k)} {pct(total[k] ?? 0)}
            </span>
          ))}
        </div>
      )}
    </Modal>
  )
}

/* ---------- 탭 1: 인력별 월 투입률 ---------- */
function PersonTab({ assignments }) {
  const [start, setStart] = useState('2026-09')
  const [detail, setDetail] = useState(null)
  const months = Array.from({ length: 12 }, (_, i) => addMonths(start, i))

  const rows = MEMBERS.map((m) => {
    const monthly = personMonthly(assignments, m.name)
    const mine = assignments.filter((a) => a.name === m.name)
    const totalMm = mine.reduce((s, a) => s + (Number(a.mm) || 0), 0)
    const over = Object.entries(monthly).filter(([, v]) => v > overLimitRatio())
    return { ...m, monthly, mine, totalMm, over }
  })
  const conflicts = rows.flatMap((r) =>
    r.over.map(([k, v]) => ({
      name: r.name,
      month: k,
      rate: v,
      projects: r.mine.filter((a) => monthlyPlan(a)[k]).map((a) => a.projectName),
    })),
  )
  const active = rows.filter((r) => r.mine.length > 0)
  const idle = rows.filter((r) => (r.monthly[NOW] ?? 0) < idleRatio())

  return (
    <div>
      <div className="pms-summary">
        <div className="pms-stat">
          <div className="label">투입 인원 (수행 중 프로젝트)</div>
          <div className="value">{active.length}</div>
          <div className="muted">협력사 {active.filter((r) => isPartner(r.name)).length}명 포함</div>
        </div>
        <div className="pms-stat">
          <div className="label">투입 M/M 합계 (계획)</div>
          <div className="value">{fmt1(rows.reduce((s, r) => s + r.totalMm, 0))}</div>
        </div>
        <div className={`pms-stat ${conflicts.length ? 'danger' : 'ok'}`}>
          <div className="label">과투입 (월 투입률 {pct(overLimitRatio())} 초과)</div>
          <div className="value">{new Set(conflicts.map((c) => c.name)).size}명</div>
          <div className="muted">{conflicts.length}개 월 건</div>
        </div>
        <div className="pms-stat warn">
          <div className="label">{NOW.slice(5)}월 여유 인력 (투입률 {Math.round(idleRatio() * 100)}% 미만)</div>
          <div className="value">{idle.length}명</div>
        </div>
      </div>

      {conflicts.length > 0 && (
        <div className="pms-blind" style={{ background: '#fdecea', borderColor: '#f3c1bc', color: '#7a1c14' }}>
          ⚠ <b>과투입 경고</b> —{' '}
          {conflicts.slice(0, 6).map((c) => (
            <span key={`${c.name}${c.month}`} style={{ marginRight: 12 }}>
              {c.name} {mLabel(c.month)} <b>{pct(c.rate)}</b> ({c.projects.join(' + ')})
            </span>
          ))}
          {conflicts.length > 6 && `외 ${conflicts.length - 6}건`}
        </div>
      )}

      <div className="pms-toolbar">
        <button className="secondary small" onClick={() => setStart(addMonths(start, -3))}>
          ◀ 3개월
        </button>
        <b>
          {start} ~ {months[11]}
        </b>
        <button className="secondary small" onClick={() => setStart(addMonths(start, 3))}>
          3개월 ▶
        </button>
        <span className="muted" style={{ marginLeft: 12 }}>
          셀 = 월 투입률 (프로젝트 투입 계획 합계, 100% = 1.0 M/M)
        </span>
        <span style={{ marginLeft: 'auto', fontSize: 12 }}>
          <span className="pms-legend" style={heat(0.3)}>~49%</span> <span className="pms-legend" style={heat(0.6)}>50~89%</span>{' '}
          <span className="pms-legend" style={heat(1)}>90~105%</span> <span className="pms-legend" style={heat(1.5)}>초과</span>
        </span>
      </div>

      <div style={{ overflowX: 'auto' }}>
        <table className="data-table pms-wbs">
          <thead>
            <tr>
              <th>인력</th>
              <th>직무 / 소속</th>
              <th>프로젝트</th>
              <th style={{ textAlign: 'right' }}>M/M</th>
              {months.map((k) => (
                <th key={k} style={{ textAlign: 'center', minWidth: 52, ...(k === NOW ? { background: '#dfe5fb' } : {}) }}>
                  {mLabel(k)}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.name} style={{ cursor: 'pointer' }} onClick={() => setDetail(r.name)}>
                <td>
                  <b>{r.name}</b> {r.over.length > 0 && <span className="pms-tag late">과투입</span>}
                </td>
                <td>
                  {r.role} · {orgOf(r.name)}
                </td>
                <td>{r.mine.length ? r.mine.map((a) => a.projectName).join(', ') : <span className="muted">투입 없음</span>}</td>
                <td style={{ textAlign: 'right' }}>{r.totalMm ? fmt1(r.totalMm) : '-'}</td>
                {months.map((k) => (
                  <td key={k} style={{ textAlign: 'center', ...heat(r.monthly[k]) }}>
                    {r.monthly[k] ? pct(r.monthly[k]) : ''}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="pms-field-hint">행을 누르면 프로젝트별 월 계획과 실적 M/M 입력 화면이 열립니다. 투입 계획(역할·단계·M/M·기간)은 수행 단계의 「프로젝트 관리」에서 수정합니다.</div>

      {detail && <PersonDetail name={detail} assignments={assignments} onClose={() => setDetail(null)} />}
    </div>
  )
}

/* ---------- 탭 2: 프로젝트별 계획 대 실적 ---------- */
function ProjectTab({ projects, assignments }) {
  const actuals = useActuals()
  const navigate = useNavigate()
  // 투입이 있는 (수행/운영) 프로젝트 목록 — 운영 투입은 `프로젝트ID#OPS`로 구분된다.
  const list = [...new Map(assignments.map((a) => [a.projectId, { id: a.projectId, name: a.projectName, kind: a.kind, base: a.baseProjectId }])).values()]
  const [projectId, setProjectId] = useState(list[0]?.id ?? '')
  const sel = list.find((p) => p.id === projectId) ?? list[0]
  if (!sel) return <p className="muted">투입 중인 프로젝트가 없습니다.</p>
  const baseProject = projects.find((p) => p.id === sel.base)
  const mgr = sel.kind === '운영' ? baseProject?.operation?.manager : baseProject?.transitioned?.manager
  const project = { id: sel.id, base: sel.base, kind: sel.kind, start: baseProject?.start, end: baseProject?.end, manager: mgr }

  const mine = assignments.filter((a) => a.projectId === project.id)
  const planTotal = mine.reduce((s, a) => s + Object.values(monthlyPlan(a)).reduce((x, v) => x + v, 0), 0)
  const actTotal = mine.reduce((s, a) => s + sumActual(actuals, a.projectId, a.name), 0)
  const mmTotal = mine.reduce((s, a) => s + (Number(a.mm) || 0), 0)

  return (
    <div>
      <div className="pms-toolbar">
        <select value={project.id} onChange={(e) => setProjectId(e.target.value)}>
          {list.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
              {p.kind === '수행' ? ` (${p.id})` : ''}
            </option>
          ))}
        </select>
        <span className="muted">
          {project.kind} 단계 · 관리자 {project.manager?.name} ({project.manager?.role}) · {project.start} ~ {project.end}
        </span>
        <button className="link-btn" style={{ marginLeft: 'auto' }} onClick={() => navigate(`/workspace/PMS/${project.kind === '운영' ? 'ops-project' : 'exec-project'}?project=${project.base}`)}>
          투입 인력 수정 ({project.kind === '운영' ? '운영 ' : ''}프로젝트 관리) ›
        </button>
      </div>

      <div className="pms-summary">
        <div className="pms-stat">
          <div className="label">투입 인원</div>
          <div className="value">{mine.length}</div>
          <div className="muted">협력사 {mine.filter((a) => isPartner(a.name)).length}명</div>
        </div>
        <div className="pms-stat">
          <div className="label">계획 M/M (입력 합계)</div>
          <div className="value">{fmt1(mmTotal)}</div>
        </div>
        <div className="pms-stat">
          <div className="label">실적 M/M (누적)</div>
          <div className="value">{fmt1(actTotal)}</div>
        </div>
        <div className="pms-stat ok">
          <div className="label">소진율 (실적 ÷ 계획)</div>
          <div className="value">{mmTotal ? `${Math.round((actTotal / mmTotal) * 100)}%` : '-'}</div>
        </div>
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>인력</th>
            <th>역할</th>
            <th>투입 단계</th>
            <th>투입 기간</th>
            <th style={{ textAlign: 'right' }}>계획 M/M</th>
            <th style={{ textAlign: 'right' }}>실적 M/M</th>
            <th>소진율</th>
            <th>다른 프로젝트 겹침</th>
          </tr>
        </thead>
        <tbody>
          {mine.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={8}>
                투입 인력이 없습니다.
              </td>
            </tr>
          )}
          {mine.map((a) => {
            const act = sumActual(actuals, a.projectId, a.name)
            const rate = Number(a.mm) > 0 ? act / Number(a.mm) : null
            const monthly = personMonthly(assignments, a.name)
            const over = Object.keys(monthlyPlan(a)).filter((k) => monthly[k] > overLimitRatio())
            return (
              <tr key={a.key}>
                <td>
                  <b>{a.name}</b> {isPartner(a.name) && <span className="pms-tag manual">협력</span>} {a.isManager && <span className="pms-tag done">관리자</span>}
                </td>
                <td>{a.role}</td>
                <td>{phasesLabel(a.phases)}</td>
                <td>
                  {a.from} ~ {a.to}
                </td>
                <td style={{ textAlign: 'right' }}>{Number(a.mm) > 0 ? a.mm : <span className="muted">미입력</span>}</td>
                <td style={{ textAlign: 'right' }}>{fmt1(act)}</td>
                <td>
                  {rate === null ? (
                    '-'
                  ) : (
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      <div className="pms-bar" style={{ width: 70 }}>
                        <span style={{ width: `${Math.min(100, Math.round(rate * 100))}%` }} />
                      </div>
                      {Math.round(rate * 100)}%
                    </div>
                  )}
                </td>
                <td>{over.length ? <span className="pms-tag late">과투입 {over.map(mLabel).join(', ')}</span> : <span className="muted">-</span>}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
      <div className="pms-field-hint">
        월별 실적 M/M은 「인력별」 탭에서 인력을 눌러 입력합니다. 월별 계획 투입률 합계 대비 {mine.length ? fmt1(planTotal) : 0} M/M이 이 프로젝트 계획 환산값입니다.
      </div>
    </div>
  )
}

/* ---------- 탭 3: 가용 인력 조회 ---------- */
function AvailableTab({ assignments }) {
  const [from, setFrom] = useState('2026-11-01')
  const [to, setTo] = useState('2027-02-28')
  const [need, setNeed] = useState(50)
  const [role, setRole] = useState('')

  const months = monthsOf(from, to)
  const rows = MEMBERS.filter((m) => !role || m.role === role)
    .map((m) => {
      const monthly = personMonthly(assignments, m.name)
      const peak = Math.max(0, ...months.map((k) => monthly[k] ?? 0))
      const free = Math.max(0, 1 - peak)
      return { ...m, monthly, peak, free, ok: free * 100 >= need }
    })
    .sort((a, b) => b.free - a.free)

  return (
    <div>
      <div className="search-bar">
        <div className="search-field">
          <label>투입 시작</label>
          <input type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
        </div>
        <div className="search-field">
          <label>투입 종료</label>
          <input type="date" value={to} onChange={(e) => setTo(e.target.value)} />
        </div>
        <div className="search-field">
          <label>필요 투입률 (%)</label>
          <input type="number" min="10" max="100" step="10" value={need} onChange={(e) => setNeed(Number(e.target.value))} />
        </div>
        <div className="search-field">
          <label>직무</label>
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="">전체</option>
            {[...new Set(MEMBERS.map((m) => m.role))].map((r) => (
              <option key={r}>{r}</option>
            ))}
          </select>
        </div>
      </div>
      <p className="muted" style={{ marginTop: 0 }}>
        지정한 기간 안에서 현재 투입 계획 기준으로 <b>여유 투입률이 가장 작은 달</b>을 기준으로 투입 가능 여부를 판단합니다(제안·수주 단계의 투입 인력 검토용).
      </p>
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>인력</th>
            <th>직무 / 소속</th>
            <th>기간 내 최대 투입률</th>
            <th>여유 투입률</th>
            <th>판정</th>
            <th>해당 기간 투입 프로젝트</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.name}>
              <td>
                <b>{r.name}</b>
              </td>
              <td>
                {r.role} · {orgOf(r.name)}
              </td>
              <td style={heat(r.peak)}>{pct(r.peak)}</td>
              <td>
                <b>{pct(r.free)}</b>
              </td>
              <td>{r.ok ? <span className="pms-tag done">투입 가능</span> : <span className="pms-tag late">여유 부족</span>}</td>
              <td className="name-cell">
                {assignments
                  .filter((a) => a.name === r.name && months.some((k) => monthlyPlan(a)[k]))
                  .map((a) => a.projectName)
                  .join(', ') || <span className="muted">-</span>}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

/* ---------- 탭 4: 변경 요청 · 이력 (승인 관리) ---------- */
function ChangesTab({ projects }) {
  const changes = useChanges()
  const [status, setStatus] = useState('')
  const { auth } = useAuth()
  const users = usePmsUsers()
  const me = users.find((x) => x.userId === auth?.userId)?.userName ?? ''
  const canApprove = hasApproveRole(auth) // 승인 권한(PMS_APPROVE·admin) — 서버도 같은 권한을 검사, 요청자 본인은 불가
  const list = changes.filter((c) => !status || c.status === status)

  const decide = (c, result) => {
    if (me && me === c.requester) return window.alert('요청자 본인은 승인/반려할 수 없습니다. 다른 승인자가 처리해야 합니다.')
    const comment = window.prompt(result === '승인' ? '승인 의견(선택)' : '반려 사유를 입력하세요.', '')
    if (comment === null) return
    if (result === '반려' && !comment.trim()) return window.alert('반려 사유를 입력하세요.')
    decideChange(c.id, result, { comment: comment.trim() }).catch((e) => window.alert(e.message))
  }

  return (
    <div>
      <div className="pms-blind">
        투입 계획(프로젝트 관리자·투입 인력의 역할·투입 단계·M/M·기간)을 바꾸려면 프로젝트 관리자가 <b>변경 요청</b>(사유 필수)을 올리고, <b>인력 관리자가 승인</b>하면 반영됩니다. 요청·승인·반려는
        모두 이력으로 남고, 승인 시 예상되는 <b>과투입</b>을 함께 보여줍니다.
      </div>
      <div className="pms-toolbar">
        <div className="pms-chips" style={{ margin: 0 }}>
          <button className={!status ? 'on' : ''} onClick={() => setStatus('')}>
            전체 <span>{changes.length}</span>
          </button>
          {CHANGE_STATUSES.map((s) => (
            <button key={s} className={status === s ? 'on' : ''} onClick={() => setStatus(s)}>
              {s} <span>{changes.filter((c) => c.status === s).length}</span>
            </button>
          ))}
        </div>
        <span className="muted" style={{ marginLeft: 'auto' }}>
          {canApprove ? '승인자 권한(센터장·admin)으로 로그인되어 있습니다' : '승인·반려는 센터장·admin만 할 수 있습니다'}
        </span>
      </div>
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>요청 번호</th>
            <th>프로젝트</th>
            <th>요청자 / 일자</th>
            <th>사유</th>
            <th>변경 내용</th>
            <th>승인 시 과투입 예상</th>
            <th>상태 / 처리</th>
          </tr>
        </thead>
        <tbody>
          {list.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={7}>
                변경 요청이 없습니다.
              </td>
            </tr>
          )}
          {list.map((c) => {
            const conflicts = c.status === '승인 대기' ? computeConflicts(projects, c.projectId, c.after) : []
            return (
              <tr key={c.id}>
                <td>
                  <b>{c.id}</b>
                </td>
                <td className="name-cell">{c.projectName}</td>
                <td>
                  {c.requester}
                  <div className="muted">{c.requestedAt}</div>
                </td>
                <td className="name-cell" style={{ maxWidth: 220 }}>
                  {c.reason}
                </td>
                <td className="name-cell" style={{ maxWidth: 320 }}>
                  {c.summary.map((s) => (
                    <div key={s} style={{ fontSize: 12 }}>
                      • {s}
                    </div>
                  ))}
                </td>
                <td>
                  {c.status !== '승인 대기' ? (
                    <span className="muted">-</span>
                  ) : conflicts.length === 0 ? (
                    <span className="pms-tag done">없음</span>
                  ) : (
                    conflicts.slice(0, 4).map((x) => (
                      <div key={`${x.name}${x.month}`}>
                        <span className="pms-tag late">
                          {x.name} {mLabel(x.month)} {pct(x.rate)}
                        </span>
                      </div>
                    ))
                  )}
                </td>
                <td>
                  <span className={`pms-tag ${{ '승인 대기': 'manual', 승인: 'done', 반려: 'late', 취소: '' }[c.status]}`}>{c.status}</span>
                  {c.status === '승인 대기' ? (
                    <div style={{ marginTop: 6 }}>
                      {!canApprove && <span className="muted">승인 대기 중</span>}
                      {canApprove && (
                        <>
                          <button className="primary small" style={{ marginRight: 6 }} onClick={() => decide(c, '승인')}>
                            승인
                          </button>
                          <button className="secondary small" onClick={() => decide(c, '반려')}>
                            반려
                          </button>
                        </>
                      )}
                    </div>
                  ) : (
                    <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
                      {c.decidedBy} · {c.decidedAt}
                      {c.comment && <div>“{c.comment}”</div>}
                    </div>
                  )}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

/* ---------- 설정 ---------- */
function SettingsModal({ onClose }) {
  const s = useStaffingSettings()
  return (
    <Modal title="투입 관리 설정" onClose={onClose} width={520}>
      <p className="muted" style={{ marginTop: 0 }}>
        회사 기준에 맞게 변경합니다. 변경 즉시 인력별 현황·과투입 경고·가용 인력 조회에 반영됩니다.
      </p>
      <div className="field">
        <label>
          과투입 기준 — 월 투입률(%) 초과<span className="req-mark"> *</span>
        </label>
        <input type="number" min="100" max="200" step="5" value={s.overLimit} onChange={(e) => setStaffingSettings({ overLimit: Math.max(100, Number(e.target.value) || 100) })} />
        <div className="pms-field-hint">예) 100 = 100% 초과 즉시 경고, 120 = 최대 120%까지 허용. 기본 105.</div>
      </div>
      <div className="field">
        <label>
          여유 인력 기준 — 월 투입률(%) 미만<span className="req-mark"> *</span>
        </label>
        <input type="number" min="0" max="100" step="5" value={s.idleLimit} onChange={(e) => setStaffingSettings({ idleLimit: Math.min(100, Math.max(0, Number(e.target.value) || 0)) })} />
        <div className="pms-field-hint">당월 투입률이 이 값 미만이면 여유 인력으로 집계합니다. 기본 50.</div>
      </div>
      <button className="primary" onClick={onClose}>
        닫기
      </button>
    </Modal>
  )
}

/**
 * 투입 관리(인력관리) — 프로젝트의 수행 관리 정보(투입 인력 = 프로젝트 멤버)를 인력 기준으로 모아
 * 월별 투입률·과투입·가용 인력을 보고, 월별 실적 M/M을 관리한다. 투입 계획의 수정은 수행 단계 「프로젝트 관리」에서 한다.
 */
export default function StaffingPage() {
  const projects = useProjects()
  useStaffingSettings() // 설정이 바뀌면 현황·경고도 다시 계산
  const changes = useChanges()
  const [tab, setTab] = useState('person')
  const [settings, setSettings] = useState(false)
  const assignments = collectAssignments(projects)
  const pending = changes.filter((c) => c.status === '승인 대기').length

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            투입 관리 <span className="pms-mock-badge">목업 · 더미 데이터</span>
          </h2>
          <p className="page-description">
            수행·운영 중인 프로젝트의 투입 계획(역할 · 투입 단계 · M/M · 기간)을 인력 기준으로 모아 보고, 월별 투입률과 과투입, 투입 가능 인력을 확인하며 실적 M/M을
            관리합니다. 실적 M/M은 프로젝트 관리자가 입력하고, 계획 변경은 요청 → 승인으로 처리합니다.
          </p>
        </div>
        <button className="secondary small" onClick={() => setSettings(true)}>
          ⚙ 설정
        </button>
      </div>
      {settings && <SettingsModal onClose={() => setSettings(false)} />}
      <div className="tabs-bar">
        {[
          ['person', '인력별 투입 현황'],
          ['project', '프로젝트별 계획 · 실적'],
          ['available', '가용 인력 조회'],
          ['changes', `변경 요청 · 이력${pending ? ` (대기 ${pending})` : ''}`],
        ].map(([k, label]) => (
          <button key={k} className={`tab-btn ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>
            {label}
          </button>
        ))}
      </div>
      <div className="tab-content">
        {tab === 'person' && <PersonTab assignments={assignments} />}
        {tab === 'project' && <ProjectTab projects={projects} assignments={assignments} />}
        {tab === 'available' && <AvailableTab assignments={assignments} />}
        {tab === 'changes' && <ChangesTab projects={projects} />}
      </div>
    </div>
  )
}
