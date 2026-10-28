import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import './pms.css'
import IssueForm from './IssueForm'
import { ProcessButton } from './ProcessModal'
import ScheduleProposalPanel from './ScheduleProposalPanel'
import { issueProcess } from './processModels'
import { ISSUE_STATUSES, ISSUE_TYPES, PRIORITIES, isHighPriority, isOpenStatus, nextIssueId, useIssues } from './issueStore'
import { useAuth } from '../../../context/AuthContext'
import { canSeeAllProjects, issueProjects } from './participation'
import { ME, MEMBERS, TODAY, useTasks } from './pmsStore'
import { useProjects } from './projectStore'

const STATUS_CLASS = { 오픈: 'late', '진행 중': 'doing', 해결: 'done', 종결: '' }
const PRIORITY_CLASS = { 긴급: 'late', 높음: 'manual', 보통: '', 낮음: '' }
const dday = (date) => {
  const diff = Math.round((new Date(date) - new Date(TODAY)) / 86400000)
  return diff === 0 ? 'D-Day' : diff > 0 ? `D-${diff}` : `D+${-diff}`
}

const newIssue = (all, projectId, taskId = null) => ({
  id: nextIssueId(all), projectId, title: '', type: ISSUE_TYPES[0], priority: '보통', status: '오픈',
  reporter: ME, assignee: '', registeredDate: TODAY, dueDate: '', resolvedDate: '', taskIds: taskId ? [taskId] : [],
  impactDays: 0, content: '', resolution: '', history: [],
})

/**
 * 이슈 관리 — WBS 업무와 양방향으로 연결된다(이슈 → 업무: "WBS ›", 업무 → 이슈: WBS 행의 이슈 버튼).
 * 프로젝트 조회 범위: 내가 참여 중인 수행 프로젝트(센터장·admin은 수행 중인 전체 프로젝트).
 */
