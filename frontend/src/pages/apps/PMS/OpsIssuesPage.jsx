import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Modal from '../../../components/Modal'
import './pms.css'
import { useOpsProject } from './OpsProjectBar'
import { MEMBERS, ME, TODAY } from './pmsStore'
import {
  INCIDENT_STATUSES, INCIDENT_TYPES, NOW, PRIORITIES, deleteIncident, emptyIncident, emptyOpp, emptyTask, fmtDue, isOpenIncident,
  linkOpportunity, saveIncident, saveOpp, saveTask, slaOf, useOpsIncidents,
} from './opsStore'

const PRI_CLASS = { 긴급: 'late', 높음: 'late', 보통: 'doing', 낮음: '' }
const SLA_CLASS = { 충족: 'done', 정상: '', 임박: 'manual', 위반: 'late' }
const toLocal = (s) => s.replace(' ', 'T')
const fromLocal = (s) => s.replace('T', ' ')

function IssueForm({ initial, onClose }) {
  const [i, setI] = useState(initial)
  const set = (patch) => setI((p) => ({ ...p, ...patch }))
  const closing = !isOpenIncident(i.status)
  const err = !i.title.trim() ? '제목을 입력하세요.' : !i.content.trim() ? '내용을 입력하세요.' : closing && !i.action.trim() ? '해결·종결 시 조치 내용은 필수입니다.' : ''
  const sel = (label, key, options) => (
    <div className="field">
      <label>{label}</label>
      <select value={i[key]} onChange={(e) => set({ [key]: e.target.value })}>
        {options.map((o) => <option key={o}>{o}</option>)}
      </select>
    </div>
  )
  return (
    <Modal title={i.id ? `운영 이슈 수정 — ${i.id}` : '운영 이슈 접수'} onClose={onClose} width={660}>
      <div className="field">
        <label>제목<span className="req-mark"> *</span></label>
        <input value={i.title} onChange={(e) => set({ title: e.target.value })} />
      </div>
      <div className="pms-form-grid">
        {sel('유형', 'type', INCIDENT_TYPES)}
        {sel('우선순위', 'priority', PRIORITIES)}
        {sel('상태', 'status', INCIDENT_STATUSES)}
        <div className="field">
          <label>접수 일시</label>
          <input type="datetime-local" value={toLocal(i.receivedAt)} onChange={(e) => set({ receivedAt: fromLocal(e.target.value) })} />
        </div>
        <div className="field">
          <label>요청자 (고객사)</label>
          <input value={i.reporter} onChange={(e) => set({ reporter: e.target.value })} />
        </div>
        <div className="field">
          <label>담당자</label>
          <select value={i.assignee} onChange={(e) => set({ assignee: e.target.value })}>
            <option value="">미배정</option>
            {MEMBERS.map((m) => <option key={m.name}>{m.name}</option>)}
          </select>
        </div>
      </div>
      <div className="field">
        <label>내용<span className="req-mark"> *</span></label>
        <textarea rows={3} value={i.content} onChange={(e) => set({ content: e.target.value })} />
      </div>
      <div className="field">
        <label>조치 내용{closing && <span className="req-mark"> *</span>}</label>
        <textarea rows={3} value={i.action} onChange={(e) => set({ action: e.target.value })} />
      </div>
      <div className="pms-field-hint">상태를 분석 이상으로 바꾸면 응답 시각, 해결·종결로 바꾸면 해결 시각이 자동 기록되어 SLA에 반영됩니다.</div>
      {err && <p style={{ color: '#c0392b' }}>{err}</p>}
      <button className="primary" disabled={!!err} onClick={() => { saveIncident({ ...i, title: i.title.trim() }); onClose() }}>저장</button>
    </Modal>
  )
}

