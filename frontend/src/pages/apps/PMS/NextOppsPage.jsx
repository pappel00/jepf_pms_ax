import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import Modal from '../../../components/Modal'
import './pms.css'
import { SortTh, useSort } from './sortUtil'
import { customerOf, useCustomerState } from './customerStore'
import { useIssues } from './issueStore'
import { MEMBERS, ME, TODAY } from './pmsStore'
import { formatKoreanWon, useProjects } from './projectStore'
import { BUDGET_STATUSES, NEXT_KINDS, NEXT_SOURCES, NEXT_STAGES, PRIORITIES, deleteNext, emptyNext, isOpenNext, saveNext, useNextOpps } from './nextOppStore'

const STAGE_CLASS = { 발굴: '', '검토 중': 'doing', '제안 준비': 'doing', '제안 완료': 'manual', '보류/실패': 'late', '이관 완료': 'done' }
const dday = (date) => {
  const diff = Math.round((new Date(date) - new Date(TODAY)) / 86400000)
  return diff === 0 ? 'D-Day' : diff > 0 ? `D-${diff}` : `D+${-diff}`
}

function NextForm({ initial, project, customerId, onClose }) {
  const { contacts } = useCustomerState()
  const issues = useIssues().filter((i) => i.projectId === project.id)
  const [o, setO] = useState(initial)
  const set = (patch) => setO((p) => ({ ...p, ...patch }))
  const cs = contacts.filter((t) => t.customerId === customerId)
  const pickIssue = (id) => {
    const i = issues.find((x) => x.id === id)
    setO((p) => ({ ...p, issueId: id, title: p.title || i?.title || '', content: p.content || i?.content || '', source: id ? '수행 이슈' : p.source }))
  }
  const err = !o.title.trim() ? '건명을 입력하세요.' : !o.content.trim() ? '요구 내용·배경을 입력하세요.' : o.nextDate && !o.nextAction.trim() ? '다음 할 일 내용을 입력하세요.' : o.stage === '보류/실패' && !o.memo.trim() ? '보류/실패 사유를 비고에 적어 주세요.' : ''
  const ctSel = (label, key, hint) => (
    <div className="field">
      <label>{label}</label>
      <select value={o[key]} onChange={(e) => set({ [key]: e.target.value })}>
        <option value="">선택 안 함</option>
        {cs.map((t) => (
          <option key={t.id} value={t.id}>{t.name}{t.title ? ` ${t.title}` : ''}{t.dept ? ` (${t.dept})` : ''}</option>
        ))}
      </select>
      {hint && <div className="pms-field-hint">{hint}</div>}
    </div>
  )
  const sel = (label, key, options) => (
    <div className="field">
      <label>{label}</label>
      <select value={o[key]} onChange={(e) => set({ [key]: e.target.value })}>
        {options.map((x) => <option key={x}>{x}</option>)}
      </select>
    </div>
  )
  const num = (label, key, step) => (
    <div className="field">
      <label>{label}</label>
      <input type="number" min="0" step={step} value={o[key] ?? ''} onChange={(e) => set({ [key]: e.target.value === '' ? null : Number(e.target.value) })} />
    </div>
  )
  return (
    <Modal title={o.id ? `차기 사업 기회 수정 — ${o.id}` : '차기 사업 기회 등록'} onClose={onClose} width={760}>
      <div className="pms-form-section">기회 정보</div>
      <div className="field">
        <label>건명<span className="req-mark"> *</span></label>
        <input value={o.title} onChange={(e) => set({ title: e.target.value })} placeholder="예) 모바일 앱(결재·알림) 개발" />
      </div>
      <div className="pms-form-grid">
        {sel('기회 유형', 'kind', NEXT_KINDS)}
        {sel('발생 경로', 'source', NEXT_SOURCES)}
        <div className="field">
          <label>연결 수행 이슈</label>
          <select value={o.issueId} onChange={(e) => pickIssue(e.target.value)}>
            <option value="">선택 안 함</option>
            {issues.map((i) => <option key={i.id} value={i.id}>{i.id} {i.title}</option>)}
          </select>
          <div className="pms-field-hint">이슈를 고르면 건명·내용이 비어 있을 때 채워집니다.</div>
        </div>
        {sel('우선순위', 'priority', PRIORITIES)}
      </div>
      <div className="field">
        <label>요구 내용·배경<span className="req-mark"> *</span></label>
        <textarea rows={3} value={o.content} onChange={(e) => set({ content: e.target.value })} placeholder="고객이 무엇을 원하는지, 왜 지금 범위에 못 넣는지" />
      </div>
      <div className="field">
        <label>현재 범위 외 사유</label>
        <input value={o.scopeReason} onChange={(e) => set({ scopeReason: e.target.value })} placeholder="예) 요구사항 정의서 범위 초과, 일정·예산 제약" />
      </div>

      <div className="pms-form-section">고객사 담당자</div>
      <div className="pms-form-grid">
        {ctSel('요청자 (고객 담당자)', 'contactId', '고객사 담당자 관리에 등록된 담당자에서 선택')}
        {ctSel('의사결정자', 'decisionMakerId', '예산·계약을 결정하는 사람 (없으면 영업 시 확인 필요)')}
      </div>
      <p className="muted" style={{ marginTop: 0 }}>
        담당자가 목록에 없으면{' '}
        <button type="button" className="link-btn" onClick={() => window.open('/workspace/PMS/customer-contacts', '_blank')}>고객사 담당자 관리</button>에서 먼저 등록하세요.
      </p>

      <div className="pms-form-section">규모·시기·진행</div>
      <div className="pms-form-grid">
        <div className="field">
          <label>예상 금액 (원)</label>
          <input inputMode="numeric" value={o.amount ? Number(o.amount).toLocaleString() : ''} onChange={(e) => { const d = e.target.value.replace(/[^\d]/g, ''); set({ amount: d ? Number(d) : null }) }} />
        </div>
        {num('예상 규모 (M/M)', 'estMM', 0.5)}
        <div className="field">
          <label>예상 시기</label>
          <input value={o.quarter} onChange={(e) => set({ quarter: e.target.value })} placeholder="예) 2027-Q1" />
        </div>
        {sel('고객 예산', 'budget', BUDGET_STATUSES)}
        {sel('단계', 'stage', NEXT_STAGES.filter((s) => s !== '이관 완료' || o.stage === '이관 완료'))}
        <div className="field">
          <label>영업 담당자</label>
          <select value={o.owner} onChange={(e) => set({ owner: e.target.value })}>
            {MEMBERS.map((m) => <option key={m.name}>{m.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>다음 할 일</label>
          <input value={o.nextAction} onChange={(e) => set({ nextAction: e.target.value })} />
        </div>
        <div className="field">
          <label>예정일</label>
          <input type="date" value={o.nextDate} onChange={(e) => set({ nextDate: e.target.value })} />
        </div>
      </div>
      <div className="field">
        <label>비고</label>
        <textarea rows={2} value={o.memo} onChange={(e) => set({ memo: e.target.value })} />
      </div>
      {o.opsOppId && <p className="muted">이관된 운영 영업 기회: {o.opsOppId}</p>}
      {err && <p style={{ color: '#c0392b' }}>{err}</p>}
      <button className="primary" disabled={!!err} onClick={() => { saveNext({ ...o, title: o.title.trim() }); onClose() }}>저장</button>
    </Modal>
  )
}

/**
 * 차기 사업 기회 관리 — 수행 중 나온 범위 외 요구·후속 사업 가능성을 모아 다음 사업으로 이어 간다.
 * 선택한 기회는 운영단계전환 때 운영 영업 관리로 이관된다.
 */
export default function NextOppsPage() {
  const projects = useProjects().filter((p) => p.transitioned && ['수행 중', '수행 종료', '운영 중'].includes(p.status))
  const all = useNextOpps()
  const { customers, contacts } = useCustomerState()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const project = projects.find((p) => p.id === params.get('project')) ?? projects[0]
  const [stage, setStage] = useState('')
  const [kind, setKind] = useState('')
  const [edit, setEdit] = useState(null)

  const customerId = project ? customerOf(customers, project)?.id : ''
  const list = all.filter((o) => o.projectId === project?.id)
  const rows = list.filter((o) => (!stage || o.stage === stage) && (!kind || o.kind === kind))
  const active = list.filter(isOpenNext)
  const { sort, onSort, sorted } = useSort(rows, {
    id: (o) => o.id,
    kind: (o) => o.kind,
    title: (o) => o.title,
    contact: (o) => contacts.find((t) => t.id === o.contactId)?.name,
    stage: (o) => NEXT_STAGES.indexOf(o.stage),
    amount: (o) => o.amount || 0,
    quarter: (o) => o.quarter,
    next: (o) => o.nextDate,
  })
  const ct = (id) => contacts.find((t) => t.id === id)

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>차기 사업 기회 관리</h2>
          <p className="page-description">
            수행 중 고객이 요청했지만 현재 요구사항 범위에 넣지 못한 추가 사항, 후속 단계, 확산 가능성을 모아 다음 사업 기회로 관리합니다. 요청자·의사결정자 같은 고객사 담당자 정보를 함께 남기고, 운영단계전환 때 선택한 기회를 운영 영업 관리로 이관합니다.
          </p>
        </div>
        <button className="primary small" disabled={!project} onClick={() => setEdit(emptyNext(project.id, ME))}>＋ 기회 등록</button>
      </div>
      <div className="search-bar">
        <div className="search-field">
          <label>프로젝트</label>
          <select value={project?.id ?? ''} onChange={(e) => setParams({ project: e.target.value })} style={{ minWidth: 340 }}>
            {projects.map((p) => <option key={p.id} value={p.id}>{p.id} · {p.name} ({p.status})</option>)}
          </select>
        </div>
        {project && <span className="muted">발주사 {project.client} · 수행 {project.start} ~ {project.end}</span>}
      </div>
      {edit && project && <NextForm initial={edit} project={project} customerId={customerId} onClose={() => setEdit(null)} />}
      {project && (
        <>
          <div className="pms-summary">
            <div className="pms-stat"><div className="label">진행 중 기회</div><div className="value">{active.length}</div></div>
            <div className="pms-stat"><div className="label">예상 금액 합계</div><div className="value" style={{ fontSize: 18 }}>{formatKoreanWon(active.reduce((s, o) => s + (o.amount ?? 0), 0))}</div></div>
            <div className="pms-stat"><div className="label">예상 규모 (M/M)</div><div className="value">{active.reduce((s, o) => s + (o.estMM ?? 0), 0)}</div></div>
            <div className={`pms-stat ${active.some((o) => !o.decisionMakerId) ? 'warn' : ''}`}><div className="label">의사결정자 미확인</div><div className="value">{active.filter((o) => !o.decisionMakerId).length}</div></div>
            <div className="pms-stat ok"><div className="label">운영 영업 이관</div><div className="value">{list.filter((o) => o.stage === '이관 완료').length}</div></div>
          </div>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap', margin: '8px 0' }}>
            <div className="pms-seg">
              <button className={stage === '' ? 'on' : ''} onClick={() => setStage('')}>전체 {list.length}</button>
              {NEXT_STAGES.map((s) => <button key={s} className={stage === s ? 'on' : ''} onClick={() => setStage(s)}>{s} {list.filter((o) => o.stage === s).length}</button>)}
            </div>
            <select value={kind} onChange={(e) => setKind(e.target.value)}>
              <option value="">유형 전체</option>
              {NEXT_KINDS.map((k) => <option key={k}>{k}</option>)}
            </select>
          </div>
          <table className="data-table pms-wbs">
            <thead>
              <tr><SortTh k="id" sort={sort} onSort={onSort}>코드</SortTh><SortTh k="kind" sort={sort} onSort={onSort}>유형</SortTh><SortTh k="title" sort={sort} onSort={onSort}>건명 · 요구 내용</SortTh><SortTh k="contact" sort={sort} onSort={onSort}>고객 담당자</SortTh><SortTh k="stage" sort={sort} onSort={onSort}>단계</SortTh><SortTh k="amount" sort={sort} onSort={onSort} style={{ textAlign: 'right' }}>예상 금액(원)</SortTh><SortTh k="quarter" sort={sort} onSort={onSort}>시기</SortTh><SortTh k="next" sort={sort} onSort={onSort}>다음 할 일</SortTh><th></th></tr>
            </thead>
            <tbody>
              {rows.length === 0 && <tr><td className="empty-cell" colSpan={9}>등록된 기회가 없습니다.</td></tr>}
              {sorted.map((o) => (
                <tr key={o.id} style={o.stage === '이관 완료' || o.stage === '보류/실패' ? { opacity: 0.65 } : undefined}>
                  <td>{o.id}</td>
                  <td><span className="pms-tag copy">{o.kind}</span><div className="muted">{o.source}</div></td>
                  <td className="name-cell" style={{ maxWidth: 360 }}>
                    <b>{o.title}</b> {o.priority === '높음' && <span className="pms-tag late">높음</span>}
                    {o.issueId && <span className="pms-tag manual" style={{ marginLeft: 6 }}>이슈 {o.issueId}</span>}
                    <div className="muted">{o.content}</div>
                    {o.scopeReason && <div className="muted">범위 외: {o.scopeReason}</div>}
                  </td>
                  <td>
                    {o.contactId ? <>요청 <b>{ct(o.contactId)?.name}</b> {ct(o.contactId)?.title}<div className="muted">{ct(o.contactId)?.phone || ct(o.contactId)?.email}</div></> : <span className="muted">-</span>}
                    {o.decisionMakerId ? <div>결정 <b>{ct(o.decisionMakerId)?.name}</b> {ct(o.decisionMakerId)?.title}</div> : isOpenNext(o) && <div><span className="pms-tag manual">결정자 미확인</span></div>}
                  </td>
                  <td><span className={`pms-tag ${STAGE_CLASS[o.stage]}`}>{o.stage}</span><div className="muted">예산 {o.budget}</div></td>
                  <td style={{ textAlign: 'right' }}>{o.amount ? o.amount.toLocaleString() : '-'}{o.estMM ? <div className="muted">{o.estMM} M/M</div> : null}</td>
                  <td>{o.quarter || '-'}</td>
                  <td>{o.nextAction ? <>{o.nextAction} {o.nextDate && <span className={`pms-tag ${o.nextDate < TODAY ? 'late' : ''}`}>{o.nextDate.slice(5)} ({dday(o.nextDate)})</span>}</> : <span className="muted">-</span>}</td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    <button className="link-btn" onClick={() => setEdit(o)}>수정</button>{' '}
                    {o.opsOppId ? <button className="link-btn" onClick={() => navigate(`/workspace/PMS/ops-sales?project=${o.projectId}`)}>운영 영업 {o.opsOppId} ›</button> : <button className="link-btn" onClick={() => window.confirm('이 기회를 삭제할까요?') && deleteNext(o.id)}>삭제</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pms-field-hint">진행 중인 기회(보류/실패·이관 완료 제외)는 운영단계전환의 「차기 사업 기회 이관」에서 선택해 운영 영업 관리의 영업 기회로 넘길 수 있습니다. 이관하면 단계가 「이관 완료」가 됩니다.</div>
        </>
      )}
    </div>
  )
}
