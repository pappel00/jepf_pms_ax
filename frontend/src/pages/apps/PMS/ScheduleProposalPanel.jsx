import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { ME, TODAY } from './pmsStore'
import { decideProposal, useScheduleProposals } from './scheduleStore'

const STATUS_CLASS = { 제안: 'manual', 승인: 'done', 반려: 'late', 무시: '' }

/** 제안 하나의 검토 모달 — 업무별 변경 전/후 일정을 보여주고 승인/반려/무시한다. */
function ProposalModal({ proposal, canDecide, onClose }) {
  const pending = proposal.status === '제안'
  const reject = () => {
    const comment = window.prompt('반려 사유를 입력하세요 (필수)')
    if (comment && comment.trim()) {
      decideProposal(proposal.id, '반려', { by: ME, comment: comment.trim(), today: TODAY })
      onClose()
    }
  }
  return (
    <Modal title={`일정 조정 제안 — ${proposal.id}`} onClose={onClose} width={820}>
      <p className="muted" style={{ marginTop: 0 }}>
        {proposal.basis} · 이슈 <b>{proposal.issueId}</b> {proposal.issueTitle}
      </p>
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>구분</th>
            <th>업무</th>
            <th>변경 전</th>
            <th>변경 후</th>
          </tr>
        </thead>
        <tbody>
          {proposal.items.map((i) => (
            <tr key={i.taskId}>
              <td><span className={`pms-tag ${i.role === '지연 업무' ? 'late' : 'doing'}`}>{i.role}</span></td>
              <td className="name-cell">{i.name}</td>
              <td>{i.oldStart} ~ {i.oldEnd}</td>
              <td>
                <b>{i.newStart} ~ {i.newEnd}</b>
                <div className="muted" style={{ fontSize: 11 }}>
                  {i.newStart !== i.oldStart ? '시작·종료 이동' : '종료일 연장'} (+{proposal.shiftDays}영업일)
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="pms-field-hint">진척 100%인 업무는 제외했습니다. 승인하면 WBS 일정이 위 내용으로 바뀝니다(주말 제외, 영업일 기준).</div>

      {pending ? (
        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          <button
            className="primary"
            disabled={!canDecide}
            title={canDecide ? '' : '프로젝트 관리자(또는 센터장·admin)만 처리할 수 있습니다'}
            onClick={() => {
              decideProposal(proposal.id, '승인', { by: ME, comment: '', today: TODAY })
              onClose()
            }}
          >
            승인 — WBS 일정 반영
          </button>
          <button className="secondary" style={{ marginTop: 0 }} disabled={!canDecide} onClick={reject}>
            반려
          </button>
          <button
            className="link-btn"
            disabled={!canDecide}
            onClick={() => {
              decideProposal(proposal.id, '무시', { by: ME, comment: '일정 조정 불필요', today: TODAY })
              onClose()
            }}
          >
            무시 (조정하지 않음)
          </button>
        </div>
      ) : (
        <p style={{ marginTop: 12 }}>
          <span className={`pms-tag ${STATUS_CLASS[proposal.status]}`}>{proposal.status}</span> {proposal.decidedBy} · {proposal.decidedAt}
          {proposal.comment && ` · ${proposal.comment}`}
        </p>
      )}
    </Modal>
  )
}

/**
 * 이슈 관리 화면 상단의 일정 조정 제안 목록 — 이슈를 해결로 바꾸면 만들어진 제안(승인 대기)과 처리 결과를 보여준다.
 * scopeIds: 조회 범위 안의 프로젝트 ID 집합, canDecideOf(projectId): 처리 권한(프로젝트 관리자·센터장·admin).
 */
export default function ScheduleProposalPanel({ scopeIds, canDecideOf }) {
  const all = useScheduleProposals().filter((p) => scopeIds.has(p.projectId))
  const [open, setOpen] = useState(null)
  const pending = all.filter((p) => p.status === '제안')
  if (all.length === 0) return null
  const current = open ? all.find((p) => p.id === open) : null
  return (
    <div className="pms-panel" style={{ marginBottom: 12 }}>
      <h3>
        일정 조정 제안 <span className="count">{pending.length}</span>
        <span className="muted" style={{ fontWeight: 400 }}>이슈를 해결하면 연결 업무와 후행 업무의 일정 조정을 제안합니다</span>
      </h3>
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>제안</th>
            <th>이슈</th>
            <th>영향</th>
            <th>대상 업무</th>
            <th>상태 / 처리</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {all.map((p) => (
            <tr key={p.id}>
              <td><b>{p.id}</b><div className="muted">{p.createdAt}</div></td>
              <td className="name-cell">{p.issueId} {p.issueTitle}</td>
              <td>{p.shiftDays}영업일</td>
              <td>{p.items.length}건</td>
              <td>
                <span className={`pms-tag ${STATUS_CLASS[p.status]}`}>{p.status}</span>
                {p.decidedBy && <div className="muted">{p.decidedBy} · {p.decidedAt}</div>}
              </td>
              <td>
                <button className="link-btn" onClick={() => setOpen(p.id)}>
                  {p.status === '제안' ? '검토' : '보기'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {current && <ProposalModal proposal={current} canDecide={canDecideOf(current.projectId)} onClose={() => setOpen(null)} />}
    </div>
  )
}
