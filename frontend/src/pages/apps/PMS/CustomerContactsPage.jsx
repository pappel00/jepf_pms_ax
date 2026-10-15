import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { contactOf, deleteContact, emptyContact, saveContact, useCustomerState } from './customerStore'
import { useProjects } from './projectStore'

function ContactForm({ initial, customers, onClose }) {
  const [t, setT] = useState(initial)
  const set = (patch) => setT((p) => ({ ...p, ...patch }))
  const err = !t.customerId
    ? '고객사를 선택하세요.'
    : !t.name.trim()
      ? '담당자명을 입력하세요.'
      : !t.phone.trim() && !t.email.trim()
        ? '연락처 또는 이메일 중 하나는 입력하세요.'
        : ''
  const f = (label, key, extra = {}, required) => (
    <div className="field">
      <label>
        {label}
        {required && <span className="req-mark"> *</span>}
      </label>
      <input value={t[key]} onChange={(e) => set({ [key]: e.target.value })} {...extra} />
    </div>
  )
  return (
    <Modal title={t.id ? `담당자 수정 — ${t.id}` : '고객사 담당자 등록'} onClose={onClose} width={520}>
      <div className="field">
        <label>
          고객사<span className="req-mark"> *</span>
        </label>
        <select value={t.customerId} onChange={(e) => set({ customerId: e.target.value })}>
          <option value="">선택</option>
          {customers.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </select>
      </div>
      {f('담당자명', 'name', {}, true)}
      {f('직책', 'title')}
      {f('부서', 'dept')}
      {f('연락처', 'phone', { placeholder: '010-0000-0000' })}
      {f('이메일', 'email', { type: 'email' })}
      <div className="field">
        <label>사용 여부</label>
        <select value={t.useYn} onChange={(e) => set({ useYn: e.target.value })}>
          <option value="Y">사용</option>
          <option value="N">미사용 (퇴사·이동 등)</option>
        </select>
      </div>
      <div className="field">
        <label>비고</label>
        <textarea rows={2} value={t.memo} onChange={(e) => set({ memo: e.target.value })} />
      </div>
      {err && <p style={{ color: '#c0392b' }}>{err}</p>}
      <button
        className="primary"
        disabled={!!err}
        onClick={() => {
          saveContact({ ...t, name: t.name.trim() })
          onClose()
        }}
      >
        저장
      </button>
    </Modal>
  )
}

/** 고객사 담당자 관리 — 고객사별 담당자 마스터. 프로젝트 관리의 발주사 담당자는 여기서 선택한다. */
export default function CustomerContactsPage() {
  const { customers, contacts } = useCustomerState()
  const projects = useProjects()
  const [customerId, setCustomerId] = useState('')
  const [keyword, setKeyword] = useState('')
  const [edit, setEdit] = useState(null)

  const nameOf = (id) => customers.find((c) => c.id === id)?.name ?? '-'
  const rows = contacts.filter(
    (t) =>
      (!customerId || t.customerId === customerId) &&
      (!keyword || `${t.name} ${t.dept} ${t.phone} ${t.email}`.toLowerCase().includes(keyword.toLowerCase())),
  )
  const projectCount = (t) => projects.filter((p) => contactOf(contacts, p)?.id === t.id).length
  const remove = (t) => {
    const n = projectCount(t)
    if (n > 0) return window.alert(`프로젝트 ${n}건에서 사용 중이라 삭제할 수 없습니다. 사용 여부를 「미사용」으로 바꾸세요.`)
    if (window.confirm(`${t.name} 담당자를 삭제할까요?`)) deleteContact(t.id)
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            고객사 담당자 관리 <span className="pms-mock-badge">목업 · 더미 데이터</span>
          </h2>
          <p className="page-description">
            고객사별 담당자(이름·직책·연락처)를 관리합니다. 프로젝트 관리에서 발주사를 고르면 해당 고객사의 담당자 중에서 선택합니다.
          </p>
        </div>
        <button className="primary" onClick={() => setEdit(emptyContact(customerId))}>
          ＋ 담당자 등록
        </button>
      </div>
      {edit && <ContactForm initial={edit} customers={customers} onClose={() => setEdit(null)} />}
      <div className="search-bar">
        <div className="search-field">
          <label>고객사</label>
          <select value={customerId} onChange={(e) => setCustomerId(e.target.value)}>
            <option value="">전체</option>
            {customers.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        </div>
        <div className="search-field">
          <label>검색</label>
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="이름·부서·연락처·이메일" />
        </div>
      </div>
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>코드</th>
            <th>고객사</th>
            <th>담당자</th>
            <th>직책</th>
            <th>부서</th>
            <th>연락처</th>
            <th>이메일</th>
            <th>사용</th>
            <th style={{ textAlign: 'right' }}>프로젝트</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={10}>
                담당자가 없습니다.
              </td>
            </tr>
          )}
          {rows.map((t) => (
            <tr key={t.id}>
              <td>{t.id}</td>
              <td>{nameOf(t.customerId)}</td>
              <td>
                <b>{t.name}</b>
              </td>
              <td>{t.title || '-'}</td>
              <td>{t.dept || '-'}</td>
              <td>{t.phone || '-'}</td>
              <td>{t.email || '-'}</td>
              <td>{t.useYn === 'Y' ? <span className="pms-tag done">사용</span> : <span className="pms-tag">미사용</span>}</td>
              <td style={{ textAlign: 'right' }}>{projectCount(t)}</td>
              <td>
                <button className="link-btn" onClick={() => setEdit(t)}>
                  수정
                </button>{' '}
                <button className="link-btn" onClick={() => remove(t)}>
                  삭제
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