export default function IssuesPage() {
  const allIssues = useIssues()
  const tasks = useTasks()
  const projects = useProjects()
  const { auth } = useAuth()
  const navigate = useNavigate()
  const [params, setParams] = useSearchParams()
  const seeAll = canSeeAllProjects(auth)
  const scopeProjects = issueProjects(projects, auth, ME)
  const scopeIds = new Set(scopeProjects.map((p) => p.id))
  const issues = allIssues.filter((i) => scopeIds.has(i.projectId)) // 조회 범위 안의 이슈만
  const projectName = (id) => projects.find((p) => p.id === id)?.name ?? id
  const [projectFilter, setProjectFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [assigneeFilter, setAssigneeFilter] = useState('')
  const [linkFilter, setLinkFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [form, setForm] = useState(null) // { issue, isNew }
  const taskParam = params.get('task') // WBS에서 "이 업무의 이슈 모두 보기"로 들어온 경우
  const issueParam = params.get('issue') // WBS 이슈 모달에서 "열기"로 들어온 경우

  useEffect(() => {
    if (issueParam) {
      const found = issues.find((i) => i.id === issueParam)
      if (found) setForm({ issue: found, isNew: false })
      setParams((p) => {
        p.delete('issue')
        return p
      }, { replace: true })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [issueParam])

  const taskName = (id) => tasks.find((t) => t.id === id)
  const open = issues.filter((i) => isOpenStatus(i.status))
  const overdue = open.filter((i) => i.dueDate && i.dueDate < TODAY)
  const filtered = issues.filter(
    (i) =>
      (!projectFilter || i.projectId === projectFilter) &&
      (!taskParam || i.taskIds.includes(taskParam)) &&
      (!statusFilter || i.status === statusFilter) &&
      (!priorityFilter || i.priority === priorityFilter) &&
      (!typeFilter || i.type === typeFilter) &&
      (!assigneeFilter || i.assignee === assigneeFilter) &&
      (!linkFilter || (linkFilter === 'linked' ? i.taskIds.length > 0 : i.taskIds.length === 0)) &&
      (!keyword || `${i.id} ${i.title} ${i.content}`.toLowerCase().includes(keyword.toLowerCase())),
  )

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            이슈 관리
          </h2>
          <p className="page-description">
            프로젝트 수행 중 발생한 이슈를 등록·추적합니다. 이슈는 <b>WBS 업무와 연결</b>할 수 있고, 이슈에서 업무로, WBS 업무에서 이슈로 서로
            이동·관리됩니다.
          </p>
        </div>
        <button
          className="primary small"
          disabled={scopeProjects.length === 0}
          onClick={() => setForm({ issue: newIssue(allIssues, projectFilter || scopeProjects[0]?.id), isNew: true })}
        >
          + 이슈 등록
        </button>
      </div>

      <div className="pms-summary">
        <div className="pms-stat warn">
          <div className="label">미해결 (오픈 · 진행 중)</div>
          <div className="value">{open.length}</div>
        </div>
        <div className="pms-stat danger">
          <div className="label">긴급 · 높음 미해결</div>
          <div className="value">{open.filter((i) => isHighPriority(i.priority)).length}</div>
        </div>
        <div className="pms-stat danger">
          <div className="label">조치 기한 초과</div>
          <div className="value">{overdue.length}</div>
        </div>
        <div className="pms-stat">
          <div className="label">업무 미연결 (공통)</div>
          <div className="value">{issues.filter((i) => i.taskIds.length === 0).length}</div>
        </div>
        <div className="pms-stat">
          <div className="label">미해결 일정 영향 합계</div>
          <div className="value">{open.reduce((s, i) => s + (i.impactDays || 0), 0)}일</div>
        </div>
      </div>

      <ScheduleProposalPanel
        scopeIds={scopeIds}
        canDecideOf={(pid) => seeAll || projects.find((p) => p.id === pid)?.transitioned?.manager?.name === ME}
      />

      {taskParam && (
        <div className="pms-blind" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>
            🔗 업무 <b>{taskName(taskParam)?.name ?? taskParam}</b> 에 연결된 이슈만 표시 중입니다.
          </span>
          <button className="link-btn" onClick={() => setParams({}, { replace: true })}>
            필터 해제
          </button>
        </div>
      )}

      <div className="pms-chips">
        <button className={!statusFilter ? 'on' : ''} onClick={() => setStatusFilter('')}>
          전체 <span>{issues.length}</span>
        </button>
        {ISSUE_STATUSES.map((s) => (
          <button key={s} className={statusFilter === s ? 'on' : ''} onClick={() => setStatusFilter(s)}>
            {s} <span>{issues.filter((i) => i.status === s).length}</span>
          </button>
        ))}
      </div>

      <div className="search-bar">
        <div className="search-field">
          <label>프로젝트 ({seeAll ? '수행 중인 전체 프로젝트' : '내가 참여 중인 프로젝트'})</label>
          <select value={projectFilter} onChange={(e) => setProjectFilter(e.target.value)} style={{ minWidth: 260 }}>
            <option value="">전체 ({scopeProjects.length})</option>
            {scopeProjects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.id})
              </option>
            ))}
          </select>
        </div>
        <div className="search-field">
          <label>검색 (ID · 제목 · 내용)</label>
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        </div>
        <div className="search-field">
          <label>우선순위</label>
          <select value={priorityFilter} onChange={(e) => setPriorityFilter(e.target.value)}>
            <option value="">전체</option>
            {PRIORITIES.map((p) => (
              <option key={p}>{p}</option>
            ))}
          </select>
        </div>
        <div className="search-field">
          <label>유형</label>
          <select value={typeFilter} onChange={(e) => setTypeFilter(e.target.value)}>
            <option value="">전체</option>
            {ISSUE_TYPES.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </div>
        <div className="search-field">
          <label>담당자</label>
          <select value={assigneeFilter} onChange={(e) => setAssigneeFilter(e.target.value)}>
            <option value="">전체</option>
            {MEMBERS.map((m) => (
              <option key={m.name}>{m.name}</option>
            ))}
          </select>
        </div>
        <div className="search-field">
          <label>업무 연결</label>
          <select value={linkFilter} onChange={(e) => setLinkFilter(e.target.value)}>
            <option value="">전체</option>
            <option value="linked">연결됨</option>
            <option value="none">미연결 (공통)</option>
          </select>
        </div>
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>ID</th>
            <th>프로젝트</th>
            <th>제목</th>
            <th>유형</th>
            <th>우선순위</th>
            <th>상태</th>
            <th>담당</th>
            <th>조치 기한</th>
            <th>연결 업무 (WBS)</th>
            <th style={{ textAlign: 'right' }}>일정 영향</th>
          </tr>
        </thead>
        <tbody>
          {filtered.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={10}>
                {scopeProjects.length === 0 ? '조회할 수 있는(참여 중인) 수행 프로젝트가 없습니다.' : '조건에 맞는 이슈가 없습니다.'}
              </td>
            </tr>
          )}
          {filtered.map((i) => {
            const linkedTasks = i.taskIds.map(taskName).filter(Boolean)
            const late = isOpenStatus(i.status) && i.dueDate && i.dueDate < TODAY
            return (
              <tr key={i.id} style={{ cursor: 'pointer' }} onClick={() => setForm({ issue: i, isNew: false })}>
                <td>
                  <b>{i.id}</b>
                </td>
                <td className="name-cell">
                  <span className="pms-tag req" title={i.projectId}>
                    {projectName(i.projectId)}
                  </span>
                </td>
                <td className="name-cell">{i.title}</td>
                <td>{i.type}</td>
                <td>
                  <span className={`pms-tag ${PRIORITY_CLASS[i.priority]}`}>{i.priority}</span>
                </td>
                <td>
                  <span className={`pms-tag ${STATUS_CLASS[i.status]}`}>{i.status}</span>
                  <ProcessButton getModel={() => issueProcess(i)} label="현황" style={{ marginLeft: 4 }} />
                </td>
                <td>{i.assignee}</td>
                <td>
                  {i.dueDate ? (
                    <>
                      {i.dueDate.slice(5)}{' '}
                      {isOpenStatus(i.status) && <span className={`pms-tag ${late ? 'late' : ''}`}>{dday(i.dueDate)}</span>}
                    </>
                  ) : (
                    '-'
                  )}
                </td>
                <td onClick={(e) => e.stopPropagation()}>
                  {linkedTasks.length > 0 ? (
                    linkedTasks.map((task) => (
                      <div key={task.id}>
                        <span className="pms-tag">{task.phase}</span> {task.name}{' '}
                        <button className="link-btn" onClick={() => navigate(`/workspace/PMS/wbs?task=${task.id}`)}>
                          WBS ›
                        </button>
                      </div>
                    ))
                  ) : (
                    <span className="muted">프로젝트 공통</span>
                  )}
                </td>
                <td style={{ textAlign: 'right' }}>{i.impactDays ? `${i.impactDays}일` : '-'}</td>
              </tr>
            )
          })}
        </tbody>
      </table>

      {form && (
        <IssueForm
          key={form.issue.id}
          initial={form.issue}
          isNew={form.isNew}
          projects={scopeProjects}
          onClose={() => setForm(null)}
        />
      )}
    </div>
  )
}
