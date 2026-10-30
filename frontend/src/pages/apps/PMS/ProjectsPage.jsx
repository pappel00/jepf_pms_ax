import { useEffect, useMemo, useState } from 'react'
import { usersApi } from '../../../api/entities'
import Modal from '../../../components/Modal'
import './pms.css'
import ProjectNotesModal from './ProjectNotesModal'
import { contactOf, customerOf, useCustomerState } from './customerStore'
import { TODAY } from './pmsStore'
import { SortTh, useSort } from './sortUtil'
import {
  BID_TYPES,
  BUSINESS_TYPES,
  FAIL_REASON_CODES,
  PIPELINE_STATUSES,
  STATUSES,
  STATUS_NAMES,
  WON_STATUSES,
  defaultProbabilityOf,
  deleteProject,
  formatKoreanWon,
  saveProject,
  statusIndex,
  useProjects,
} from './projectStore'

const STAGE_CLASS = { 영업: 'doing', 수주: 'done', 수행: 'done', 운영: 'done', 종료: '' }
const won = (n) => (n ? `${n.toLocaleString()}` : '-')
const dday = (date) => {
  if (!date) return ''
  const diff = Math.round((new Date(date) - new Date(TODAY)) / 86400000)
  return diff === 0 ? 'D-Day' : diff > 0 ? `D-${diff}` : `D+${-diff}`
}

const emptyProject = () => ({
  id: '', // 프로젝트 코드는 서버가 자동 채번한다
  name: '', client: '', start: '', end: '', amount: null,
  status: '공고 전', probability: defaultProbabilityOf('공고 전'),
  contactName: '', contactTitle: '', contactPhone: '', contactEmail: '',
  salesId: null, salesName: '',
  businessType: '', bidType: '', noticeNo: '', dueDate: '', presentDate: '',
  competitors: '', pmName: '', costEstimate: null, failReasonCode: '', failReason: '', vatIncluded: false, memo: '',
})

/** 상태에 따라 조건부로 필수가 되는 항목 판정(요구: 제안 단계부터 발주사 담당자 정보 필수 등). */
const needsContact = (status) => statusIndex(status) >= statusIndex('제안 중') && !['실패', '보류/취소'].includes(status)
const needsDueDate = (status) => ['공고 중', '제안 중'].includes(status)
const needsFailReason = (status) => ['실패', '보류/취소'].includes(status)

function validate(p) {
  const e = {}
  if (!p.name.trim()) e.name = '프로젝트명을 입력하세요.'
  if (!p.customerId) e.client = '발주사를 선택하세요.'
  if (!p.start) e.start = '시작일을 선택하세요.'
  if (!p.end) e.end = '종료일을 선택하세요.'
  if (p.start && p.end && p.end < p.start) e.end = '종료일이 시작일보다 빠릅니다.'
  if (p.amount === null || p.amount === '' || p.amount < 0) e.amount = '발주금액(추정 가능)을 입력하세요.'
  if (!p.salesName) e.salesName = '영업 담당자를 선택하세요.'
  if (p.probability === '' || p.probability < 0 || p.probability > 100) e.probability = '0~100 사이 값을 입력하세요.'
  if (needsContact(p.status)) {
    if (!p.contactName.trim()) e.contactName = '제안 단계부터 발주사 담당자는 필수입니다.'
    if (!p.contactPhone.trim()) e.contactPhone = '제안 단계부터 연락처는 필수입니다.'
  }
  if (needsDueDate(p.status) && !p.dueDate) e.dueDate = '공고 중/제안 중에는 제안 마감일이 필요합니다.'
  if (needsFailReason(p.status) && !p.failReasonCode) e.failReasonCode = '실패/보류 사유를 선택하세요.'
  return e
}

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