/** 운영 이슈 관리 — 장애·문의·개선 요청을 접수·처리하고 SLA(응답/해결 시한) 준수를 확인한다. */
export default function OpsIssuesPage() {
  const { project, bar } = useOpsProject()
  const all = useOpsIncidents()
  const navigate = useNavigate()
  const [f, setF] = useState({ status: '', type: '', priority: '', keyword: '' })
  const [edit, setEdit] = useState(null)
  const set = (patch) => setF((p) => ({ ...p, ...patch }))

  const list = all.filter((i) => i.projectId === project?.id)
  const withSla = list.map((i) => ({ i, sla: slaOf(i, project?.operation.sla) }))
  const rows = withSla.filter(({ i }) => (!f.status || (f.status === '미해결' ? isOpenIncident(i.status) : i.status === f.status)) && (!f.type || i.type === f.type) && (!f.priority || i.priority === f.priority) && (!f.keyword || `${i.title} ${i.content}`.includes(f.keyword)))
  const open = withSla.filter(({ i }) => isOpenIncident(i.status))
  const violated = withSla.filter(({ sla }) => sla && (sla.respond === '위반' || sla.resolve === '위반'))
  const done = withSla.filter(({ sla, i }) => sla && !isOpenIncident(i.status))
  const ok = done.filter(({ sla }) => sla.respond !== '위반' && sla.resolve !== '위반').length

  const makeTask = (i) => {
    saveTask({ ...emptyTask(project.id, i.assignee || ME), title: `[${i.id}] ${i.title}`, category: i.type === '장애' ? '장애 조치' : '문의 대응', priority: i.priority, due: TODAY, incidentId: i.id })
    window.alert('운영 업무를 등록했습니다. 운영 업무 관리에서 확인하세요.')
  }
  const makeOpp = async (i) => {
    const id = await saveOpp({ ...emptyOpp(project.id, ME), kind: '유상 개선', title: i.title, memo: `운영 이슈 ${i.id}에서 발굴: ${i.content}`, sourceIncidentId: i.id })
    await linkOpportunity(i.id, id)
    navigate(`/workspace/PMS/ops-sales?project=${project.id}`)
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>운영 이슈 관리</h2>
          <p className="page-description">운영 중 들어오는 장애·문의·개선 요청을 접수해 분석 → 조치 → 해결 → 종결로 처리합니다. 운영 정보의 SLA(응답·해결 목표 시간)로 시한을 계산해 위반·임박을 표시하고, 이슈에서 운영 업무나 영업 기회를 바로 만들 수 있습니다.</p>
        </div>
        <button className="primary" disabled={!project} onClick={() => setEdit(emptyIncident(project.id, project.client))}>＋ 이슈 접수</button>
      </div>
      {bar}
      {edit && <IssueForm initial={edit} onClose={() => setEdit(null)} />}
      {project && (
        <>
          <div className="pms-summary">
            <div className="pms-stat"><div className="label">미해결</div><div className="value">{open.length}</div></div>
            <div className={`pms-stat ${open.some(({ i }) => i.priority === '긴급') ? 'danger' : ''}`}><div className="label">긴급 미해결</div><div className="value">{open.filter(({ i }) => i.priority === '긴급').length}</div></div>
            <div className={`pms-stat ${violated.length ? 'danger' : ''}`}><div className="label">SLA 위반</div><div className="value">{violated.length}</div></div>
            <div className="pms-stat warn"><div className="label">SLA 임박</div><div className="value">{withSla.filter(({ sla }) => sla && (sla.respond === '임박' || sla.resolve === '임박')).length}</div></div>
            <div className="pms-stat ok"><div className="label">SLA 준수율 (처리 완료 기준)</div><div className="value">{done.length ? `${Math.round((ok / done.length) * 100)}%` : '-'}</div></div>
          </div>
          <div className="search-bar">
            <div className="search-field">
              <label>상태</label>
              <select value={f.status} onChange={(e) => set({ status: e.target.value })}>
                <option value="">전체</option>
                <option>미해결</option>
                {INCIDENT_STATUSES.map((s) => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="search-field">
              <label>유형</label>
              <select value={f.type} onChange={(e) => set({ type: e.target.value })}>
                <option value="">전체</option>
                {INCIDENT_TYPES.map((s) => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="search-field">
              <label>우선순위</label>
              <select value={f.priority} onChange={(e) => set({ priority: e.target.value })}>
                <option value="">전체</option>
                {PRIORITIES.map((s) => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="search-field">
              <label>검색</label>
              <input value={f.keyword} onChange={(e) => set({ keyword: e.target.value })} placeholder="제목·내용" />
            </div>
          </div>
          <table className="data-table pms-wbs">
            <thead>
              <tr><th>코드</th><th>유형</th><th>제목</th><th>우선순위</th><th>상태</th><th>접수 일시</th><th>담당</th><th>SLA 응답</th><th>SLA 해결</th><th></th></tr>
            </thead>
            <tbody>
              {rows.length === 0 && <tr><td className="empty-cell" colSpan={10}>운영 이슈가 없습니다.</td></tr>}
              {rows.map(({ i, sla }) => (
                <tr key={i.id}>
                  <td>{i.id}</td>
                  <td>{i.type}</td>
                  <td className="name-cell">
                    <b>{i.title}</b>
                    <div className="muted">요청 {i.reporter || '-'}{i.opportunityId && ` · 영업 기회 ${i.opportunityId}`}</div>
                  </td>
                  <td><span className={`pms-tag ${PRI_CLASS[i.priority]}`}>{i.priority}</span></td>
                  <td><span className={`pms-tag ${isOpenIncident(i.status) ? 'doing' : 'done'}`}>{i.status}</span></td>
                  <td>{i.receivedAt.slice(5)}</td>
                  <td>{i.assignee || <span className="pms-tag late">미배정</span>}</td>
                  {sla ? (
                    <>
                      <td><span className={`pms-tag ${SLA_CLASS[sla.respond]}`}>{sla.respond}</span><div className="muted">~{fmtDue(sla.respondDue)}</div></td>
                      <td><span className={`pms-tag ${SLA_CLASS[sla.resolve]}`}>{sla.resolve}</span><div className="muted">~{fmtDue(sla.resolveDue)}</div></td>
                    </>
                  ) : (
                    <td colSpan={2}><span className="muted">SLA 대상 아님</span></td>
                  )}
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <button className="link-btn" onClick={() => setEdit(i)}>수정</button>{' '}
                    <button className="link-btn" onClick={() => makeTask(i)}>업무 등록</button>{' '}
                    {!i.opportunityId && <button className="link-btn" onClick={() => makeOpp(i)}>영업 기회</button>}{' '}
                    <button className="link-btn" onClick={() => window.confirm('이 이슈를 삭제할까요?') && deleteIncident(i.id)}>삭제</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pms-field-hint">기준 시각 {NOW} · 응답 시한 = 접수 + 응답 목표 시간, 해결 시한 = 접수 + 해결 목표 시간(서비스 시간·휴일은 아직 반영하지 않음). 개선 요청·기타는 SLA 대상이 아닙니다.</div>
        </>
      )}
    </div>
  )
}
