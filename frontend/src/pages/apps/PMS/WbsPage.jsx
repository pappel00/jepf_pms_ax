import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useAuth } from '../../../context/AuthContext'
import Modal from '../../../components/Modal'
import './pms.css'
import { stageSummary, useDocs } from './docStore'
import { issueStats, useIssues } from './issueStore'
import { useProjects } from './projectStore'
import StageDocsModal from './StageDocsModal'
import TaskIssuesModal from './TaskIssuesModal'
import { isPartner } from './teamConfig'
import {
  CHAIN,
  CHAIN_RATIO,
  MEMBERS,
  TODAY,
  WBS_DEFAULT_PROJECT_ID,
  blockedBy,
  ME,
  copyTask,
  createTask,
  deleteTask,
  updateTask,
  featureMatrix,
  flattenTree,
  isLeaf,
  progressOf,
  projectProgress,
  rangeOf,
  setProgress,
  statusOf,
  targetProgressOf,
  useTasks,
} from './pmsStore'

const STATUS_CLASS = { 완료: 'done', 지연: 'late', 진행중: 'doing', 미착수: '' }

function MatrixView({ all }) {
  const rows = featureMatrix(all)
  const categories = [...new Set(rows.map((r) => r.category))]
  return (
    <div>
      <div className="pms-hint">
        • 같은 기능(요구사항)이 <b>분석 → 설계 → 개발 → 테스트</b> 4개 단계에 각각 업무로 들어가 있고, 앞 단계가 끝나야 다음
        단계를 진행할 수 있습니다.
        <br />• 기능 전체 진척은 단계 비율(분석 {CHAIN_RATIO.분석} / 설계 {CHAIN_RATIO.설계} / 개발 {CHAIN_RATIO.개발} / 테스트{' '}
        {CHAIN_RATIO.테스트})로 가중평균합니다. 진척 입력은 「단계별 트리」에서 합니다.
      </div>
      <table className="data-table pms-wbs pms-matrix">
        <thead>
          <tr>
            <th>분류</th>
            <th>기능 (요구사항)</th>
            {CHAIN.map((ph, i) => (
              <th key={ph}>
                {i + 1}. {ph}
              </th>
            ))}
            <th>기능 전체</th>
          </tr>
        </thead>
        <tbody>
          {categories.flatMap((cat) =>
            rows
              .filter((r) => r.category === cat)
              .map((r, idx) => (
                <tr key={r.reqId}>
                  <td>{idx === 0 ? <b>{cat}</b> : ''}</td>
                  <td>
                    <span className="pms-tag req" style={{ marginRight: 6 }}>
                      {r.reqId}
                    </span>
                    {r.feature}
                  </td>
                  {CHAIN.map((ph) => {
                    const c = r.cells[ph]
                    if (!c) return <td key={ph}>—</td>
                    return (
                      <td key={ph} className="matrix-cell">
                        <div className="pms-bar-row">
                          <div className={`pms-bar ${c.status === '지연' ? 'late' : c.progress >= 100 ? 'done' : ''}`}>
                            <span style={{ width: `${c.progress}%` }} />
                          </div>
                          <span className="pms-pct">{c.progress}%</span>
                        </div>
                        <div className="matrix-sub">
                          {c.assignees.map((a) => a || '미배정').join(', ')} · {c.start.slice(5)}~{c.end.slice(5)}
                        </div>
                        <span className={`pms-tag ${STATUS_CLASS[c.status]}`}>{c.status}</span>
                        {c.blocked && (
                          <span className="pms-tag late" style={{ marginLeft: 4 }}>
                            선행 대기
                          </span>
                        )}
                      </td>
                    )
                  })}
                  <td>
                    <b>{r.overall}%</b>
                  </td>
                </tr>
              )),
          )}
        </tbody>
      </table>
    </div>
  )
}

/**
 * WBS / 진척 관리 — 프로젝트별 WBS. WBS가 있는 프로젝트(기본 프로젝트 + 단계전환으로 생성된 프로젝트)를 고를 수 있다.
 * ?project=프로젝트ID 또는 ?task=업무ID 로 들어오면 해당 프로젝트를 먼저 연다.
 */
export default function WbsPage() {
  const allTasks = useTasks()
  const [params] = useSearchParams()
  const [projectId, setProjectId] = useState(
    () => params.get('project') || allTasks.find((t) => t.id === params.get('task'))?.projectId || WBS_DEFAULT_PROJECT_ID,
  )
  return <WbsView key={projectId} projectId={projectId} onProjectChange={setProjectId} />
}