function ProjectForm({ initial, isNew, users, onClose }) {
  const { customers, contacts } = useCustomerState()
  // 발주사·담당자는 고객사 관리 / 고객사 담당자 관리에 등록된 마스터에서 선택한다(id가 없는 기존 데이터는 이름으로 매칭).
  const [p, setP] = useState(() => ({
    ...initial,
    customerId: customerOf(customers, initial)?.id ?? '',
    contactId: contactOf(contacts, initial)?.id ?? '',
  }))
  const [probTouched, setProbTouched] = useState(false)
  const [errors, setErrors] = useState({})
  const set = (key, value) => setP((prev) => ({ ...prev, [key]: value }))

  const onStatus = (status) =>
    setP((prev) => ({
      ...prev,
      status,
      probability: probTouched ? prev.probability : defaultProbabilityOf(status),
    }))

  const pickCustomer = (id) => {
    const c = customers.find((x) => x.id === id)
    setP((prev) => ({ ...prev, customerId: id, client: c?.name ?? '', contactId: '', contactName: '', contactTitle: '', contactPhone: '', contactEmail: '' }))
  }
  const pickContact = (id) => {
    const t = contacts.find((x) => x.id === id)
    setP((prev) => ({ ...prev, contactId: id, contactName: t?.name ?? '', contactTitle: t?.title ?? '', contactPhone: t?.phone ?? '', contactEmail: t?.email ?? '' }))
  }
  const contactOptions = contacts.filter((t) => t.customerId === p.customerId && (t.useYn === 'Y' || t.id === p.contactId))

  const salesOptions = useMemo(() => {
    const list = users.map((u) => ({ value: u.userName, label: `${u.userName} (${u.loginId})`, id: u.id }))
    if (p.salesName && !list.some((o) => o.value === p.salesName)) {
      list.push({ value: p.salesName, label: `${p.salesName} (미등록 사용자)`, id: null })
    }
    return list
  }, [users, p.salesName])

  const pmOptions = useMemo(() => users.map((u) => ({ value: u.userName, label: `${u.userName} (${u.loginId})`, id: u.id })), [users])

  const margin =
    p.amount && p.costEstimate ? Math.round(((p.amount - p.costEstimate) / p.amount) * 1000) / 10 : null
  const weighted = p.amount && p.probability !== '' ? Math.round((p.amount * p.probability) / 100) : null
  const statusInfo = STATUSES.find((s) => s.name === p.status)

  const [saving, setSaving] = useState(false)
  // 서버 검증 오류의 필드명 → 화면 오류 키
  const FIELD_KEY = {
    projectName: 'name', customerId: 'client', contactId: 'contactName', salesUserId: 'salesName', pmUserId: 'pmName',
    failReasonCode: 'failReasonCode', dueDate: 'dueDate', startDate: 'start', endDate: 'end', amount: 'amount',
    probabilityPct: 'probability', statusCode: 'status',
  }

  const submit = async () => {
    const err = validate(p)
    setErrors(err)
    if (Object.keys(err).length) return
    setSaving(true)
    try {
      await saveProject({ ...p, name: p.name.trim(), client: p.client.trim() })
      onClose()
    } catch (e) {
      const next = {}
      ;(e.errors || []).forEach((d) => {
        next[FIELD_KEY[d.field] ?? 'server'] = d.reason
      })
      if (!Object.keys(next).length) window.alert(e.message)
      setErrors(next)
      setSaving(false)
    }
  }

  const text = (key, extra = {}) => (
    <input value={p[key] ?? ''} onChange={(e) => set(key, e.target.value)} {...extra} />
  )
  const moneyInput = (key, onEdit) => (
    <input
      inputMode="numeric"
      value={p[key] === null || p[key] === '' ? '' : Number(p[key]).toLocaleString()}
      onChange={(e) => {
        const digits = e.target.value.replace(/[^\d]/g, '')
        set(key, digits === '' ? null : Number(digits))
        onEdit?.()
      }}
      placeholder="숫자만 입력 (원)"
    />
  )

  return (
    <Modal title={isNew ? '프로젝트 등록' : `프로젝트 수정 — ${p.id}`} onClose={onClose} width={820}>
      <div className="pms-form-legend">
        <span className="req-mark">*</span> 필수 · <span className="cond-mark">조건부 필수</span> 상태에 따라 필수 ·{' '}
        <span className="opt-mark">옵션</span>
      </div>

      <div className="pms-form-section">기본 정보</div>
      <div className="pms-form-grid">
        <Field label="프로젝트 코드" hint="자동 채번 (PRJ-연도-일련번호)">
          <input value={p.id} disabled />
        </Field>
        <Field label="상태" required hint={statusInfo?.desc}>
          <select value={p.status} onChange={(e) => onStatus(e.target.value)}>
            {STATUS_NAMES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </Field>
        <Field label="프로젝트명" required error={errors.name} wide>
          {text('name', { placeholder: '예) 통합업무포탈 구축사업' })}
        </Field>
        <Field label="사업 구분">
          <select value={p.businessType} onChange={(e) => set('businessType', e.target.value)}>
            <option value="">선택 안 함</option>
            {BUSINESS_TYPES.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </Field>
        <Field label="수행 PM" hint="수주 후 지정해도 됩니다 (프로젝트 멤버와 연동 예정)" error={errors.pmName}>
          <select
            value={p.pmName}
            onChange={(e) => {
              const o = pmOptions.find((x) => x.value === e.target.value)
              setP((prev) => ({ ...prev, pmName: e.target.value, pmId: o?.id ?? null }))
            }}
          >
            <option value="">지정 안 함</option>
            {pmOptions.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </Field>
        <Field label="시작일(예정)" required error={errors.start}>
          <input type="date" value={p.start} onChange={(e) => set('start', e.target.value)} />
        </Field>
        <Field label="종료일(예정)" required error={errors.end}>
          <input type="date" value={p.end} onChange={(e) => set('end', e.target.value)} />
        </Field>
      </div>

      <div className="pms-form-section">발주 정보</div>
      <div className="pms-form-grid">
        <Field label="발주사" required error={errors.client} hint="고객사 관리에 등록된 고객사에서 선택합니다.">
          <div style={{ display: 'flex', gap: 6 }}>
            <select value={p.customerId} onChange={(e) => pickCustomer(e.target.value)} style={{ flex: 1 }}>
              <option value="">선택</option>
              {customers.filter((c) => c.status !== '거래 종료' || c.id === p.customerId).map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name}
                </option>
              ))}
            </select>
            <button type="button" className="secondary small" onClick={() => window.open('/workspace/PMS/customers', '_blank')}>
              고객사 등록
            </button>
          </div>
        </Field>
        <Field
          label="발주금액 (원)"
          required
          error={errors.amount}
          hint={p.amount ? `≈ ${formatKoreanWon(p.amount)} (${p.vatIncluded ? '부가세 포함' : '부가세 별도'}) · 확정 전에는 추정 금액을 입력` : '확정 전에는 추정 금액을 입력'}
        >
          {moneyInput('amount')}
          <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13, marginTop: 4 }}>
            <input type="checkbox" checked={!!p.vatIncluded} onChange={(e) => set('vatIncluded', e.target.checked)} />
            부가세 포함 금액
          </label>
        </Field>
        <Field label="발주사 담당자" conditional={needsContact(p.status)} error={errors.contactName} hint={p.customerId ? '선택한 고객사의 담당자 중에서 고릅니다.' : '발주사를 먼저 선택하세요.'}>
          <div style={{ display: 'flex', gap: 6 }}>
            <select value={p.contactId} disabled={!p.customerId} onChange={(e) => pickContact(e.target.value)} style={{ flex: 1 }}>
              <option value="">선택</option>
              {contactOptions.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                  {t.title ? ` ${t.title}` : ''}
                </option>
              ))}
            </select>
            <button type="button" className="secondary small" onClick={() => window.open('/workspace/PMS/customer-contacts', '_blank')}>
              담당자 등록
            </button>
          </div>
        </Field>
        <Field label="담당자 직책" hint="담당자 관리 정보 (수정은 고객사 담당자 관리에서)">
          <input value={p.contactTitle} disabled />
        </Field>
        <Field label="담당자 연락처" conditional={needsContact(p.status)} error={errors.contactPhone}>
          <input value={p.contactPhone} disabled />
        </Field>
        <Field label="담당자 이메일">
          <input value={p.contactEmail} disabled />
        </Field>
      </div>

      <div className="pms-form-section">영업 정보</div>
      <div className="pms-form-grid">
        <Field label="영업 담당자" required error={errors.salesName} hint="사용자 관리(/api/v1/users) 연동 — 로그인 사용자 목록에서 선택">
          <select
            value={p.salesName}
            onChange={(e) => {
              const o = salesOptions.find((x) => x.value === e.target.value)
              setP((prev) => ({ ...prev, salesName: e.target.value, salesId: o?.id ?? null }))
            }}
          >
            <option value="">선택하세요</option>
            {salesOptions.map((o) => (
              <option key={o.value} value={o.value}>
                {o.label}
              </option>
            ))}
          </select>
        </Field>
        <Field
          label="수주 확률도 (%)"
          required
          error={errors.probability}
          hint={`상태별 기본값 ${defaultProbabilityOf(p.status)}% 자동 제안 · 직접 수정하면 상태를 바꿔도 유지됩니다${
            weighted !== null ? ` · 가중 예상금액 ${formatKoreanWon(weighted)}` : ''
          }`}
        >
          <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
            <input
              type="range"
              min="0"
              max="100"
              step="5"
              value={p.probability}
              style={{ padding: 0 }}
              onChange={(e) => {
                setProbTouched(true)
                set('probability', Number(e.target.value))
              }}
            />
            <b style={{ width: 44, textAlign: 'right' }}>{p.probability}%</b>
          </div>
        </Field>
      </div>

      <div className="pms-form-section">입찰 정보</div>
      <div className="pms-form-grid">
        <Field label="계약/입찰 방식">
          <select value={p.bidType} onChange={(e) => set('bidType', e.target.value)}>
            <option value="">선택 안 함</option>
            {BID_TYPES.map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </Field>
        <Field label="공고번호">{text('noticeNo')}</Field>
        <Field label="제안 마감일" conditional={needsDueDate(p.status)} error={errors.dueDate}>
          <input type="date" value={p.dueDate} onChange={(e) => set('dueDate', e.target.value)} />
        </Field>
        <Field label="발표/평가일">
          <input type="date" value={p.presentDate} onChange={(e) => set('presentDate', e.target.value)} />
        </Field>
        <Field label="경쟁사" wide>
          {text('competitors', { placeholder: '쉼표로 구분' })}
        </Field>
        <Field
          label="예상 원가 (원)"
          hint={`${margin !== null ? `예상 이익률 ${margin}%` : '입력하면 예상 이익률을 계산합니다'}${p.costSource ? ` · 출처: ${p.costSource}` : ''}`}
        >
          {moneyInput('costEstimate', () => setP((prev) => ({ ...prev, costSource: '', costSourceType: 'MANUAL' })))}
        </Field>
      </div>

      <div className="pms-form-section">기타</div>
      <div className="pms-form-grid">
        <Field label="실패/보류 사유" conditional={needsFailReason(p.status)} error={errors.failReasonCode}>
          <select value={p.failReasonCode ?? ''} onChange={(e) => set('failReasonCode', e.target.value)}>
            <option value="">선택하세요</option>
            {FAIL_REASON_CODES.map((c) => (
              <option key={c}>{c}</option>
            ))}
          </select>
        </Field>
        <Field label="사유 상세" hint="사유 코드 외에 남길 설명" wide>
          <textarea rows={2} value={p.failReason} onChange={(e) => set('failReason', e.target.value)} />
        </Field>
        <Field label="비고" wide>
          <textarea rows={2} value={p.memo} onChange={(e) => set('memo', e.target.value)} />
        </Field>
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <button className="primary" style={{ flex: 1 }} disabled={saving} onClick={submit}>
          {saving ? '저장 중…' : '저장'}
        </button>
        {errors.server && <span className="pms-field-error" style={{ alignSelf: 'center' }}>{errors.server}</span>}
        <button className="secondary" style={{ marginTop: 0 }} onClick={onClose}>
          취소
        </button>
        {!isNew && (
          <button
            className="secondary"
            style={{ marginTop: 0, color: '#d32f2f' }}
            onClick={() => {
              if (window.confirm(`"${p.name}" 프로젝트를 삭제할까요?`)) {
                deleteProject(p.id)
                  .then(onClose)
                  .catch((e) => window.alert(e.message))
              }
            }}
          >
            삭제
          </button>
        )}
      </div>
    </Modal>
  )
}

