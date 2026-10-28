import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import './pms.css'
import { OPS_TYPES, SLA_LEVELS } from './opsConfig'
import { ME } from './pmsStore'
import { formatKoreanWon, updateOperation, useProjects } from './projectStore'
import TeamEditor from './TeamEditor'
import { OPS_MANAGER_ROLES, OPS_ROLES, isPartner } from './teamConfig'

function Kv({ k, children, wide }) {
  return (
    <div className="kv-row" style={wide ? { gridColumn: '1 / -1' } : undefined}>
      <span className="k">{k}</span>
      <span style={{ textAlign: 'right' }}>{children || '-'}</span>
    </div>
  )
}

function OpsProject({ project }) {
  const navigate = useNavigate()
  const op = project.operation
  const exec = project.transitioned
  const [form, setForm] = useState({ type: op.type, startDate: op.startDate, endDate: op.endDate, sla: op.sla, contractAmount: op.contractAmount })
  const [manager, setManager] = useState(op.manager)
  const [team, setTeam] = useState(op.team)
  const [saved, setSaved] = useState(false)

  // 운영 단계의 프로젝트 관리는 운영단계전환에서 지정한 운영 관리자가 한다.
  const canEdit = op.manager.name === ME && project.status === '운영 중'
  const dirty = JSON.stringify({ form, manager, team }) !== JSON.stringify({ form: { type: op.type, startDate: op.startDate, endDate: op.endDate, sla: op.sla, contractAmount: op.contractAmount }, manager: op.manager, team: op.team })
  const partners = team.filter((m) => isPartner(m.name))
  const set = (k, v) => setForm((p) => ({ ...p, [k]: v }))

  const save = () => {
    if (!form.startDate || !form.endDate || form.endDate < form.startDate) return window.alert('운영 기간을 확인하세요.')
    if (team.some((m) => !m.name)) return window.alert('운영 인력의 이름을 모두 선택하세요.')
    if (team.some((m) => m.from && m.to && m.to < m.from)) return window.alert('투입 기간의 종료일이 시작일보다 빠른 인력이 있습니다.')
    updateOperation(project.id, { ...form, manager, team })
    setSaved(true)
    setTimeout(() => setSaved(false), 2500)
  }

  return (
    <div>
      <div className="pms-panel">
        <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
          <h3 style={{ margin: 0 }}>
            영업·수행 단계 정보 <span className="pms-tag">수정 불가</span>
          </h3>
          <span>
            <button className="link-btn" onClick={() => navigate('/workspace/PMS/projects')}>
              영업단계 프로젝트 관리 ›
            </button>
            <button className="link-btn" onClick={() => navigate(`/workspace/PMS/exec-project?project=${project.id}`)}>
              수행 프로젝트 관리 ›
            </button>
          </span>
        </div>
        <div className="pms-form-grid" style={{ columnGap: 28, marginTop: 8 }}>
          <Kv k="프로젝트 코드">{project.id}</Kv>
          <Kv k="상태">
            <span className="pms-tag done">{project.status}</span>
          </Kv>
          <Kv k="프로젝트명" wide>
            <b>{project.name}</b>
          </Kv>
          <Kv k="발주사">{project.client}</Kv>
          <Kv k="발주금액">{formatKoreanWon(project.amount)}</Kv>
          <Kv k="수행 기간">
            {project.start} ~ {project.end}
          </Kv>
          <Kv k="수행 종료일">{project.completed?.date}</Kv>
          <Kv k="수행 관리자">{exec ? `${exec.manager.name} (${exec.manager.role})` : '-'}</Kv>
          <Kv k="수행 투입">{exec ? `${exec.team.length}명 · M/M ${exec.team.reduce((s, m) => s + (Number(m.mm) || 0), 0)}` : '-'}</Kv>
          <Kv k="발주사 담당자">{project.contactName && `${project.contactName}${project.contactTitle ? ` ${project.contactTitle}` : ''} · ${project.contactPhone}`}</Kv>
          <Kv k="영업 담당자">{project.salesName}</Kv>
        </div>
      </div>

      <div className="pms-panel">
        <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10, alignItems: 'center' }}>
          <h3 style={{ margin: 0 }}>
            운영 정보 <span className="pms-tag done">운영단계전환에서 지정</span>
          </h3>
          <span className="muted">운영단계전환 {op.date}</span>
        </div>
        {!canEdit && (
          <div className="pms-blind" style={{ marginTop: 10 }}>
            🔒 이 프로젝트의 운영 관리자는 <b>{op.manager.name}</b> ({op.manager.role}) 님입니다. 운영 정보는 운영 관리자만 수정할 수 있습니다.
          </div>
        )}

        <div className="pms-form-grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', marginTop: 10 }}>
          <div className="field pms-field">
            <label>운영 형태</label>
            <select disabled={!canEdit} value={form.type} onChange={(e) => set('type', e.target.value)}>
              {OPS_TYPES.map((t) => (
                <option key={t.value}>{t.value}</option>
              ))}
            </select>
          </div>
          <div className="field pms-field">
            <label>운영 시작일</label>
            <input type="date" disabled={!canEdit} value={form.startDate} onChange={(e) => set('startDate', e.target.value)} />
          </div>
          <div className="field pms-field">
            <label>운영 종료일</label>
            <input type="date" disabled={!canEdit} value={form.endDate} onChange={(e) => set('endDate', e.target.value)} />
          </div>
          <div className="field pms-field">
            <label>운영 계약금액 (원)</label>
            <input
              inputMode="numeric"
              disabled={!canEdit}
              value={form.contractAmount ? Number(form.contractAmount).toLocaleString() : ''}
              onChange={(e) => {
                const d = e.target.value.replace(/[^\d]/g, '')
                set('contractAmount', d === '' ? null : Number(d))
              }}
            />
          </div>
          <div className="field pms-field">
            <label>SLA 등급</label>
            <select
              disabled={!canEdit}
              value={form.sla.level}
              onChange={(e) => set('sla', { level: e.target.value, ...SLA_LEVELS[e.target.value] })}
            >
              {Object.keys(SLA_LEVELS).map((k) => (
                <option key={k}>{k}</option>
              ))}
            </select>
          </div>
          <div className="field pms-field">
            <label>장애 응답 / 해결 목표 (시간)</label>
            <div style={{ display: 'flex', gap: 6 }}>
              <input type="number" min="1" disabled={!canEdit} value={form.sla.respond} onChange={(e) => set('sla', { ...form.sla, respond: Number(e.target.value) })} />
              <input type="number" min="1" disabled={!canEdit} value={form.sla.resolve} onChange={(e) => set('sla', { ...form.sla, resolve: Number(e.target.value) })} />
            </div>
          </div>
          <div className="field pms-field">
            <label>서비스 시간</label>
            <input disabled={!canEdit} value={form.sla.hours} onChange={(e) => set('sla', { ...form.sla, hours: e.target.value })} />
          </div>
        </div>

        <TeamEditor
          manager={manager}
          setManager={setManager}
          team={team}
          setTeam={setTeam}
          phases={null}
          period={{ start: form.startDate, end: form.endDate }}
          managerRoles={OPS_MANAGER_ROLES}
          roles={OPS_ROLES}
          phaseList={['운영']}
          readOnly={!canEdit}
          managerHint="운영 단계의 프로젝트 관리(운영 정보·운영 인력·인수 현황)를 관리하는 사람입니다."
        />

        {canEdit && (
          <div style={{ display: 'flex', gap: 8, marginTop: 14, alignItems: 'center' }}>
            <button className="primary" style={{ width: 160 }} disabled={!dirty} onClick={save}>
              저장
            </button>
            <button
              className="secondary"
              style={{ marginTop: 0 }}
              disabled={!dirty}
              onClick={() => {
                setForm({ type: op.type, startDate: op.startDate, endDate: op.endDate, sla: op.sla, contractAmount: op.contractAmount })
                setManager(op.manager)
                setTeam(op.team)
              }}
            >
              되돌리기
            </button>
            {saved && <span className="pms-tag done">저장했습니다</span>}
            {dirty && !saved && <span className="pms-tag manual">저장하지 않은 변경</span>}
          </div>
        )}
        {partners.length > 0 && (
          <div className="pms-field-hint" style={{ marginTop: 8 }}>
            협력사 운영 인력({partners.map((m) => `${m.name}(${m.notify || '미설정'})`).join(', ')})은 업무 알림으로 진척·이슈를 처리하고, 운영 관리자가 대신 입력할 수도 있습니다.
          </div>
        )}
      </div>

      <div className="pms-panel">
        <h3>인수(이관) 현황</h3>
        <div className="pms-form-section" style={{ marginTop: 4 }}>
          운영 대상 기능 <span className="muted" style={{ fontWeight: 400 }}>{op.features.length}건</span>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {op.features.length ? op.features.map((f) => <span key={f} className="pms-tag doing">{f}</span>) : <span className="muted">없음</span>}
        </div>
        <div className="pms-form-section">
          인수 산출물 <span className="muted" style={{ fontWeight: 400 }}>{op.docs.length}종</span>
        </div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {op.docs.length ? op.docs.map((d) => <span key={`${d.phase}${d.name}`} className="pms-tag">{d.phase} · {d.name} {d.version}</span>) : <span className="muted">없음</span>}
        </div>
        <div className="pms-form-section">
          이관 이슈 <span className="muted" style={{ fontWeight: 400 }}>{op.carriedIssues.length}건</span>
        </div>
        {op.carriedIssues.length ? (
          op.carriedIssues.map((i) => (
            <div key={i.id} style={{ fontSize: 13 }}>
              • <b>{i.id}</b> {i.title} <span className={`pms-tag ${i.priority === '긴급' ? 'late' : ''}`}>{i.priority}</span>
            </div>
          ))
        ) : (
          <span className="muted">없음</span>
        )}
      </div>
    </div>
  )
}

