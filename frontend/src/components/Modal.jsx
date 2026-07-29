export default function Modal({ title, onClose, children, width = 480 }) {
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal-box" style={{ width }} onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h3>{title}</h3>
          <button className="modal-close" onClick={onClose}>
            ×
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}
