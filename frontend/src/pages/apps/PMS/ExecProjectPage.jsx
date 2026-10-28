import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import Modal from '../../../components/Modal'
import './pms.css'
import { cancelChange, computeConflicts, diffPlan, pendingOf, submitChange, useChanges } from './changeStore'
import { DEFAULT_PROJECT_ID } from './docStore'
import { isOpenStatus, useIssues } from './issueStore'
import { ProcessButton } from './ProcessModal'
import { changeProcess } from './processModels'
import TeamEditor from './TeamEditor'
import { CHAIN, ME, TODAY, isLeaf, useTasks } from './pmsStore'
import { endExecution, formatKoreanWon, useProjects } from './projectStore'
import { useRfps } from './rfpStore'
import { isPartner } from './teamConfig'

function Kv({ k, children, wide }) {
  return (
    <div className="kv-row" style={wide ? { gridColumn: '1 / -1' } : undefined}>
      <span className="k">{k}</span>
      <span style={{ textAlign: 'right' }}>{children || '-'}</span>
    </div>
  )
}

const CHANGE_CLASS = { '승인 대기': 'manual', 승인: 'done', 반려: 'late', 취소: '' }

/** 투입 계획 변경 요청 등록 모달 — 변경 내용·과투입 예상을 보여주고 사유(필수)를 받는다. */
function ChangeRequestModal({ project, after, onClose, onSubmitted }) {
  const projects = useProjects()
  const [reason, setReason] = useState('')
  const [error, setError] = useState('')
  const exec = project.transitioned
  const summary = diffPlan({ manager: exec.manager, team: exec.team, restricted: !!exec.restricted }, after)
  const conflicts = computeConflicts(projects, project.id, after)

  return (
    <Modal title="투입 계획 변경 요청" onClose={onClose} width={640}>
      <p style={{ marginTop: 0, fontSize: 13 }}>
        투입 계획은 <b>인력 관리자의 승인 후</b> 반영됩니다. 승인 전까지는 기존 계획이 유지됩니다.
      </p>
      <div className="pms-form-section" style={{ marginTop: 6 }}>변경 내용</div>
      {summary.length === 0 ? <p className="muted">변경 내용이 없습니다.</p> : summary.map((s) => <div key={s} style={{ fontSize: 13 }}>• {s}</div>)}
      <div className="pms-form-section">승인 시 과투입 예상</div>
      {conflicts.length === 0 ? (
        <span className="pms-tag done">없음</span>
      ) : (
        conflicts.map((c) => (
          <span key={`${c.name}${c.month}`} className="pms-tag late" style={{ marginRight: 6 }}>
            {c.name} {c.month.slice(2, 4)}.{c.month.slice(5, 7)} {Math.round(c.rate * 100)}%
          </span>
        ))
      )}
      <div className="field" style={{ marginTop: 14 }}>
        <label>
          변경 사유<span className="req-mark"> *</span>
        </label>
        <textarea rows={3} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="예) 개발 일정 지연 대응을 위한 인력 추가" />
        {error && <div className="pms-field-error">{error}</div>}
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <button
          className="primary"
          style={{ flex: 1 }}
          disabled={summary.length === 0}
          onClick={() => {
            if (!reason.trim()) return setError('변경 사유를 입력하세요.')
            submitChange({ project, after, reason: reason.trim() })
              .then(() => {
                onSubmitted()
                onClose()
              })
              .catch((e) => setError(e.message))
          }}
        >
          변경 요청 등록
        </button>
        <button className="secondary" style={{ marginTop: 0 }} onClick={onClose}>
          취소
        </button>
      </div>
    </Modal>
  )
}

