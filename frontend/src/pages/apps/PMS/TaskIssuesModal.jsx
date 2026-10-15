import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Modal from '../../../components/Modal'
import './pms.css'
import IssueForm from './IssueForm'
import { ISSUE_TYPES, isOpenStatus, issuesOfTask, linkIssue, nextIssueId, useIssues } from './issueStore'
import { ME, TODAY, useTasks } from './pmsStore'

const STATUS_CLASS = { 오픈: 'late', '진행 중': 'doing', 해결: 'done', 종결: '' }
const PRIORITY_CLASS = { 긴급: 'late', 높음: 'manual', 보통: '', 낮음: '' }

/**
 * WBS 업무에 연결된 이슈 관리 — WBS 행의 「이슈」 버튼에서 연다.
 * 이 업무에 이슈 등록 / 기존 이슈 연결 / 연결 해제 / 이슈 관리 화면에서 열기를 지원한다(업무 → 이슈 방향).
 */
export default function TaskIssuesModal({ task, onClose }) {
  const issues = useIssues()
  const tasks = useTasks()
  const navigate = useNavigate()
  const [form, setForm] = useState(null)
  const [linkId, setLinkId] = useState('')

  const linked = issuesOfTask(issues, task.id)
  const linkable = issues.filter((i) => i.taskId !== task.id && isOpenStatus(i.status) && i.projectId === task.projectId)
  const taskName = (id) => tasks.find((t) => t.id === id)?.name

  const startNew = () =>
    setForm({
      id: nextIssueId(issues), projectId: task.projectId, title: '', type: ISSUE_TYPES[0], priority: '보통', status: '오픈',
      reporter: ME, assignee: task.assignee ?? '', registeredDate: TODAY, dueDate: '', resolvedDate: '', taskId: task.id,
      impactDays: 0, content: '', resolution: '', history: [],
    })

  return (
    <Modal title={`이슈 — ${task.name}`} onClose={onClose} width={900}>
      <p className="muted" style={{ marginTop: 0 }}>
        {task.phase} 단계 · 담당 {task.assignee || '미배정'} · {task.start} ~ {task.end}
      </p>

      <div style={{ display: 'flex', gap: 8, marginBottom: 12, flexWrap: 'wrap' }}>
        <button className="primary small" onClick={startNew}>
          + 이 업무에 이슈 등록
        </button>
        <select className="pms-cell-input" style={{ width: 360 }} value={linkId} onChange={(e) => setLinkId(e.target.value)}>
          <option value="">기존 이슈 연결… (미해결 이슈 선택)</option>
          {linkable.map((i) => (
            <option key={i.id} value={i.id}>
              {i.id} · {i.title}
              {i.taskId ? ` (현재: ${taskName(i.taskId)})` : ' (업무 미연결)'}
            </option>
          ))}
        </select>
        <button
          className="secondary small"
          disabled={!linkId}
          onClick={() => {
            linkIssue(linkId, task.id, ME, TODAY, task.name)
            setLinkId('')
          }}
        >
          연결
        </button>
        <button className="link-btn" style={{ marginLeft: 'auto' }} onClick={() => navigate(`/workspace/PMS/issues?task=${task.id}`)}>
          이슈 관리에서 보기 ›
        </button>
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>ID</th>
            <th>제목</th>
            <th>우선순위</th>
            <th>상태</th>
            <th>담당</th>
            <th>조치 기한</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {linked.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={7}>
                이 업무에 연결된 이슈가 없습니다.
              </td>
            </tr>
          )}
          {linked.map((i) => (
            <tr key={i.id}>
              <td>
                <b>{i.id}</b>
              </td>
              <td className="name-cell">{i.title}</td>
              <td>
                <span className={`pms-tag ${PRIORITY_CLASS[i.priority]}`}>{i.priority}</span>
              </td>
              <td>
                <span className={`pms-tag ${STATUS_CLASS[i.status]}`}>{i.status}</span>
              </td>
              <td>{i.assignee}</td>
              <td>{i.dueDate || '-'}</td>
              <td style={{ whiteSpace: 'nowrap' }}>
                <button className="link-btn" onClick={() => setForm(i)}>
                  수정
                </button>
                <button className="link-btn" onClick={() => navigate(`/workspace/PMS/issues?issue=${i.id}`)}>
                  이슈 관리에서 열기
                </button>
                <button className="link-btn danger" onClick={() => linkIssue(i.id, null, ME, TODAY)}>
                  연결 해제
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {form && (
        <IssueForm
          key={form.id}
          initial={form}
          isNew={!issues.some((i) => i.id === form.id)}
          lockTask
          onClose={() => setForm(null)}
        />
      )}
    </Modal>
  )
}
