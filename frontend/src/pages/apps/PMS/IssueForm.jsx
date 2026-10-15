import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { ISSUE_STATUSES, ISSUE_TYPES, PRIORITIES, addHistory, isOpenStatus, saveIssue } from './issueStore'
import { ME, MEMBERS, TODAY, isLeaf, useTasks } from './pmsStore'
import { useProjects } from './projectStore'

function Field({ label, required, conditional, error, hint, children, wide }) {
  return (
    <div className={`field pms-field ${wide ? 'wide' : ''}`}>
      <label>
        {label}
        {required && <span className="req-mark"> *</span>}
        {conditional && <span className="cond-mark"> (조건부 필수)</span>}
        {!required && !conditional && <span className="opt-mark"> (옵션)</span>}
      </label>
      {children}
      {hint && <div className="pms-field-hint">{hint}</div>}
      {error && <div className="pms-field-error">{error}</div>}
    </div>
  )
}

/** 연결 업무 선택 목록 — 리프 업무를 단계별로 묶어서 보여준다. */
export function TaskOptions({ all, projectId: onlyProject }) {
  const projects = useProjects()
  // 프로젝트 → 단계 순으로 묶는다(WBS가 여러 프로젝트에 있을 수 있음). onlyProject가 있으면 그 프로젝트 업무만.
  const groups = all
    .filter((t) => !t.parentId && (!onlyProject || t.projectId === onlyProject))
    .map((r) => ({ key: r.id, projectId: r.projectId, phase: r.name }))
  return groups.map(({ key, projectId, phase: ph }) => {
    const leaves = all.filter((t) => t.projectId === projectId && t.phase === ph && isLeaf(all, t))
    if (leaves.length === 0) return null
    const pName = projects.find((p) => p.id === projectId)?.name
    return (
      <optgroup key={key} label={`${pName ? `${pName} · ` : ''}${ph} 단계`}>
        {leaves.map((t) => (
          <option key={t.id} value={t.id}>
            {t.name} ({t.assignee || '미배정'})
          </option>
        ))}
      </optgroup>
    )
  })
}

/**
 * 이슈 등록/수정 폼. 이슈 관리 화면과 WBS 업무의 이슈 모달에서 같이 쓴다.
 * lockTask가 true면 연결 업무를 바꿀 수 없다(WBS 업무에서 등록하는 경우).
 */