/** 수행 종료 처리 모달 — 준공/검수 완료를 확정하고 운영단계 인수 대상 기능 목록을 남긴다. */
function EndExecutionModal({ project, defaultFeatures, openTasks, openIssues, onClose }) {
  const [date, setDate] = useState(TODAY)
  const [memo, setMemo] = useState('')
  const [featureText, setFeatureText] = useState(defaultFeatures.join('\n'))
  return (
    <Modal title="수행 종료 처리 (준공/검수 완료)" onClose={onClose} width={620}>
      <p style={{ marginTop: 0, fontSize: 13 }}>
        수행 종료로 처리하면 프로젝트 상태가 <b>수행 종료</b>가 되고 운영단계전환 대상이 됩니다. 수행 정보는 읽기 전용이 됩니다.
      </p>
      {(openTasks > 0 || openIssues > 0) && (
        <div className="pms-field-error" style={{ marginBottom: 8 }}>
          ⚠ 미완료 WBS 업무 {openTasks}건, 미해결 이슈 {openIssues}건이 있습니다. 운영단계전환에서 미해결 이슈를 이관할 수 있습니다.
        </div>
      )}
      <div className="field">
        <label>
          수행 종료일(준공/검수일)<span className="req-mark"> *</span>
        </label>
        <input type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      </div>
      <div className="field">
        <label>
          운영 인수 대상 기능 <span className="opt-mark">(한 줄에 하나, WBS/RFP에서 자동 구성)</span>
        </label>
        <textarea rows={5} value={featureText} onChange={(e) => setFeatureText(e.target.value)} />
      </div>
      <div className="field">
        <label>
          비고<span className="opt-mark"> (옵션)</span>
        </label>
        <input value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="예) 최종 검수 완료" />
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <button
          className="primary"
          style={{ flex: 1 }}
          onClick={() => {
            if (!date) return window.alert('수행 종료일을 입력하세요.')
            endExecution(project.id, {
              date,
              memo: memo.trim(),
              features: featureText.split('\n').map((s) => s.trim()).filter(Boolean),
              openIssues,
            })
            onClose()
          }}
        >
          수행 종료 처리
        </button>
        <button className="secondary" style={{ marginTop: 0 }} onClick={onClose}>
          취소
        </button>
      </div>
    </Modal>
  )
}