/**
 * 운영 단계 「프로젝트 관리」 — 영업·수행 단계 정보(수정 불가) + 운영단계전환에서 지정한 운영 정보(운영 형태·기간·SLA, 운영 관리자·인력)와 인수 현황.
 * 운영 관리자만 운영 정보를 수정한다.
 */
export default function OpsProjectPage() {
  const projects = useProjects()
  const [params] = useSearchParams()
  const list = projects.filter((p) => p.operation)
  const [projectId, setProjectId] = useState(() => list.find((p) => p.id === params.get('project'))?.id ?? list[0]?.id ?? '')
  const project = list.find((p) => p.id === projectId)

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            프로젝트 관리 (운영)
          </h2>
          <p className="page-description">
            운영 중인 프로젝트의 기본 정보와 운영 정보를 봅니다. 영업·수행 단계 정보는 수정할 수 없고, 운영단계전환에서 지정한 <b>운영 형태·기간·SLA, 운영 관리자·운영 인력</b>은
            운영 관리자가 수정합니다.
          </p>
        </div>
      </div>
      {list.length === 0 ? (
        <p className="muted">운영 중인 프로젝트가 없습니다. 운영단계전환에서 수행 종료 프로젝트를 전환하세요.</p>
      ) : (
        <>
          <div className="pms-toolbar">
            <select value={projectId} onChange={(e) => setProjectId(e.target.value)}>
              {list.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.id} · {p.name} (운영 관리자 {p.operation.manager.name})
                </option>
              ))}
            </select>
          </div>
          {project && <OpsProject key={`${project.id}|${project.status}`} project={project} />}
        </>
      )}
    </div>
  )
}
