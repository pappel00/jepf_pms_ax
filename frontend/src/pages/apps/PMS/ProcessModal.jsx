import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'

/**
 * 프로세스(상태) 현황 모달 — 업무가 어떤 단계를 거쳤는지(지난 처리: 상태·담당자·일자), 지금 어느 단계이고
 * 누구에게 있는지(현재 담당)를 한 화면에서 보여준다. 화면마다 model만 만들어서 넘기면 된다(processModels.js).
 *
 * model = {
 *   title, subtitle,
 *   steps:   [{ label, state: 'done'|'current'|'pending'|'rejected'|'skipped', who, date, note }],
 *   current: { label, who, hint } | null,   // null이면 종료된 프로세스
 *   history: [{ date, who, action, text }],
 *   endNote?: string,                        // 종료된 프로세스 안내
 * }
 */
const STATE_STYLE = {
  done: { bg: '#e8f6ec', border: '#9bd3ab', color: '#1f6b34', mark: '✔' },
  current: { bg: '#fff4d6', border: '#e5b93c', color: '#6b4d00', mark: '●' },
  pending: { bg: '#f3f5f8', border: '#d7dce5', color: '#7a8599', mark: '○' },
  rejected: { bg: '#fdeaea', border: '#e6a3a3', color: '#a02525', mark: '✖' },
  skipped: { bg: '#f3f5f8', border: '#d7dce5', color: '#9aa5b8', mark: '–' },
}

export function ProcessSteps({ steps }) {
  return (
    <div style={{ display: 'flex', alignItems: 'stretch', gap: 6, flexWrap: 'wrap' }}>
      {steps.map((s, i) => {
        const st = STATE_STYLE[s.state] ?? STATE_STYLE.pending
        return (
          <div key={`${s.label}-${i}`} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <div
              style={{
                minWidth: 118, padding: '8px 10px', borderRadius: 8, background: st.bg, border: `1px solid ${st.border}`, color: st.color,
                boxShadow: s.state === 'current' ? '0 0 0 2px #f1d27a' : 'none',
              }}
            >
              <div style={{ fontWeight: 700, fontSize: 13 }}>
                {st.mark} {s.label}
              </div>
              <div style={{ fontSize: 12, marginTop: 2 }}>{s.who || (s.state === 'pending' ? '예정' : '-')}</div>
              <div style={{ fontSize: 11, opacity: 0.85 }}>{s.date || ''}</div>
              {s.note && <div style={{ fontSize: 11, marginTop: 2 }}>{s.note}</div>}
            </div>
            {i < steps.length - 1 && <span style={{ color: '#9aa5b8' }}>›</span>}
          </div>
        )
      })}
    </div>
  )
}

export default function ProcessModal({ model, onClose }) {
  return (
    <Modal title={model.title} onClose={onClose} width={860}>
      {model.subtitle && <p className="muted" style={{ marginTop: 0 }}>{model.subtitle}</p>}

      {model.current ? (
        <div className="pms-blind" style={{ background: '#fff4d6', borderColor: '#ecd48b', color: '#6b4d00', display: 'block' }}>
          📍 현재 <b>{model.current.label}</b> 단계 — 지금 <b>{model.current.who}</b>에게 있습니다.
          {model.current.hint && <div style={{ fontSize: 12, marginTop: 2 }}>{model.current.hint}</div>}
        </div>
      ) : (
        <div className="pms-blind" style={{ display: 'block' }}>🏁 {model.endNote ?? '처리가 끝났습니다.'}</div>
      )}

      <div className="pms-form-section">진행 단계</div>
      <ProcessSteps steps={model.steps} />

      <div className="pms-form-section">지난 처리 내역 <span className="muted" style={{ fontWeight: 400 }}>{model.history.length}건</span></div>
      {model.history.length === 0 ? (
        <p className="muted">처리 내역이 없습니다.</p>
      ) : (
        <table className="data-table pms-wbs">
          <thead>
            <tr>
              <th style={{ width: 100 }}>일자</th>
              <th style={{ width: 110 }}>담당자</th>
              <th style={{ width: 110 }}>처리</th>
              <th>내용</th>
            </tr>
          </thead>
          <tbody>
            {[...model.history].reverse().map((h, i) => (
              <tr key={i}>
                <td>{h.date}</td>
                <td><b>{h.who}</b></td>
                <td><span className="pms-tag">{h.action}</span></td>
                <td className="name-cell">{h.text}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </Modal>
  )
}

/** 목록/상세에서 쓰는 작은 버튼 + 모달 묶음. getModel은 열릴 때 호출해서 최신 데이터를 반영한다. */
export function ProcessButton({ getModel, label = '처리 현황', style }) {
  const [open, setOpen] = useState(false)
  return (
    <>
      <button
        type="button"
        className="link-btn"
        style={style}
        title="지난 처리와 현재 담당자 보기"
        onClick={(e) => {
          e.stopPropagation()
          setOpen(true)
        }}
      >
        🔄 {label}
      </button>
      {open && <ProcessModal model={getModel()} onClose={() => setOpen(false)} />}
    </>
  )
}