function ExecProject({ project }) {
  const navigate = useNavigate()
  const rfps = useRfps()
  const allTasks = useTasks()
  const issues = useIssues()
  const changes = useChanges()
  const exec = project.transitioned
  const [manager, setManager] = useState(exec.manager)
  const [team, setTeam] = useState(exec.team)
  const [restricted, setRestricted] = useState(!!exec.restricted)
  const [requestOpen, setRequestOpen] = useState(false)
  const [endOpen, setEndOpen] = useState(false)
  const [sent, setSent] = useState(false)

  // 수행 단계의 프로젝트 관리는 단계전환에서 지정한 프로젝트 관리자가 한다. 승인 대기 중이거나 수행 종료 후에는 수정할 수 없다.
  const isManager = exec.manager.name === ME
  const running = project.status === '수행 중'
  const pending = pendingOf(changes, project.id)
  const canEdit = isManager && running && !pending
  const dirty = JSON.stringify({ manager, team, restricted }) !== JSON.stringify({ manager: exec.manager, team: exec.team, restricted: !!exec.restricted })
  const rfp = rfps.find((r) => r.id === exec.rfpId)
  const partners = team.filter((m) => isPartner(m.name))
  const totalMm = team.reduce((s, m) => s + (Number(m.mm) || 0), 0)
  const history = changes.filter((c) => c.projectId === project.id)

  // 수행 종료용 집계: 미완료 WBS 업무, 미해결 이슈, 운영 인수 대상 기능(WBS 기능 → RFP 항목 순)
  const projTasks = allTasks.filter((t) => t.projectId === project.id)
  const projLeaves = projTasks.filter((t) => isLeaf(projTasks, t))
  const openTasks = projLeaves.filter((t) => (t.progress ?? 0) < 100).length
  const taskIds = new Set(projTasks.map((t) => t.id))
  const openIssues = issues.filter((i) => isOpenStatus(i.status) && i.projectId === project.id).length
  const defaultFeatures = [...new Set(projLeaves.filter((t) => t.feature).map((t) => t.feature))].length
    ? [...new Set(projLeaves.filter((t) => t.feature).map((t) => t.feature))]
    : (rfp?.items ?? []).filter((i) => i.category === 'SFR').map((i) => i.name)

  const requestChange = () => {
    if (!manager.name) return window.alert('프로젝트 관리자를 선택하세요.')
    if (team.some((m) => !m.name)) return window.alert('투입 인력의 이름을 모두 선택하세요.')
    if (team.some((m) => m.phases.length === 0)) return window.alert('투입 단계가 없는 투입 인력이 있습니다. 투입 단계를 선택하거나 삭제하세요.')
    if (team.some((m) => m.from && m.to && m.to < m.from)) return window.alert('투입 기간의 종료일이 시작일보다 빠른 인력이 있습니다.')
    setRequestOpen(true)
  }

  return (
    <div>
      <div className="pms-panel">
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
          <h3 style={{ margin: 0 }}>
            영업단계 기본 정보 <span className="pms-tag">수정 불가</span>
          </h3>
          <button className="link-btn" onClick={() => navigate('/workspace/PMS/projects')}>
            영업단계 프로젝트 관리에서 보기 ›
          </button>
        </div>
        <p className="muted" style={{ margin: '4px 0 10px' }}>
          수주까지의 정보입니다. 수정은 영업단계의 프로젝트 관리에서 합니다.
        </p>
        <div className="pms-form-grid" style={{ columnGap: 28 }}>
          <Kv k="프로젝트 코드">{project.id}</Kv>
          <Kv k="상태">
            <span className="pms-tag doing">{project.status}</span>
          </Kv>
          <Kv k="프로젝트명" wide>
            <b>{project.name}</b>
          </Kv>
          <Kv k="발주사">{project.client}</Kv>
          <Kv k="발주금액">{formatKoreanWon(project.amount)}</Kv>
          <Kv k="기간 (예정)">
            {project.start} ~ {project.end}
          </Kv>
          <Kv k="사업 구분">{project.businessType}</Kv>
          <Kv k="계약/입찰 방식">{project.bidType}</Kv>
          <Kv k="공고번호">{project.noticeNo}</Kv>
          <Kv k="발주사 담당자">{project.contactName && `${project.contactName}${project.contactTitle ? ` ${project.contactTitle}` : ''} · ${project.contactPhone}`}</Kv>
          <Kv k="영업 담당자">{project.salesName}</Kv>
          <Kv k="대응 RFP">{rfp ? `${rfp.id} · 항목 ${rfp.items.length}건 (전환 시 ${exec.itemCount}건 선택)` : 'RFP 없음'}</Kv>
          <Kv k="비고" wide>
            {project.memo}
          </Kv>
        </div>
      </div>

      <div className="pms-panel">
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <h3 style={{ margin: 0 }}>
            수행 관리 정보 <span className="pms-tag done">단계전환에서 지정</span>
          </h3>
          <span className="muted">
            단계전환 {exec.date} · 단계 {exec.phases.join(' → ')}
          </span>
        </div>
        {!running && (
          <div className="pms-blind" style={{ marginTop: 10 }}>
            🏁 수행이 종료된 프로젝트입니다({project.completed?.date}). 수행 관리 정보는 읽기 전용이며, 운영 단계는 <b>운영단계전환</b>에서 이어집니다.
          </div>
        )}
        {running && !isManager && (
          <div className="pms-blind" style={{ marginTop: 10 }}>
            🔒 이 프로젝트의 관리자는 <b>{exec.manager.name}</b> ({exec.manager.role}) 님입니다. 수행 관리 정보는 프로젝트 관리자만 변경 요청할 수 있습니다.
          </div>
        )}
        {running && pending && (
          <div className="pms-blind" style={{ background: '#fff4d6', borderColor: '#ecd48b', color: '#6b4d00', marginTop: 10 }}>
            ⏳ <b>변경 요청 {pending.id}</b>이(가) 승인 대기 중입니다({pending.requester}, {pending.requestedAt}). 승인 또는 반려될 때까지 수정할 수 없습니다.
            {pending.requester === ME && (
              <button className="link-btn" style={{ marginLeft: 12 }} onClick={() => cancelChange(pending.id).catch((e) => window.alert(e.message))}>
                요청 취소
              </button>
            )}
            <div style={{ fontSize: 12, marginTop: 4 }}>{pending.summary.join(' / ')}</div>
            <ProcessButton getModel={() => changeProcess(pending)} label="누구에게 있나?" />
          </div>
        )}
        <p className="muted" style={{ margin: '8px 0' }}>
          프로젝트 관리자와 투입 인력은 <b>프로젝트 멤버</b>와 같은 데이터이며, 이후 인력관리(투입관리)의 기준이 됩니다. 문서 테일러링 · WBS/진척 관리도 이 관리자가 관리합니다.
        </p>

        <TeamEditor
          manager={manager}
          setManager={setManager}
          team={team}
          setTeam={setTeam}
          phases={new Set(exec.phases)}
          period={{ start: project.start, end: project.end }}
          readOnly={!canEdit}
        />
        <label style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 10, fontSize: 13 }}>
          <input type="checkbox" disabled={!canEdit} checked={restricted} onChange={(e) => setRestricted(e.target.checked)} />
          멤버 외 열람 제한 <span className="muted">(켜면 프로젝트 관리자·투입 인력만 이 프로젝트의 WBS·이슈·보고서를 볼 수 있습니다. 센터장·admin은 예외)</span>
        </label>

        {canEdit && (
          <div style={{ display: 'flex', gap: 8, marginTop: 14, alignItems: 'center' }}>
            <button className="primary" style={{ width: 200 }} disabled={!dirty} onClick={requestChange}>
              변경 요청 (승인 필요)
            </button>
            <button
              className="secondary"
              style={{ marginTop: 0 }}
              disabled={!dirty}
              onClick={() => {
                setManager(exec.manager)
                setTeam(exec.team)
                setRestricted(!!exec.restricted)
              }}
            >
              되돌리기
            </button>
            {sent && <span className="pms-tag done">변경 요청을 등록했습니다 — 인력 관리자의 승인을 기다립니다</span>}
            {dirty && !sent && <span className="pms-tag manual">저장하지 않은 변경</span>}
          </div>
        )}        <div className="pms-field-hint" style={{ marginTop: 8 }}>
          투입 인력을 바꿔도 이미 생성된 WBS 업무의 담당자는 바뀌지 않습니다. WBS/진척 관리에서 업무별로 지정하거나 복사해 조정하세요.
        </div>
      </div>

      <div className="pms-summary">
        <div className="pms-stat">
          <div className="label">투입 인원</div>
          <div className="value">{team.length}</div>
          <div className="muted">자사 {team.length - partners.length} · 협력사 {partners.length}</div>
        </div>
        <div className="pms-stat">
          <div className="label">투입 M/M 합계</div>
          <div className="value">{totalMm || 0}</div>
        </div>
        {CHAIN.filter((p) => exec.phases.includes(p)).map((ph) => (
          <div key={ph} className="pms-stat">
            <div className="label">{ph} 단계 투입</div>
            <div className="value">{team.filter((m) => m.phases.includes(ph)).length}</div>
          </div>
        ))}
      </div>

      {partners.length > 0 && (
        <div className="pms-hint">
          <b>협력사 인력 — 내 업무 알림으로 진척·이슈 관리</b>
          <br />
          {partners.map((m) => `${m.name}(${m.notify || '미설정'})`).join(', ')} — 협력사 인력은 업무 알림(메신저)을 받고, 알림에서 진척률 입력과 이슈 등록을 합니다. 관리자가 WBS/진척
          관리와 이슈 관리에서 대신 입력할 수도 있습니다.
        </div>
      )}

      <div style={{ display: 'flex', gap: 8 }}>
        <button className="secondary small" style={{ marginTop: 0 }} onClick={() => navigate(`/workspace/PMS/doc-tailoring?project=${project.id}`)}>
          문서 테일러링 관리 ›
        </button>
        <button
          className="secondary small"
          style={{ marginTop: 0 }}
          disabled={!allTasks.some((t) => t.projectId === project.id)}
          title={allTasks.some((t) => t.projectId === project.id) ? '' : '이 프로젝트에는 아직 WBS가 없습니다'}
          onClick={() => navigate(`/workspace/PMS/wbs?project=${project.id}`)}
        >
          WBS / 진척 관리 ›
        </button>
        {running && isManager && (
          <button className="primary small" style={{ marginLeft: 'auto' }} onClick={() => setEndOpen(true)}>
            🏁 수행 종료 처리
          </button>
        )}
      </div>

      {/* 투입 계획 변경 이력 */}
      <div className="pms-panel" style={{ marginTop: 18 }}>
        <h3>
          투입 계획 변경 이력 <span className="count">{history.length}</span>
        </h3>
        <table className="data-table pms-wbs">
          <thead>
            <tr>
              <th>요청 번호</th>
              <th>요청자 / 일자</th>
              <th>사유</th>
              <th>변경 내용</th>
              <th>상태 / 처리</th>
            </tr>
          </thead>
          <tbody>
            {history.length === 0 && (
              <tr>
                <td className="empty-cell" colSpan={5}>
                  변경 이력이 없습니다. (단계전환 최초 지정은 승인 없이 반영됩니다)
                </td>
              </tr>
            )}
            {history.map((c) => (
              <tr key={c.id}>
                <td>
                  <b>{c.id}</b>
                </td>
                <td>
                  {c.requester}
                  <div className="muted">{c.requestedAt}</div>
                </td>
                <td className="name-cell">{c.reason}</td>
                <td className="name-cell">
                  {c.summary.map((s) => (
                    <div key={s} style={{ fontSize: 12 }}>
                      • {s}
                    </div>
                  ))}
                </td>
                <td>
                  <span className={`pms-tag ${CHANGE_CLASS[c.status]}`}>{c.status}</span>
                  {c.decidedBy && (
                    <div className="muted" style={{ fontSize: 12 }}>
                      {c.decidedBy} · {c.decidedAt}
                      {c.comment && <div>“{c.comment}”</div>}
                    </div>
                  )}
                  <ProcessButton getModel={() => changeProcess(c)} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {requestOpen && <ChangeRequestModal project={project} after={{ manager, team, restricted }} onClose={() => setRequestOpen(false)} onSubmitted={() => setSent(true)} />}
      {endOpen && (
        <EndExecutionModal project={project} defaultFeatures={defaultFeatures} openTasks={openTasks} openIssues={openIssues} onClose={() => setEndOpen(false)} />
      )}
    </div>
  )
}

/**
 * 수행 단계 「프로젝트 관리」 — 영업단계의 프로젝트 기본 정보(수정 불가) + 단계전환에서 지정한 수행 관리 정보(프로젝트 관리자·투입 인력 = 프로젝트 멤버, 수정 가능).
 * 단계전환에서 지정한 프로젝트 관리자가 수행 단계의 프로젝트 관리 + 문서 테일러링 + WBS/진척 관리를 맡는다.
 */
export default function ExecProjectPage() {
  const projects = useProjects()
  const [params] = useSearchParams()
  const list = projects.filter((p) => p.transitioned)
  const [projectId, setProjectId] = useState(
    () => list.find((p) => p.id === params.get('project'))?.id ?? list.find((p) => p.transitioned.manager.name === ME)?.id ?? list[0]?.id ?? '',
  )
  const project = list.find((p) => p.id === projectId)

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            프로젝트 관리 (수행) <span className="pms-mock-badge">목업 · 더미 데이터</span>
          </h2>
          <p className="page-description">
            수행 중인 프로젝트의 기본 정보와 수행 관리 정보를 봅니다. 영업단계 기본 정보는 수정할 수 없고, 단계전환에서 지정한 <b>프로젝트 관리자 · 투입 인력(프로젝트 멤버)</b>은
            프로젝트 관리자가 수정합니다.
          </p>
        </div>
      </div>

      {list.length === 0 ? (
        <p className="muted">수행 중인 프로젝트가 없습니다. 단계전환에서 수주 프로젝트를 전환하세요.</p>
      ) : (
        <>
          <div className="pms-toolbar">
            <select value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              {list.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.id} · {p.name} (관리자 {p.transitioned.manager.name})
                </option>
              ))}
            </select>
          </div>
          {/* 승인으로 투입 계획이 바뀌면 편집기를 새 값으로 다시 시작한다 */}
          {project && <ExecProject key={`${project.id}|${project.status}|${JSON.stringify(project.transitioned)}`} project={project} />}
        </>
      )}
    </div>
  )
}
