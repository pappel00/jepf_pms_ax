import Modal from '../../../components/Modal'
import './pms.css'
import { useCustomerState } from './customerStore'
import { TODAY } from './pmsStore'
import { useSalesNotes } from './salesNoteStore'

/** 프로젝트 하나의 영업 노트를 시간순(최신 먼저)으로 한눈에 보는 읽기 전용 모달. */
export default function ProjectNotesModal({ project, onClose }) {
  const notes = useSalesNotes()
  const { contacts } = useCustomerState()
  const list = notes.filter((n) => n.projectId === project.id).sort((a, b) => b.date.localeCompare(a.date) || b.id.localeCompare(a.id))
  const open = list.filter((n) => n.nextAction && !n.done)

  return (
    <Modal title={`영업 노트 — ${project.name}`} onClose={onClose} width={820}>
      <p className="muted" style={{ marginTop: 0 }}>
        {project.id} · {project.client} · {project.status} · 영업 담당 {project.salesName || '-'}
      </p>
      <div className="pms-summary">
        <div className="pms-stat"><div className="label">전체 노트</div><div className="value">{list.length}</div></div>
        <div className="pms-stat"><div className="label">최근 활동</div><div className="value" style={{ fontSize: 16 }}>{list[0]?.date ?? '-'}</div></div>
        <div className={`pms-stat ${open.some((n) => n.nextDate && n.nextDate < TODAY) ? 'danger' : ''}`}><div className="label">남은 후속 조치</div><div className="value">{open.length}</div></div>
      </div>

      {open.length > 0 && (
        <div className="pms-panel" style={{ boxShadow: 'none', border: '1px solid #e4e5ea' }}>
          <h3>남은 후속 조치</h3>
          {open.map((n) => (
            <div key={n.id} className="pms-task">
              <div className="pms-task-main">
                <div className="pms-task-title">{n.nextAction}</div>
                <div className="pms-task-sub">{n.author} · 기록 {n.date}</div>
              </div>
              {n.nextDate && <span className={`pms-tag ${n.nextDate < TODAY ? 'late' : 'doing'}`}>{n.nextDate}</span>}
            </div>
          ))}
        </div>
      )}

      {list.length === 0 ? (
        <p className="muted">이 프로젝트에 기록된 영업 노트가 없습니다. 영업 노트 메뉴에서 작성하세요.</p>
      ) : (
        list.map((n) => (
          <div key={n.id} className="pms-task" style={{ display: 'block' }}>
            <div>
              <b>{n.date}</b> <span className="pms-tag doing">{n.type}</span> <b>{n.title}</b>
              <span className="muted"> · {n.author}{n.contactId && ` · ${contacts.find((t) => t.id === n.contactId)?.name ?? ''}`}</span>
            </div>
            <div style={{ whiteSpace: 'pre-wrap', margin: '4px 0' }}>{n.content}</div>
            {n.nextAction && (
              <div className="muted" style={{ textDecoration: n.done ? 'line-through' : 'none' }}>
                → 다음 할 일: {n.nextAction} {n.nextDate && `(${n.nextDate})`} {n.done && '· 완료'}
              </div>
            )}
          </div>
        ))
      )}
      <button className="secondary small" style={{ marginTop: 10 }} onClick={() => window.open(`/workspace/PMS/sales-notes?project=${project.id}`, '_blank')}>
        영업 노트 메뉴에서 열기 (작성·수정)
      </button>
    </Modal>
  )
}