export default function ProjectsPage() {
  const all = useProjects()
  const [users, setUsers] = useState([])
  const [statusFilter, setStatusFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [salesFilter, setSalesFilter] = useState('')
  const [editing, setEditing] = useState(null) // { project, isNew }
  const [notesOf, setNotesOf] = useState(null)

  useEffect(() => {
    usersApi
      .list()
      .then((res) => setUsers(res.data || []))
      .catch(() => setUsers([]))
  }, [])

  const salesNames = [...new Set(all.map((p) => p.salesName).filter(Boolean))]
  const filtered = all.filter(
    (p) =>
      (!statusFilter || p.status === statusFilter) &&
      (!salesFilter || p.salesName === salesFilter) &&
      (!keyword || `${p.name} ${p.client} ${p.id}`.toLowerCase().includes(keyword.toLowerCase())),
  )

  const { sort, onSort, sorted } = useSort(filtered, {
    id: (p) => p.id,
    name: (p) => p.name,
    client: (p) => p.client,
    period: (p) => p.start,
    amount: (p) => p.amount,
    probability: (p) => p.probability,
    status: (p) => STATUSES.findIndex((s) => s.name === p.status),
    sales: (p) => p.salesName,
    contact: (p) => p.contactName,
    due: (p) => p.dueDate,
  })

  const pipeline = all.filter((p) => PIPELINE_STATUSES.includes(p.status))
  const pipelineAmount = pipeline.reduce((s, p) => s + (p.amount || 0), 0)
  const weightedAmount = pipeline.reduce((s, p) => s + ((p.amount || 0) * p.probability) / 100, 0)
  const wonAmount = all.filter((p) => WON_STATUSES.includes(p.status)).reduce((s, p) => s + (p.amount || 0), 0)
  const finished = all.filter((p) => [...WON_STATUSES, '실패'].includes(p.status))
  const winRate = finished.length
    ? Math.round((finished.filter((p) => p.status !== '실패').length / finished.length) * 100)
    : 0

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            프로젝트 관리
          </h2>
          <p className="page-description">
            영업(공고 전 ~ 협상 중) → 수주 → 수행 → 종료까지 사업 단위를 관리합니다. 수주 후에는 같은 프로젝트로 WBS·진척 관리가
            이어집니다.
          </p>
        </div>
        <button className="primary small" onClick={() => setEditing({ project: emptyProject(), isNew: true })}>
          + 프로젝트 등록
        </button>
      </div>

      <div className="pms-summary">
        <div className="pms-stat">
          <div className="label">영업 진행 건 (수주 전)</div>
          <div className="value">{pipeline.length}</div>
          <div className="muted">{formatKoreanWon(pipelineAmount)}</div>
        </div>
        <div className="pms-stat warn">
          <div className="label">가중 예상 금액 (금액×확률)</div>
          <div className="value" style={{ fontSize: 20 }}>{formatKoreanWon(Math.round(weightedAmount))}</div>
        </div>
        <div className="pms-stat ok">
          <div className="label">수주 금액 (수주·수행·완료)</div>
          <div className="value" style={{ fontSize: 20 }}>{formatKoreanWon(wonAmount)}</div>
        </div>
        <div className="pms-stat">
          <div className="label">수주율 (수주 ÷ 결과 확정)</div>
          <div className="value">{winRate}%</div>
        </div>
      </div>

      <div className="pms-chips">
        <button className={!statusFilter ? 'on' : ''} onClick={() => setStatusFilter('')}>
          전체 <span>{all.length}</span>
        </button>
        {STATUSES.map((s) => (
          <button key={s.name} className={statusFilter === s.name ? 'on' : ''} onClick={() => setStatusFilter(s.name)} title={s.desc}>
            {s.name} <span>{all.filter((p) => p.status === s.name).length}</span>
          </button>
        ))}
      </div>

      <div className="search-bar">
        <div className="search-field">
          <label>검색 (프로젝트명 · 발주사 · 코드)</label>
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="검색어" />
        </div>
        <div className="search-field">
          <label>영업 담당자</label>
          <select value={salesFilter} onChange={(e) => setSalesFilter(e.target.value)}>
            <option value="">전체</option>
            {salesNames.map((n) => (
              <option key={n}>{n}</option>
            ))}
          </select>
        </div>
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <SortTh k="id" sort={sort} onSort={onSort}>코드</SortTh>
            <SortTh k="name" sort={sort} onSort={onSort}>프로젝트명</SortTh>
            <SortTh k="client" sort={sort} onSort={onSort}>발주사</SortTh>
            <SortTh k="period" sort={sort} onSort={onSort}>기간</SortTh>
            <SortTh k="amount" sort={sort} onSort={onSort} style={{ textAlign: 'right' }}>발주금액(원)</SortTh>
            <SortTh k="probability" sort={sort} onSort={onSort}>확률도</SortTh>
            <SortTh k="status" sort={sort} onSort={onSort}>상태</SortTh>
            <SortTh k="sales" sort={sort} onSort={onSort}>영업 담당</SortTh>
            <SortTh k="contact" sort={sort} onSort={onSort}>발주사 담당자</SortTh>
            <SortTh k="due" sort={sort} onSort={onSort}>제안 마감</SortTh>
            <th>영업 노트</th>
          </tr>
        </thead>
        <tbody>
          {filtered.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={11}>
                조건에 맞는 프로젝트가 없습니다.
              </td>
            </tr>
          )}
          {sorted.map((p) => {
            const stage = STATUSES.find((s) => s.name === p.status)?.stage
            return (
              <tr key={p.id} style={{ cursor: 'pointer' }} onClick={() => setEditing({ project: p, isNew: false })}>
                <td>{p.id}</td>
                <td className="name-cell">
                  <b>{p.name}</b>
                </td>
                <td>{p.client}</td>
                <td>
                  {p.start} ~ {p.end}
                </td>
                <td style={{ textAlign: 'right' }}>{won(p.amount)}</td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <div className="pms-bar" style={{ width: 60 }}>
                      <span style={{ width: `${p.probability}%` }} />
                    </div>
                    {p.probability}%
                  </div>
                </td>
                <td>
                  <span className={`pms-tag ${STAGE_CLASS[stage]}`}>{p.status}</span>
                </td>
                <td>{p.salesName}</td>
                <td>{p.contactName ? `${p.contactName} · ${p.contactPhone}` : '-'}</td>
                <td>
                  {p.dueDate && ['공고 중', '제안 중'].includes(p.status) ? (
                    <span className={`pms-tag ${dday(p.dueDate).startsWith('D+') ? 'late' : ''}`}>
                      {p.dueDate.slice(5)} ({dday(p.dueDate)})
                    </span>
                  ) : (
                    '-'
                  )}
                </td>
                <td onClick={(e) => e.stopPropagation()}>
                  <button className="secondary small" onClick={() => setNotesOf(p)}>
                    영업노트 보기
                  </button>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>

      {notesOf && <ProjectNotesModal project={notesOf} onClose={() => setNotesOf(null)} />}
      {editing && (
        <ProjectForm
          key={editing.project.id}
          initial={editing.project}
          isNew={editing.isNew}
          users={users}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  )
}
