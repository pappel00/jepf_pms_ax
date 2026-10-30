import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { SortTh, useSort } from './sortUtil'
import { CUSTOMER_STATUSES, INDUSTRIES, customerOf, deleteCustomer, emptyCustomer, saveCustomer, useCustomerState } from './customerStore'
import { useProjects } from './projectStore'

function CustomerForm({ initial, onClose }) {
  const [c, setC] = useState(initial)
  const set = (patch) => setC((p) => ({ ...p, ...patch }))
  const err = !c.name.trim() ? '고객사명을 입력하세요.' : ''
  const f = (label, key, extra = {}, required) => (
    <div className="field">
      <label>
        {label}
        {required && <span className="req-mark"> *</span>}
      </label>
      <input value={c[key]} onChange={(e) => set({ [key]: e.target.value })} {...extra} />
    </div>
  )
  return (
    <Modal title={c.id ? `고객사 수정 — ${c.id}` : '고객사 등록'} onClose={onClose} width={560}>
      {f('고객사명', 'name', {}, true)}
      <div className="field">
        <label>업종</label>
        <select value={c.industry} onChange={(e) => set({ industry: e.target.value })}>
          <option value="">선택 안 함</option>
          {INDUSTRIES.map((i) => (
            <option key={i}>{i}</option>
          ))}
        </select>
      </div>
      <div className="field">
        <label>거래 상태</label>
        <select value={c.status} onChange={(e) => set({ status: e.target.value })}>
          {CUSTOMER_STATUSES.map((s) => (
            <option key={s}>{s}</option>
          ))}
        </select>
      </div>
      {f('사업자등록번호', 'bizNo', { placeholder: '000-00-00000' })}
      {f('대표 전화', 'phone')}
      {f('주소', 'address')}
      {f('홈페이지', 'homepage')}
      <div className="field">
        <label>비고</label>
        <textarea rows={2} value={c.memo} onChange={(e) => set({ memo: e.target.value })} />
      </div>
      {err && <p style={{ color: '#c0392b' }}>{err}</p>}
      <button
        className="primary"
        disabled={!!err}
        onClick={() => {
          saveCustomer({ ...c, name: c.name.trim() })
            .then(onClose)
            .catch((e) => window.alert(e.message))
        }}
      >
        저장
      </button>
    </Modal>
  )
}

/** 고객사 관리 — 발주사 마스터. 프로젝트 관리의 발주사는 여기서 선택한다. */
export default function CustomersPage() {
  const { customers, contacts } = useCustomerState()
  const projects = useProjects()
  const [keyword, setKeyword] = useState('')
  const [status, setStatus] = useState('')
  const [edit, setEdit] = useState(null)

  const rows = customers.filter(
    (c) => (!status || c.status === status) && (!keyword || `${c.name} ${c.bizNo} ${c.id}`.toLowerCase().includes(keyword.toLowerCase())),
  )
  const projectCount = (c) => projects.filter((p) => customerOf(customers, p)?.id === c.id).length
  const { sort, onSort, sorted } = useSort(rows, {
    id: (c) => c.id,
    name: (c) => c.name,
    industry: (c) => c.industry,
    status: (c) => c.status,
    phone: (c) => c.phone,
    address: (c) => c.address,
    contacts: (c) => contacts.filter((t) => t.customerId === c.id).length,
    projects: (c) => projectCount(c),
  })
  const remove = (c) => {
    const n = projectCount(c)
    if (n > 0) return window.alert(`프로젝트 ${n}건에서 사용 중이라 삭제할 수 없습니다. 거래 상태를 「거래 종료」로 바꾸세요.`)
    if (window.confirm(`${c.name}과(와) 담당자 정보를 삭제할까요?`)) deleteCustomer(c.id).catch((e) => window.alert(e.message))
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            고객사 관리
          </h2>
          <p className="page-description">발주사 정보를 자산으로 관리합니다. 프로젝트 관리에서는 여기 등록된 고객사를 선택해 입력합니다.</p>
        </div>
        <button className="primary small" onClick={() => setEdit(emptyCustomer())}>
          ＋ 고객사 등록
        </button>
      </div>
      {edit && <CustomerForm initial={edit} onClose={() => setEdit(null)} />}
      <div className="search-bar">
        <div className="search-field">
          <label>검색</label>
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="고객사명·코드·사업자번호" />
        </div>
        <div className="search-field">
          <label>거래 상태</label>
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">전체</option>
            {CUSTOMER_STATUSES.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <SortTh k="id" sort={sort} onSort={onSort}>코드</SortTh>
            <SortTh k="name" sort={sort} onSort={onSort}>고객사명</SortTh>
            <SortTh k="industry" sort={sort} onSort={onSort}>업종</SortTh>
            <SortTh k="status" sort={sort} onSort={onSort}>거래 상태</SortTh>
            <SortTh k="phone" sort={sort} onSort={onSort}>대표 전화</SortTh>
            <SortTh k="address" sort={sort} onSort={onSort}>주소</SortTh>
            <SortTh k="contacts" sort={sort} onSort={onSort} style={{ textAlign: 'right' }}>담당자</SortTh>
            <SortTh k="projects" sort={sort} onSort={onSort} style={{ textAlign: 'right' }}>프로젝트</SortTh>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={9}>
                고객사가 없습니다.
              </td>
            </tr>
          )}
          {sorted.map((c) => (
            <tr key={c.id}>
              <td>{c.id}</td>
              <td className="name-cell">
                <b>{c.name}</b>
              </td>
              <td>{c.industry || '-'}</td>
              <td>
                <span className={`pms-tag ${c.status === '거래 중' ? 'done' : c.status === '잠재' ? 'manual' : ''}`}>{c.status}</span>
              </td>
              <td>{c.phone || '-'}</td>
              <td>{c.address || '-'}</td>
              <td style={{ textAlign: 'right' }}>{contacts.filter((t) => t.customerId === c.id).length}</td>
              <td style={{ textAlign: 'right' }}>{projectCount(c)}</td>
              <td>
                <button className="link-btn" onClick={() => setEdit(c)}>
                  수정
                </button>{' '}
                <button className="link-btn" onClick={() => remove(c)}>
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
