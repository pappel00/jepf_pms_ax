import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { SortTh, useSort } from './sortUtil'
import { dday, useOpsProject } from './OpsProjectBar'
import { MEMBERS, ME, TODAY } from './pmsStore'
import { PRIORITIES, REPEATS, TASK_CATEGORIES, TASK_STATUSES, deleteTask, emptyTask, saveTask, setTaskStatus, useOpsTasks } from './opsStore'

const PRI_CLASS = { 긴급: 'late', 높음: 'late', 보통: 'doing', 낮음: '' }
const overdue = (t) => t.status !== '완료' && t.due && t.due < TODAY

function TaskForm({ initial, opsEnd, onClose, onSaved }) {
  const [t, setT] = useState(initial)
  const set = (patch) => setT((p) => ({ ...p, ...patch }))
  const err = !t.title.trim() ? '업무명을 입력하세요.' : !t.assignee ? '담당자를 선택하세요.' : !t.due ? '기한을 입력하세요.' : ''
  const sel = (label, key, options) => (
    <div className="field">
      <label>{label}</label>
      <select value={t[key]} onChange={(e) => set({ [key]: e.target.value })}>
        {options.map((o) => <option key={o}>{o}</option>)}
      </select>
    </div>
  )
  return (
    <Modal title={t.id ? `운영 업무 수정 — ${t.id}` : '운영 업무 등록'} onClose={onClose} width={620}>
      <div className="field">
        <label>업무명<span className="req-mark"> *</span></label>
        <input value={t.title} onChange={(e) => set({ title: e.target.value })} placeholder="예) 10월 정기 점검" />
      </div>
      <div className="pms-form-grid">
        {sel('구분', 'category', TASK_CATEGORIES)}
        {sel('우선순위', 'priority', PRIORITIES)}
        <div className="field">
          <label>담당자<span className="req-mark"> *</span></label>
          <select value={t.assignee} onChange={(e) => set({ assignee: e.target.value })}>
            <option value="">선택</option>
            {MEMBERS.map((m) => <option key={m.name}>{m.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>기한<span className="req-mark"> *</span></label>
          <input type="date" value={t.due} onChange={(e) => set({ due: e.target.value })} />
        </div>
        {sel('상태', 'status', TASK_STATUSES)}
        {sel('반복', 'repeat', REPEATS)}
        {t.repeat !== '없음' && (
          <div className="field">
            <label>반복 종료일 <span className="opt-mark">(옵션)</span></label>
            <input type="date" value={t.repeatEnd ?? ''} onChange={(e) => set({ repeatEnd: e.target.value })} />
            <div className="pms-field-hint">비우면 운영 종료일({opsEnd || '-'})까지 반복합니다.</div>
          </div>
        )}
      </div>
      {t.repeat !== '없음' && <div className="pms-field-hint">"완료"로 바꾸면 다음 {t.repeat === '매주' ? '주' : t.repeat === '매월' ? '달' : '분기'} 업무가 자동으로 만들어집니다(상태 대기, 기한 +{t.repeat === '매주' ? '7일' : t.repeat === '매월' ? '1개월' : '3개월'}).</div>}
      <div className="field">
        <label>메모</label>
        <textarea rows={3} value={t.memo} onChange={(e) => set({ memo: e.target.value })} />
      </div>
      {t.incidentId && <p className="muted">연결된 운영 이슈: {t.incidentId}</p>}
      {t.prevTaskId && <p className="muted">반복으로 만들어진 업무 — 이전 업무 {t.prevTaskId}</p>}
      {err && <p style={{ color: '#c0392b' }}>{err}</p>}
      <button className="primary" disabled={!!err} onClick={async () => { onClose(); onSaved(await saveTask({ ...t, title: t.title.trim() })) }}>저장</button>
    </Modal>
  )
}

/** 운영 업무 관리 — 정기 점검·패치·보고 등 운영 중 프로젝트의 업무를 등록하고 진행 상태를 관리한다. */
export default function OpsTasksPage() {
  const { project, bar } = useOpsProject()
  const all = useOpsTasks()
  const [f, setF] = useState({ status: '', category: '', assignee: '', keyword: '', mine: false })
  const [edit, setEdit] = useState(null)
  const [notice, setNotice] = useState(null) // 반복 업무 자동 생성 결과 안내
  const set = (patch) => setF((p) => ({ ...p, ...patch }))

  const list = all.filter((t) => t.projectId === project?.id)
  const rows = list
    .filter((t) => (!f.status || t.status === f.status) && (!f.category || t.category === f.category) && (!f.assignee || t.assignee === f.assignee) && (!f.mine || t.assignee === ME) && (!f.keyword || `${t.title} ${t.memo}`.includes(f.keyword)))
    .sort((a, b) => (a.status === '완료') - (b.status === '완료') || a.due.localeCompare(b.due))
  const { sort, onSort, sorted } = useSort(rows, {
    id: (t) => t.id,
    category: (t) => t.category,
    title: (t) => t.title,
    priority: (t) => PRIORITIES.indexOf(t.priority),
    assignee: (t) => t.assignee,
    due: (t) => t.due,
    repeat: (t) => t.repeat,
    status: (t) => TASK_STATUSES.indexOf(t.status),
  })
  const open = list.filter((t) => t.status !== '완료')
  const showSpawn = (spawn) => setNotice(spawn ?? null)

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>운영 업무 관리</h2>
          <p className="page-description">운영 중인 프로젝트의 정기 점검, 패치·배포, 장애 조치, 요청 작업, 보고 같은 업무를 등록하고 담당자·기한·진행 상태를 관리합니다. 반복 업무와 운영 이슈에서 만든 업무도 여기서 봅니다.</p>
        </div>
        <button className="primary small" disabled={!project} onClick={() => setEdit(emptyTask(project.id, ME))}>＋ 업무 등록</button>
      </div>
      {bar}
      {edit && <TaskForm initial={edit} opsEnd={project?.operation?.endDate} onClose={() => setEdit(null)} onSaved={showSpawn} />}
      {notice && (
        <div className="pms-blind" style={{ display: 'flex', justifyContent: 'space-between', gap: 8, background: notice.created ? '#f1faf3' : '#fff4d6' }}>
          <span>{notice.created ? `🔁 반복 업무를 완료하여 다음 업무 ${notice.created.id}(${notice.created.title}, 기한 ${notice.created.due})를 자동 생성했습니다.` : `ℹ️ ${notice.skipped}`}</span>
          <button className="link-btn" onClick={() => setNotice(null)}>닫기</button>
        </div>
      )}
      {project && (
        <>
          <div className="pms-summary">
            <div className="pms-stat"><div className="label">전체 업무</div><div className="value">{list.length}</div></div>
            <div className="pms-stat"><div className="label">진행 중</div><div className="value">{list.filter((t) => t.status === '진행 중').length}</div></div>
            <div className={`pms-stat ${open.some(overdue) ? 'danger' : ''}`}><div className="label">기한 초과</div><div className="value">{open.filter(overdue).length}</div></div>
            <div className="pms-stat"><div className="label">7일 내 마감</div><div className="value">{open.filter((t) => t.due >= TODAY && (new Date(t.due) - new Date(TODAY)) / 86400000 <= 7).length}</div></div>
            <div className="pms-stat ok"><div className="label">완료</div><div className="value">{list.filter((t) => t.status === '완료').length}</div></div>
          </div>
          <div className="search-bar">
            <div className="search-field">
              <label>상태</label>
              <select value={f.status} onChange={(e) => set({ status: e.target.value })}>
                <option value="">전체</option>
                {TASK_STATUSES.map((s) => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="search-field">
              <label>구분</label>
              <select value={f.category} onChange={(e) => set({ category: e.target.value })}>
                <option value="">전체</option>
                {TASK_CATEGORIES.map((s) => <option key={s}>{s}</option>)}
              </select>
            </div>
            <div className="search-field">
              <label>담당자</label>
              <select value={f.assignee} onChange={(e) => set({ assignee: e.target.value })}>
                <option value="">전체</option>
                {MEMBERS.map((m) => <option key={m.name}>{m.name}</option>)}
              </select>
            </div>
            <div className="search-field">
              <label>검색</label>
              <input value={f.keyword} onChange={(e) => set({ keyword: e.target.value })} placeholder="업무명·메모" />
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <input type="checkbox" checked={f.mine} onChange={(e) => set({ mine: e.target.checked })} /> 내 업무만
            </label>
          </div>
          <table className="data-table pms-wbs">
            <thead>
              <tr><SortTh k="id" sort={sort} onSort={onSort}>코드</SortTh><SortTh k="category" sort={sort} onSort={onSort}>구분</SortTh><SortTh k="title" sort={sort} onSort={onSort}>업무명</SortTh><SortTh k="priority" sort={sort} onSort={onSort}>우선순위</SortTh><SortTh k="assignee" sort={sort} onSort={onSort}>담당자</SortTh><SortTh k="due" sort={sort} onSort={onSort}>기한</SortTh><SortTh k="repeat" sort={sort} onSort={onSort}>반복</SortTh><SortTh k="status" sort={sort} onSort={onSort}>상태</SortTh><th></th></tr>
            </thead>
            <tbody>
              {rows.length === 0 && <tr><td className="empty-cell" colSpan={9}>등록된 운영 업무가 없습니다.</td></tr>}
              {sorted.map((t) => (
                <tr key={t.id} style={t.status === '완료' ? { opacity: 0.6 } : undefined}>
                  <td>{t.id}</td>
                  <td>{t.category}</td>
                  <td className="name-cell">
                    <b>{t.title}</b>
                    {t.incidentId && <span className="pms-tag copy" style={{ marginLeft: 6 }}>이슈 {t.incidentId}</span>}
                    {t.autoCreated && <span className="pms-tag done" style={{ marginLeft: 6 }} title={`반복으로 자동 생성 (이전 업무 ${t.prevTaskId})`}>자동 생성</span>}
                    {t.memo && <div className="muted">{t.memo}</div>}
                  </td>
                  <td><span className={`pms-tag ${PRI_CLASS[t.priority]}`}>{t.priority}</span></td>
                  <td>{t.assignee}</td>
                  <td>{t.due} {t.status !== '완료' && <span className={`pms-tag ${overdue(t) ? 'late' : ''}`}>{dday(t.due)}</span>}</td>
                  <td>{t.repeat === '없음' ? <span className="muted">-</span> : <span className="pms-tag manual" title={t.repeatEnd ? `반복 종료일 ${t.repeatEnd}` : '운영 종료일까지'}>{t.repeat}</span>}</td>
                  <td>
                    <select value={t.status} onChange={async (e) => showSpawn(await setTaskStatus(t.id, e.target.value))}>
                      {TASK_STATUSES.map((s) => <option key={s}>{s}</option>)}
                    </select>
                  </td>
                  <td>
                    <button className="link-btn" onClick={() => setEdit(t)}>수정</button>{' '}
                    <button className="link-btn" onClick={() => window.confirm('이 업무를 삭제할까요?') && deleteTask(t.id)}>삭제</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pms-field-hint">반복 업무(매주·매월·분기)를 완료 처리하면 다음 주기 업무가 자동 생성됩니다(상태 대기, 기한 +7일/+1개월/+3개월, 담당자·우선순위 유지). 반복 종료일이나 운영 종료일을 넘으면 생성하지 않고, 같은 기한의 동일 업무가 있으면 중복 생성하지 않습니다.</div>
        </>
      )}
    </div>
  )
}