/** 업무 추가·수정 — 하위 업무는 기간·담당자·선행 업무까지, 단계·분류는 이름과 가중치만 다룬다. */
function TaskFormModal({ mode, task, defaultParentId, all, assignable, issueCount, onSubmit, onClose }) {
  const isAdd = mode === 'add'
  const leafEdit = isAdd || isLeaf(all, task)
  const pathOf = (t) => {
    const names = []
    for (let x = t; x; x = all.find((y) => y.id === x.parentId)) names.unshift(x.name)
    return names.join(' › ')
  }
  const parents = all.filter((t) => !isLeaf(all, t) || (t.id === defaultParentId))
  const parent0 = all.find((t) => t.id === defaultParentId)
  const range0 = parent0 ? rangeOf(all, parent0) : {}
  const [f, setF] = useState(
    isAdd
      ? { parentId: defaultParentId ?? parents[0]?.id ?? '', name: '', start: range0.start ?? TODAY, end: range0.end ?? TODAY, weight: 10, assignee: '', predId: '' }
      : { parentId: task.parentId, name: task.name, start: task.start ?? '', end: task.end ?? '', weight: task.weight, assignee: task.assignee ?? '', predId: task.predId ?? '' },
  )
  const [saving, setSaving] = useState(false)
  const set = (patch) => setF((p) => ({ ...p, ...patch }))
  const siblings = all.filter((t) => t.parentId === f.parentId && (isAdd || t.id !== task.id))
  const siblingSum = siblings.reduce((s, t) => s + (t.weight || 0), 0)
  const total = Math.round((siblingSum + (Number(f.weight) || 0)) * 10) / 10
  const preds = all.filter((t) => isLeaf(all, t) && (isAdd || t.id !== task.id))
  const err = !f.name.trim()
    ? '업무명을 입력하세요.'
    : leafEdit && (!f.start || !f.end)
      ? '시작일과 종료일을 입력하세요.'
      : leafEdit && f.end < f.start
        ? '종료일은 시작일보다 빠를 수 없습니다.'
        : !(Number(f.weight) >= 0)
          ? '가중치를 입력하세요.'
          : ''
  const submit = async () => {
    setSaving(true)
    try {
      await onSubmit({ ...f, name: f.name.trim() })
      onClose()
    } catch (e) {
      window.alert(e.message)
    } finally {
      setSaving(false)
    }
  }
  return (
    <Modal title={isAdd ? '업무 추가' : `업무 수정 — ${task.name}`} onClose={onClose} width={560}>
      {isAdd && (
        <div className="field">
          <label>
            상위 항목 <span className="req-mark">*</span>
          </label>
          <select value={f.parentId} onChange={(e) => set({ parentId: e.target.value })}>
            {parents.map((t) => (
              <option key={t.id} value={t.id}>
                {pathOf(t)}
              </option>
            ))}
          </select>
        </div>
      )}
      <div className="field">
        <label>
          업무명 <span className="req-mark">*</span>
        </label>
        <input value={f.name} onChange={(e) => set({ name: e.target.value })} />
      </div>
      {leafEdit && (
        <>
          <div className="field">
            <label>
              시작일 <span className="req-mark">*</span>
            </label>
            <input type="date" value={f.start} onChange={(e) => set({ start: e.target.value })} />
          </div>
          <div className="field">
            <label>
              종료일 <span className="req-mark">*</span>
            </label>
            <input type="date" value={f.end} onChange={(e) => set({ end: e.target.value })} />
          </div>
          <div className="field">
            <label>담당자</label>
            <select value={f.assignee} onChange={(e) => set({ assignee: e.target.value })}>
              <option value="">미배정</option>
              {assignable.map((m) => (
                <option key={m.name} value={m.name}>
                  {m.name} ({m.org})
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label>선행 업무</label>
            <select value={f.predId} onChange={(e) => set({ predId: e.target.value })}>
              <option value="">없음</option>
              {preds.map((t) => (
                <option key={t.id} value={t.id}>
                  {pathOf(t)}
                </option>
              ))}
            </select>
          </div>
        </>
      )}
      <div className="field">
        <label>가중치</label>
        <input type="number" min={0} step={0.1} value={f.weight} onChange={(e) => set({ weight: e.target.value })} />
        <div className="pms-field-hint">
          같은 상위 항목 아래 형제 업무 {siblings.length}건의 가중치 합계 {Math.round(siblingSum * 10) / 10} → 저장 후 {total}. 가중치는 형제끼리의 비율로 쓰이므로 합계가 100이 아니어도 저장됩니다.
        </div>
      </div>
      {!isAdd && leafEdit && issueCount > 0 && (
        <div className="pms-hint" style={{ boxShadow: 'none', background: '#fff4d6' }}>
          이 업무에 이슈 {issueCount}건이 연결되어 있습니다. 기간을 바꾸면 이슈의 조치 기한·일정 영향도 함께 확인하세요.
        </div>
      )}
      {!isAdd && leafEdit && <div className="pms-field-hint">담당자를 바꿔도 이미 입력된 진척({task.progress ?? 0}%)은 그대로 유지됩니다.</div>}
      {err && <p style={{ color: '#c0392b' }}>{err}</p>}
      <button className="primary" disabled={!!err || saving} onClick={submit}>
        저장
      </button>
    </Modal>
  )
}

function WbsView({ projectId, onProjectChange }) {
  const allTasks = useTasks()
  const projects = useProjects()
  const all = useMemo(() => allTasks.filter((t) => t.projectId === projectId), [allTasks, projectId])
  const wbsProjects = projects.filter((p) => allTasks.some((t) => t.projectId === p.id))
  const [view, setView] = useState('tree')
  const [expanded, setExpanded] = useState(() => new Set(all.filter((t) => !isLeaf(all, t)).map((t) => t.id)))
  const issues = useIssues() // 이슈가 바뀌면 행의 이슈 표시도 다시 그린다
  const [issueTaskId, setIssueTaskId] = useState(null) // 이슈 모달을 열 업무
  const [params, setParams] = useSearchParams()
  const [highlightId, setHighlightId] = useState(null)
  const docs = useDocs() // 단계말 산출물 등록 현황이 바뀌면 행의 "산출물 n/m"도 다시 그린다
  const [stageDocs, setStageDocs] = useState(null) // 산출물 관리를 열 단계명
  const [copying, setCopying] = useState(null)
  const [taskForm, setTaskForm] = useState(null) // { mode: 'add' | 'edit', task?, parentId? }
  const { auth } = useAuth()
  const [copyAssignee, setCopyAssignee] = useState(MEMBERS[0].name)

  // 이슈 관리에서 "WBS ›"로 들어오면(?task=업무ID) 해당 업무까지 트리를 펼치고 강조한다.
  const taskParam = params.get('task')
  useEffect(() => {
    if (!taskParam) return
    const target = all.find((t) => t.id === taskParam)
    if (!target) return
    setView('tree')
    setExpanded((prev) => {
      const next = new Set(prev)
      for (let p = all.find((t) => t.id === target.parentId); p; p = all.find((t) => t.id === p.parentId)) next.add(p.id)
      return next
    })
    setHighlightId(taskParam)
    setParams({}, { replace: true })
    // 쿼리 파라미터를 지우면 이 effect가 다시 돌므로 타이머는 정리하지 않고 그대로 둔다.
    setTimeout(() => document.getElementById(`wbs-row-${taskParam}`)?.scrollIntoView({ block: 'center' }), 150)
    setTimeout(() => setHighlightId(null), 5000)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [taskParam])

  const leafIdsUnder = (task) => {
    const kids = all.filter((t) => t.parentId === task.id)
    return kids.length === 0 ? [task.id] : kids.flatMap(leafIdsUnder)
  }
  const issueTask = issueTaskId ? all.find((t) => t.id === issueTaskId) : null

  const rows = flattenTree(all, expanded)
  const overall = projectProgress(all)
  const roots = all.filter((t) => !t.parentId)
  const rootWeight = roots.reduce((s, t) => s + t.weight, 0)
  const overallTarget =
    Math.round((roots.reduce((s, t) => s + targetProgressOf(all, t) * t.weight, 0) / rootWeight) * 10) / 10

  const toggle = (id) =>
    setExpanded((prev) => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })

  const expandAll = () => setExpanded(new Set(all.filter((t) => !isLeaf(all, t)).map((t) => t.id)))
  const collapseAll = () => setExpanded(new Set())
  // 단계(Level1)만 펼쳐서 분류 목록까지만 보기
  const expandPhases = () => setExpanded(new Set(roots.map((t) => t.id)))

  const onProgress = (task, value) => {
    const res = setProgress(task.id, value)
    if (!res.ok) window.alert(`선행 업무 "${res.pred.name}"이(가) 완료되어야 진행할 수 있습니다.`)
  }

  // 업무 복사 시 담당자 후보 = 프로젝트 관리자 + 투입 인력(프로젝트 멤버). 수행 정보가 없으면 전체 인력.
  const exec = projects.find((p) => p.id === projectId)?.transitioned
  const assignable = exec ? MEMBERS.filter((m) => m.name === exec.manager.name || exec.team.some((t) => t.name === m.name)) : MEMBERS
  const openCopy = (task) => {
    setCopying(task)
    setCopyAssignee((assignable.find((m) => m.name !== task.assignee) ?? assignable[0] ?? MEMBERS[0]).name)
  }

  // 추가·수정·삭제는 프로젝트 관리자(수행 관리자·수행 PM)와 관리자만 — 서버도 같은 규칙으로 검사한다.
  const project = projects.find((p) => p.id === projectId)
  const canEdit = (auth?.roles ?? []).some((r) => ['ROLE_ADMIN', 'ROLE_SYSTEM_ADMIN'].includes(r)) || exec?.manager?.name === ME || project?.pmName === ME
  const onDelete = (task) => {
    if (!window.confirm(`「${task.name}」 업무를 삭제할까요?\n같은 상위 항목의 남은 업무는 가중치 합계가 유지되도록 비율대로 다시 맞춥니다.`)) return
    deleteTask(task.id).catch((e) => window.alert(`삭제하지 못했습니다: ${e.message}`))
  }

  const groupOf = (task) => {
    const rootId = task.copiedFromId ?? task.id
    return all.filter((t) => t.id === rootId || t.copiedFromId === rootId)
  }
  const groupSize = copying ? groupOf(copying).length : 0
  const groupWeight = copying ? groupOf(copying).reduce((s, t) => s + t.weight, 0) : 0

  const nameOf = (id) => all.find((t) => t.id === id)?.name

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            WBS / 진척 관리
          </h2>
          <p className="page-description">
            단계(사업관리 · 분석 · 설계 · 개발 · 테스트 · 이행 · 준공) → 분류 → 작업 구조입니다. 하나의 기능은 분석 · 설계
            · 개발 · 테스트 단계마다 업무가 하나씩 포함됩니다.
          </p>
        </div>
      </div>

      <div className="pms-toolbar">
        <select value={projectId} onChange={(e) => onProjectChange(e.target.value)}>
          {wbsProjects.map((p) => (
            <option key={p.id} value={p.id}>
              {p.id} · {p.name} ({p.status})
            </option>
          ))}
        </select>
        <div className="pms-seg">
          <button className={view === 'tree' ? 'on' : ''} onClick={() => setView('tree')}>
            단계별 트리
          </button>
          <button className={view === 'matrix' ? 'on' : ''} onClick={() => setView('matrix')}>
            기능 × 단계
          </button>
        </div>
        {view === 'tree' && (
          <>
            <button className="secondary small" onClick={expandAll}>
              모두 펼치기
            </button>
            <button className="secondary small" onClick={expandPhases}>
              분류까지
            </button>
            <button className="secondary small" onClick={collapseAll}>
              모두 접기
            </button>
          </>
        )}
        {canEdit && (
          <button className="primary small" onClick={() => setTaskForm({ mode: 'add' })}>
            + 업무 추가
          </button>
        )}
      </div>

      <div className="pms-summary">
        <div className="pms-stat">
          <div className="label">전체 실적 공정율</div>
          <div className="value">{overall}%</div>
        </div>
        <div className="pms-stat">
          <div className="label">기준일({TODAY}) 목표 공정율</div>
          <div className="value">{overallTarget}%</div>
        </div>
        <div className={`pms-stat ${overall < overallTarget ? 'danger' : 'ok'}`}>
          <div className="label">계획 대비</div>
          <div className="value">
            {overall - overallTarget >= 0 ? '+' : ''}
            {Math.round((overall - overallTarget) * 10) / 10}%p
          </div>
        </div>
      </div>

      {view === 'matrix' ? (
        <MatrixView all={all} />
      ) : (
        <>
          <div className="pms-hint">
            • <b>리프(최하위) 작업만</b> 실적 공정율을 직접 입력합니다. 상위(분류/단계)는 하위의 <b>가중평균으로 자동 계산</b>됩니다.
            <br />• 기능 업무는 <b>선행</b> 단계(분석 → 설계 → 개발 → 테스트)가 100%여야 진척을 입력할 수 있습니다.
            <br />• 업무당 담당자는 1명입니다. 여러 명이 필요하면 <b>복사</b>로 담당자별 업무를 만들며, 가중치는 균등 분배됩니다.
            <br />• 각 업무 행의 <b>⚠ 이슈</b> 버튼에서 그 업무에 연결된 이슈를 조회·등록·연결합니다(이슈 관리 화면과 양방향 연결). 상위 행의 ⚠ 표시는 하위 업무 이슈 합계입니다.
            <br />• <b>단계말 검토</b> 행의 <b>📄 산출물</b> 버튼에서 해당 단계의 문서(문서 테일러링 적용분)를 등록·수정·다운로드합니다.
            <br />• <span className="pms-tag req">REQ</span> 요구사항(기능)에서 자동 생성 / <span className="pms-tag manual">수동</span> PM이 직접 등록
          </div>

          <table className="data-table pms-wbs">
            <thead>
              <tr>
                <th>WBS</th>
                <th>작업</th>
                <th>구분</th>
                <th>선행</th>
                <th>담당자</th>
                <th>시작(계획)</th>
                <th>종료(계획)</th>
                <th>가중치</th>
                <th>목표%</th>
                <th>실적%</th>
                <th>상태</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {rows.map(({ task, no, depth }) => {
                const leaf = isLeaf(all, task)
                const { start, end } = rangeOf(all, task)
                const status = statusOf(all, task)
                const p = progressOf(all, task)
                const blocker = leaf ? blockedBy(all, task) : null
                const is = issueStats(issues, leafIdsUnder(task))
                return (
                  <tr
                    key={task.id}
                    id={`wbs-row-${task.id}`}
                    className={`${leaf ? '' : depth === 0 ? 'phase-row' : 'group-row'} ${highlightId === task.id ? 'row-flash' : ''}`}
                  >
                    <td>{no}</td>
                    <td className="name-cell" style={{ paddingLeft: 14 + depth * 22 }}>
                      {leaf ? (
                        <span style={{ display: 'inline-block', width: 18 }} />
                      ) : (
                        <button className="toggle" onClick={() => toggle(task.id)}>
                          {expanded.has(task.id) ? '▼' : '▶'}
                        </button>
                      )}
                      {task.name}
                      {is.total > 0 && (
                        <span
                          className={`pms-tag ${is.high > 0 ? 'late' : is.open > 0 ? 'manual' : 'done'}`}
                          style={{ marginLeft: 8, cursor: leaf ? 'pointer' : 'default' }}
                          title={`이슈 미해결 ${is.open}건 / 전체 ${is.total}건${is.high ? ` (긴급·높음 ${is.high}건)` : ''}`}
                          onClick={() => leaf && setIssueTaskId(task.id)}
                        >
                          ⚠ {is.open > 0 ? `이슈 ${is.open}` : `이슈 해결 ${is.total}`}
                        </span>
                      )}
                      {task.copiedFromId && (
                        <span className="pms-tag copy" style={{ marginLeft: 8 }}>
                          복사
                        </span>
                      )}
                    </td>
                    <td>
                      {leaf &&
                        (task.reqId ? (
                          <span className="pms-tag req">{task.reqId}</span>
                        ) : (
                          <span className="pms-tag manual">수동</span>
                        ))}
                    </td>
                    <td>
                      {leaf && task.predId ? (
                        <span className={`pms-tag ${blocker ? 'late' : 'done'}`} title={nameOf(task.predId)}>
                          {blocker ? '대기 · ' : '완료 · '}
                          {nameOf(task.predId)?.split(' ').slice(-1)[0]}
                        </span>
                      ) : (
                        ''
                      )}
                    </td>
                    <td>
                      {leaf ? (
                        task.assignee ? (
                          <>
                            {task.assignee}
                            {isPartner(task.assignee) && (
                              <span className="pms-tag manual" style={{ marginLeft: 4 }} title="협력사 — 내 업무 알림으로 진척·이슈 처리(관리자가 대신 입력 가능)">
                                협력
                              </span>
                            )}
                          </>
                        ) : (
                          <span className="pms-tag manual">미배정</span>
                        )
                      ) : (
                        '—'
                      )}
                    </td>
                    <td>{start}</td>
                    <td>{end}</td>
                    <td>{task.weight}</td>
                    <td className="auto-val">{targetProgressOf(all, task)}</td>
                    <td>
                      {leaf ? (
                        <input
                          type="number"
                          min="0"
                          max="100"
                          step="5"
                          value={task.progress}
                          disabled={!!blocker}
                          title={blocker ? `선행 업무 "${blocker.name}" 완료 후 입력 가능` : ''}
                          onChange={(e) => onProgress(task, e.target.value)}
                        />
                      ) : (
                        <span className="auto-val" title="하위 가중평균 (자동)">
                          {p} <span className="muted">자동</span>
                        </span>
                      )}
                    </td>
                    <td>
                      <span className={`pms-tag ${STATUS_CLASS[status]}`}>{status}</span>
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>
                      {leaf && task.category === '단계말 검토' && (
                        <button
                          className="pms-docs-btn"
                          title="이 단계의 산출물(문서 테일러링 적용 문서)을 등록·수정·다운로드합니다"
                          onClick={() => setStageDocs(task.phase)}
                        >
                          📄 산출물 {stageSummary(docs, projectId, task.phase).registered}/
                          {stageSummary(docs, projectId, task.phase).total}
                        </button>
                      )}
                      {leaf && (
                        <button
                          className="pms-docs-btn"
                          style={{ background: is.open ? '#fff4d6' : undefined, borderColor: is.open ? '#ecd48b' : undefined, color: is.open ? '#8a6100' : undefined }}
                          title="이 업무에 연결된 이슈를 조회·등록·연결합니다"
                          onClick={() => setIssueTaskId(task.id)}
                        >
                          ⚠ 이슈 {is.total > 0 ? `${is.open}/${is.total}` : '+'}
                        </button>
                      )}
                      {leaf && (
                        <button className="link-btn" onClick={() => openCopy(task)}>
                          복사
                        </button>
                      )}
                      {canEdit && (
                        <button className="link-btn" onClick={() => setTaskForm({ mode: 'edit', task })}>
                          수정
                        </button>
                      )}
                      {canEdit && leaf && (
                        <button className="link-btn" onClick={() => onDelete(task)}>
                          삭제
                        </button>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </>
      )}

      {issueTask && <TaskIssuesModal key={issueTask.id} task={issueTask} onClose={() => setIssueTaskId(null)} />}

      {stageDocs && <StageDocsModal projectId={projectId} phase={stageDocs} onClose={() => setStageDocs(null)} />}

      {taskForm && (
        <TaskFormModal
          key={taskForm.task?.id ?? 'add'}
          mode={taskForm.mode}
          task={taskForm.task}
          defaultParentId={taskForm.parentId}
          all={all}
          assignable={assignable}
          issueCount={taskForm.task ? issueStats(issues, leafIdsUnder(taskForm.task)).total : 0}
          onSubmit={(form) => (taskForm.mode === 'add' ? createTask(project, form) : updateTask(taskForm.task.id, form))}
          onClose={() => setTaskForm(null)}
        />
      )}

      {copying && (
        <Modal title="업무 복사" onClose={() => setCopying(null)}>
          <p style={{ marginTop: 0, fontSize: 13 }}>
            <b>{copying.name}</b> 업무를 다른 담당자용으로 복사합니다. 진척률은 0%로 시작합니다.
          </p>
          <div className="field">
            <label>담당자</label>
            <select value={copyAssignee} onChange={(e) => setCopyAssignee(e.target.value)}>
              {assignable.map((m) => (
                <option key={m.name} value={m.name}>
                  {m.name} ({exec?.team.find((t) => t.name === m.name)?.role ?? (m.name === exec?.manager.name ? exec.manager.role : m.role)} · {m.org})
                </option>
              ))}
            </select>
          </div>
          <div className="pms-hint" style={{ boxShadow: 'none', background: '#f4f5f7' }}>
            가중치 균등 분배: 현재 합계 {groupWeight} → 복사 후 {groupSize + 1}건이 각{' '}
            {Math.round((groupWeight / (groupSize + 1)) * 10) / 10}씩
          </div>
          <button
            className="primary"
            onClick={() => {
              copyTask(copying.id, copyAssignee)
              setCopying(null)
            }}
          >
            복사
          </button>
        </Modal>
      )}
    </div>
  )
}