export default function IssueForm({ initial, isNew, lockTask, projects: projectOptions, onClose, onSaved }) {
  const all = useTasks()
  const allProjects = useProjects()
  // 프로젝트 선택지: 조회 범위(참여 중인 수행 프로젝트)가 넘어오면 그것을, 아니면 이슈의 프로젝트만.
  const options = projectOptions ?? allProjects.filter((p) => p.id === initial.projectId)
  const [it, setIt] = useState(initial)
  const [errors, setErrors] = useState({})
  const [note, setNote] = useState('')
  const set = (k, v) => setIt((p) => ({ ...p, [k]: v }))
  const task = all.find((t) => t.id === it.taskId)
  const needResolution = !isOpenStatus(it.status)

  const submit = () => {
    const e = {}
    if (!it.projectId) e.projectId = '프로젝트를 선택하세요.'
    if (!it.title.trim()) e.title = '제목을 입력하세요.'
    if (!it.content.trim()) e.content = '이슈 내용을 입력하세요.'
    if (!it.assignee) e.assignee = '담당자를 선택하세요.'
    if (needResolution && !it.resolution.trim()) e.resolution = '해결/종결 시 조치 내용을 기록하세요.'
    setErrors(e)
    if (Object.keys(e).length) return
    saveIssue({ ...it, title: it.title.trim(), taskId: it.taskId || null }, ME, TODAY)
    onSaved?.(it.id)
    onClose()
  }

  return (
    <Modal title={isNew ? '이슈 등록' : `이슈 수정 — ${it.id}`} onClose={onClose} width={780}>
      <div className="pms-form-grid">
        <Field label="프로젝트" required error={errors.projectId} hint={lockTask || !isNew ? '등록된 이슈의 프로젝트는 변경할 수 없습니다' : '연결 업무는 선택한 프로젝트의 WBS 업무에서 고릅니다'} wide>
          <select
            value={it.projectId ?? ''}
            disabled={!isNew || lockTask}
            onChange={(e) => setIt((p) => ({ ...p, projectId: e.target.value, taskId: null }))}
          >
            <option value="">선택하세요</option>
            {options.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.id})
              </option>
            ))}
          </select>
        </Field>
        <Field label="제목" required error={errors.title} wide>
          <input value={it.title} onChange={(e) => set('title', e.target.value)} />
        </Field>
        <Field label="유형" required>
          <select value={it.type} onChange={(e) => set('type', e.target.value)}>
            {ISSUE_TYPES.map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </Field>
        <Field label="우선순위" required>
          <select value={it.priority} onChange={(e) => set('priority', e.target.value)}>
            {PRIORITIES.map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </Field>
        <Field label="상태" required>
          <select value={it.status} onChange={(e) => set('status', e.target.value)}>
            {ISSUE_STATUSES.map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </Field>
        <Field label="담당자" required error={errors.assignee}>
          <select value={it.assignee} onChange={(e) => set('assignee', e.target.value)}>
            <option value="">선택하세요</option>
            {MEMBERS.map((m) => (
              <option key={m.name} value={m.name}>
                {m.name} ({m.role})
              </option>
            ))}
          </select>
        </Field>
        <Field label="조치 기한">
          <input type="date" value={it.dueDate} onChange={(e) => set('dueDate', e.target.value)} />
        </Field>
        <Field label="일정 영향 (일)" hint="이슈로 인해 예상되는 일정 지연 일수">
          <input type="number" min="0" value={it.impactDays ?? 0} onChange={(e) => set('impactDays', Number(e.target.value))} />
        </Field>
        <Field
          label="연결 업무 (WBS)"
          hint={task ? `${task.phase} 단계 · 담당 ${task.assignee || '미배정'} · ${task.start} ~ ${task.end}` : '특정 업무와 무관하면 연결하지 않아도 됩니다 (프로젝트 공통 이슈)'}
          wide
        >
          <select value={it.taskId ?? ''} disabled={lockTask} onChange={(e) => set('taskId', e.target.value || null)}>
            <option value="">연결 안 함 (프로젝트 공통)</option>
            <TaskOptions all={all} projectId={it.projectId} />
          </select>
        </Field>
        <Field label="이슈 내용" required error={errors.content} wide>
          <textarea rows={3} value={it.content} onChange={(e) => set('content', e.target.value)} />
        </Field>
        <Field label="조치 내용 / 해결 방안" conditional={needResolution} error={errors.resolution} wide>
          <textarea rows={2} value={it.resolution} onChange={(e) => set('resolution', e.target.value)} />
        </Field>
      </div>

      {!isNew && (
        <>
          <div className="pms-form-section">처리 이력</div>
          {[...it.history].reverse().map((h, i) => (
            <div key={i} style={{ display: 'flex', gap: 12, fontSize: 12, padding: '3px 0' }}>
              <span style={{ width: 80, color: '#888' }}>{h.date}</span>
              <span style={{ width: 54, fontWeight: 600 }}>{h.author}</span>
              <span>{h.text}</span>
            </div>
          ))}
          <div className="mapping-add-row" style={{ marginTop: 8 }}>
            <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="처리 내용 메모 추가" />
            <button
              className="secondary small"
              onClick={() => {
                if (!note.trim()) return
                addHistory(it.id, note.trim(), ME, TODAY)
                setIt((p) => ({ ...p, history: [...p.history, { date: TODAY, author: ME, text: note.trim() }] }))
                setNote('')
              }}
            >
              추가
            </button>
          </div>
        </>
      )}

      <div style={{ display: 'flex', gap: 8, marginTop: 14 }}>
        <button className="primary" style={{ flex: 1 }} onClick={submit}>
          저장
        </button>
        <button className="secondary" style={{ marginTop: 0 }} onClick={onClose}>
          취소
        </button>
      </div>
    </Modal>
  )
}
