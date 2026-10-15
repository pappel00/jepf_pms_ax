import { Fragment, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import Modal from '../../../components/Modal'
import './pms.css'
import { contactOf, useCustomerState } from './customerStore'
import { ME, MEMBERS, TODAY } from './pmsStore'
import { PIPELINE_STATUSES, saveProject, useProjects } from './projectStore'
import { NOTE_TYPES, deleteNote, emptyNote, saveNote, toggleDone, useSalesNotes } from './salesNoteStore'

const dday = (date) => {
  const diff = Math.round((new Date(date) - new Date(TODAY)) / 86400000)
  return diff === 0 ? 'D-Day' : diff > 0 ? `D-${diff}` : `D+${-diff}`
}
const TYPE_CLASS = { 방문: 'doing', 미팅: 'doing', 전화: '', 이메일: '', '식사·접대': 'manual', '제안·발표': 'done', 인수인계: 'late' }

/* ---------- 노트 작성/수정 ---------- */
function NoteForm({ initial, projects, customers, contacts, onClose }) {
  const [n, setN] = useState(initial)
  const set = (patch) => setN((p) => ({ ...p, ...patch }))
  const pickProject = (id) => {
    const p = projects.find((x) => x.id === id)
    const c = p ? customers.find((x) => x.name === p.client) : null
    setN((prev) => ({ ...prev, projectId: id, customerId: c?.id ?? prev.customerId, contactId: '' }))
  }
  const err = !n.title.trim() ? '제목을 입력하세요.' : !n.content.trim() ? '내용을 입력하세요.' : !n.projectId && !n.customerId ? '프로젝트 또는 고객사 중 하나는 선택하세요.' : n.nextDate && !n.nextAction.trim() ? '다음 할 일 내용을 입력하세요.' : ''
  const cs = contacts.filter((t) => t.customerId === n.customerId)
  return (
    <Modal title={n.id ? `영업 노트 수정 — ${n.id}` : '영업 노트 작성'} onClose={onClose} width={640}>
      <div className="pms-form-grid">
        <div className="field">
          <label>활동일<span className="req-mark"> *</span></label>
          <input type="date" value={n.date} onChange={(e) => set({ date: e.target.value })} />
        </div>
        <div className="field">
          <label>활동 유형</label>
          <select value={n.type} onChange={(e) => set({ type: e.target.value })}>
            {NOTE_TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
        </div>
        <div className="field">
          <label>프로젝트</label>
          <select value={n.projectId} onChange={(e) => pickProject(e.target.value)}>
            <option value="">프로젝트 등록 전 (고객사만)</option>
            {projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>고객사</label>
          <select value={n.customerId} onChange={(e) => set({ customerId: e.target.value, contactId: '' })}>
            <option value="">선택</option>
            {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>만난 담당자</label>
          <select value={n.contactId} disabled={!n.customerId} onChange={(e) => set({ contactId: e.target.value })}>
            <option value="">선택 안 함</option>
            {cs.map((t) => <option key={t.id} value={t.id}>{t.name}{t.title ? ` ${t.title}` : ''}</option>)}
          </select>
        </div>
        <div className="field">
          <label>작성자</label>
          <input value={n.author} disabled />
        </div>
      </div>
      <div className="field">
        <label>제목<span className="req-mark"> *</span></label>
        <input value={n.title} onChange={(e) => set({ title: e.target.value })} placeholder="예) 예산 진행 상황 확인" />
      </div>
      <div className="field">
        <label>내용<span className="req-mark"> *</span></label>
        <textarea rows={5} value={n.content} onChange={(e) => set({ content: e.target.value })} placeholder="논의 내용, 고객 반응, 경쟁사 동향, 알게 된 정보 등 — 후임자가 읽어도 이해되게 적어 주세요." />
      </div>
      <div className="pms-form-grid">
        <div className="field">
          <label>다음 할 일</label>
          <input value={n.nextAction} onChange={(e) => set({ nextAction: e.target.value })} placeholder="예) 사전 제안 미팅 요청" />
        </div>
        <div className="field">
          <label>예정일</label>
          <input type="date" value={n.nextDate} onChange={(e) => set({ nextDate: e.target.value })} />
        </div>
      </div>
      {err && <p style={{ color: '#c0392b' }}>{err}</p>}
      <button className="primary" disabled={!!err} onClick={() => { saveNote({ ...n, title: n.title.trim(), content: n.content.trim() }); onClose() }}>
        저장
      </button>
    </Modal>
  )
}

/* ---------- 인수인계 ---------- */
function HandoverModal({ projects, notes, customers, onClose }) {
  const mine = projects.filter((p) => PIPELINE_STATUSES.includes(p.status))
  const [pid, setPid] = useState(mine[0]?.id ?? '')
  const [to, setTo] = useState('')
  const [memo, setMemo] = useState('')
  const [done, setDone] = useState(false)
  const project = projects.find((p) => p.id === pid)
  const hist = notes.filter((n) => n.projectId === pid).sort((a, b) => a.date.localeCompare(b.date))
  const open = hist.filter((n) => n.nextAction && !n.done)
  const customer = project ? customers.find((c) => c.name === project.client) : null
  const names = [...new Set([...MEMBERS.map((m) => m.name), ...projects.map((p) => p.salesName).filter(Boolean)])]

  const summary = project
    ? [
        `[영업 인수인계] ${project.name} (${project.id})`,
        `고객사: ${project.client} / 상태: ${project.status} / 확률 ${project.probability}% / 발주금액 ${project.amount?.toLocaleString() ?? '-'}원`,
        `담당자: ${project.salesName || '-'} → ${to || '(미정)'}`,
        ...(memo ? [`인수인계 메모: ${memo}`] : []),
        '',
        '■ 활동 히스토리',
        ...(hist.length ? hist.map((n) => `- ${n.date} [${n.type}] ${n.title}: ${n.content}`) : ['- (기록 없음)']),
        '',
        '■ 남은 후속 조치',
        ...(open.length ? open.map((n) => `- ${n.nextDate || '일정 미정'} ${n.nextAction}`) : ['- 없음']),
      ].join('\n')
    : ''

  const submit = () => {
    saveProject({ ...project, salesName: to, salesId: null })
    saveNote({ ...emptyNote(ME, TODAY), type: '인수인계', projectId: project.id, customerId: customer?.id ?? '', title: `영업 담당 인수인계: ${project.salesName || '-'} → ${to}`, content: memo || '인수인계 완료' })
    setDone(true)
  }

  return (
    <Modal title="영업 담당자 인수인계" onClose={onClose} width={760}>
      {done ? (
        <div>
          <p>인수인계를 처리했습니다. 영업 담당자가 <b>{to}</b>(으)로 변경되었고, 영업 노트에 인수인계 기록이 남았습니다.</p>
          <button className="primary" onClick={onClose}>닫기</button>
        </div>
      ) : (
        <>
          <p className="muted" style={{ marginTop: 0 }}>담당자를 바꾸면 기존 활동 기록은 그대로 유지되고, 후임자는 프로젝트별 히스토리를 이어서 봅니다.</p>
          <div className="pms-form-grid">
            <div className="field">
              <label>프로젝트(영업 진행 중)<span className="req-mark"> *</span></label>
              <select value={pid} onChange={(e) => setPid(e.target.value)}>
                {mine.map((p) => <option key={p.id} value={p.id}>{p.name} — {p.salesName || '담당자 없음'}</option>)}
              </select>
            </div>
            <div className="field">
              <label>인수 담당자<span className="req-mark"> *</span></label>
              <input list="sales-people" value={to} onChange={(e) => setTo(e.target.value)} placeholder="이름 선택/입력" />
              <datalist id="sales-people">{names.filter((x) => x !== project?.salesName).map((x) => <option key={x} value={x} />)}</datalist>
            </div>
          </div>
          <div className="field">
            <label>인수인계 메모</label>
            <textarea rows={3} value={memo} onChange={(e) => setMemo(e.target.value)} placeholder="핵심 인물, 주의사항, 진행 중인 약속 등" />
          </div>
          <div className="pms-form-section">인수인계 요약 (자동 생성)</div>
          <textarea readOnly rows={10} style={{ width: '100%', fontSize: 12 }} value={summary} />
          <div style={{ marginTop: 8, display: 'flex', gap: 8 }}>
            <button className="secondary small" onClick={() => navigator.clipboard?.writeText(summary)}>요약 복사</button>
            <button className="primary" disabled={!project || !to.trim() || to === project.salesName} onClick={submit}>인수인계 처리</button>
          </div>
        </>
      )}
    </Modal>
  )
}

/**
 * 영업 노트 — 영업 담당자의 사전영업·제안 활동 기록. 프로젝트/고객사 단위 히스토리로 담당자가 바뀌어도 이어진다.
 */
export default function SalesNotesPage() {
  const notes = useSalesNotes()
  const projects = useProjects()
  const { customers, contacts } = useCustomerState()
  const [params] = useSearchParams()
  const [f, setF] = useState({ projectId: params.get('project') ?? '', customerId: '', author: '', type: '', keyword: '' })
  const [edit, setEdit] = useState(null)
  const [handover, setHandover] = useState(false)
  const [open, setOpen] = useState(null)

  const pname = (id) => projects.find((p) => p.id === id)?.name
  const cname = (id) => customers.find((c) => c.id === id)?.name
  const authors = [...new Set(notes.map((n) => n.author))]
  const rows = useMemo(
    () =>
      notes
        .filter(
          (n) =>
            (!f.projectId || n.projectId === f.projectId) &&
            (!f.customerId || n.customerId === f.customerId) &&
            (!f.author || n.author === f.author) &&
            (!f.type || n.type === f.type) &&
            (!f.keyword || `${n.title} ${n.content} ${n.nextAction}`.toLowerCase().includes(f.keyword.toLowerCase())),
        )
        .sort((a, b) => b.date.localeCompare(a.date) || b.id.localeCompare(a.id)),
    [notes, f],
  )
  const todo = notes.filter((n) => n.nextAction && !n.done).sort((a, b) => (a.nextDate || '9').localeCompare(b.nextDate || '9'))
  const overdue = todo.filter((n) => n.nextDate && n.nextDate < TODAY)
  const set = (patch) => setF((p) => ({ ...p, ...patch }))

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>영업 노트 <span className="pms-mock-badge">목업 · 더미 데이터</span></h2>
          <p className="page-description">
            방문·미팅·통화 등 영업 활동과 고객 반응, 다음 할 일을 기록합니다. 프로젝트·고객사별로 히스토리가 쌓여, 담당자가 바뀌어도 업무를 그대로 이어받을 수 있습니다.
          </p>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="secondary" onClick={() => setHandover(true)}>🔁 담당자 인수인계</button>
          <button className="primary" onClick={() => setEdit(emptyNote(ME, TODAY))}>＋ 노트 작성</button>
        </div>
      </div>
      {edit && <NoteForm initial={edit} projects={projects} customers={customers} contacts={contacts} onClose={() => setEdit(null)} />}
      {handover && <HandoverModal projects={projects} notes={notes} customers={customers} onClose={() => setHandover(false)} />}

      <div className="pms-summary">
        <div className="pms-stat"><div className="label">전체 노트</div><div className="value">{notes.length}</div></div>
        <div className="pms-stat"><div className="label">남은 후속 조치</div><div className="value">{todo.length}</div></div>
        <div className={`pms-stat ${overdue.length ? 'danger' : ''}`}><div className="label">예정일 초과</div><div className="value">{overdue.length}</div></div>
        <div className="pms-stat"><div className="label">최근 7일 활동</div><div className="value">{notes.filter((n) => n.date >= '2026-10-06' && n.type !== '인수인계').length}</div></div>
      </div>

      {todo.length > 0 && (
        <div className="pms-panel">
          <h3>후속 조치 <span className="count">{todo.length}</span></h3>
          {todo.map((n) => (
            <div key={n.id} className="pms-task">
              <div className="pms-task-main">
                <div className="pms-task-title">{n.nextAction}</div>
                <div className="pms-task-sub">
                  {pname(n.projectId) ?? cname(n.customerId)} · {n.author} · 기록 {n.date}
                </div>
              </div>
              {n.nextDate && <span className={`pms-tag ${n.nextDate < TODAY ? 'late' : 'doing'}`}>{n.nextDate} ({dday(n.nextDate)})</span>}
              <button className="secondary small" onClick={() => toggleDone(n.id)}>완료</button>
            </div>
          ))}
        </div>
      )}

      <div className="search-bar">
        <div className="search-field">
          <label>프로젝트</label>
          <select value={f.projectId} onChange={(e) => set({ projectId: e.target.value })}>
            <option value="">전체</option>
            {projects.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </select>
        </div>
        <div className="search-field">
          <label>고객사</label>
          <select value={f.customerId} onChange={(e) => set({ customerId: e.target.value })}>
            <option value="">전체</option>
            {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
        <div className="search-field">
          <label>작성자</label>
          <select value={f.author} onChange={(e) => set({ author: e.target.value })}>
            <option value="">전체</option>
            {authors.map((a) => <option key={a}>{a}</option>)}
          </select>
        </div>
        <div className="search-field">
          <label>유형</label>
          <select value={f.type} onChange={(e) => set({ type: e.target.value })}>
            <option value="">전체</option>
            {NOTE_TYPES.map((t) => <option key={t}>{t}</option>)}
          </select>
        </div>
        <div className="search-field">
          <label>검색</label>
          <input value={f.keyword} onChange={(e) => set({ keyword: e.target.value })} placeholder="제목·내용" />
        </div>
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>활동일</th>
            <th>유형</th>
            <th>프로젝트 / 고객사</th>
            <th>제목</th>
            <th>작성자</th>
            <th>다음 할 일</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && <tr><td className="empty-cell" colSpan={7}>기록이 없습니다.</td></tr>}
          {rows.map((n) => (
            <Fragment key={n.id}>
              <tr style={{ cursor: 'pointer' }} onClick={() => setOpen(open === n.id ? null : n.id)}>
                <td>{n.date}</td>
                <td><span className={`pms-tag ${TYPE_CLASS[n.type] ?? ''}`}>{n.type}</span></td>
                <td className="name-cell">
                  <b>{pname(n.projectId) ?? '(프로젝트 등록 전)'}</b>
                  <div className="muted">{cname(n.customerId) ?? '-'}{n.contactId && ` · ${contacts.find((t) => t.id === n.contactId)?.name ?? ''}`}</div>
                </td>
                <td className="name-cell">{n.title}</td>
                <td>{n.author}</td>
                <td>
                  {n.nextAction ? (
                    <span style={{ textDecoration: n.done ? 'line-through' : 'none' }}>
                      {n.nextAction} {n.nextDate && <span className="muted">({n.nextDate})</span>}
                    </span>
                  ) : <span className="muted">-</span>}
                </td>
                <td onClick={(e) => e.stopPropagation()}>
                  <button className="link-btn" onClick={() => setEdit(n)}>수정</button>{' '}
                  <button className="link-btn" onClick={() => window.confirm('이 노트를 삭제할까요?') && deleteNote(n.id)}>삭제</button>
                </td>
              </tr>
              {open === n.id && (
                <tr key={`${n.id}-d`}>
                  <td colSpan={7} style={{ background: '#fafbfd', whiteSpace: 'pre-wrap' }}>{n.content}</td>
                </tr>
              )}
            </Fragment>
          ))}
        </tbody>
      </table>
      <div className="pms-field-hint">행을 누르면 내용이 펼쳐집니다. 프로젝트 등록 전 활동은 고객사만 선택해 남기고, 프로젝트가 생기면 연결해 이어갈 수 있습니다.</div>
    </div>
  )
}
