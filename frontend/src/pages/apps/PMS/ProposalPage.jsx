import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { MEMBERS, TODAY } from './pmsStore'
import { formatKoreanWon, useProjects } from './projectStore'
import { REQ_CATEGORIES, useRfps } from './rfpStore'
import {
  DEFAULT_CHECKLIST,
  PROPOSAL_STATUSES,
  RESPONSES,
  addCheck,
  addVersion,
  deleteProposal,
  nextProposalId,
  nextVersion,
  removeCheck,
  saveProposal,
  setMapping,
  toggleCheck,
  useProposals,
} from './proposalStore'

const STATUS_CLASS = { '작성 중': 'manual', '내부 검토': 'doing', '제출 완료': 'done', '결과 확정': 'done' }
const RESPONSE_CLASS = { 수용: 'done', '일부 수용': 'doing', '대안 제시': 'manual', 미수용: 'late' }
const dday = (date) => {
  if (!date) return ''
  const diff = Math.round((new Date(date) - new Date(TODAY)) / 86400000)
  return diff === 0 ? 'D-Day' : diff > 0 ? `D-${diff}` : `D+${-diff}`
}
const won = (n) => (n ? n.toLocaleString() : '-')
const marginOf = (p) => (p.price && p.costEstimate ? Math.round(((p.price - p.costEstimate) / p.price) * 1000) / 10 : null)

function Field({ label, required, error, hint, children, wide }) {
  return (
    <div className={`field pms-field ${wide ? 'wide' : ''}`}>
      <label>
        {label}
        {required ? <span className="req-mark"> *</span> : <span className="opt-mark"> (옵션)</span>}
      </label>
      {children}
      {hint && <div className="pms-field-hint">{hint}</div>}
      {error && <div className="pms-field-error">{error}</div>}
    </div>
  )
}

/* ---------- 제안서 등록/수정 ---------- */
function ProposalForm({ initial, isNew, projects, rfps, onClose, onSaved }) {
  const [p, setP] = useState(initial)
  const [errors, setErrors] = useState({})
  const set = (k, v) => setP((prev) => ({ ...prev, [k]: v }))
  const projectRfps = rfps.filter((r) => r.projectId === p.projectId)
  const project = projects.find((x) => x.id === p.projectId)

  const moneyInput = (key) => (
    <input
      inputMode="numeric"
      value={p[key] ? Number(p[key]).toLocaleString() : ''}
      onChange={(e) => {
        const d = e.target.value.replace(/[^\d]/g, '')
        set(key, d === '' ? null : Number(d))
      }}
      placeholder="숫자만 입력 (원)"
    />
  )

  const submit = () => {
    const e = {}
    if (!p.projectId) e.projectId = '프로젝트를 선택하세요.'
    if (!p.rfpId) e.rfpId = '대응할 RFP를 선택하세요.'
    if (!p.title.trim()) e.title = '제안서명을 입력하세요.'
    if (!p.owner) e.owner = '작성 책임자를 선택하세요.'
    if (!p.dueDate) e.dueDate = '제출 마감일을 입력하세요.'
    setErrors(e)
    if (Object.keys(e).length) return
    const next = { ...p, title: p.title.trim() }
    if (next.status === '제출 완료' && !next.submittedDate) next.submittedDate = TODAY
    saveProposal(next)
    onSaved(next.id)
    onClose()
  }

  return (
    <Modal title={isNew ? '제안서 등록' : `제안서 수정 — ${p.id}`} onClose={onClose} width={720}>
      <div className="pms-form-grid">
        <Field label="프로젝트" required error={errors.projectId} wide>
          <select
            value={p.projectId}
            disabled={!isNew}
            onChange={(e) => {
              const r = rfps.find((x) => x.projectId === e.target.value)
              setP((prev) => ({ ...prev, projectId: e.target.value, rfpId: r?.id ?? '', dueDate: r?.dueDate || prev.dueDate }))
            }}
          >
            <option value="">선택하세요</option>
            {projects.map((x) => (
              <option key={x.id} value={x.id}>
                {x.id} · {x.name} ({x.status})
              </option>
            ))}
          </select>
        </Field>
        <Field label="대응 RFP" required error={errors.rfpId} hint={project && projectRfps.length === 0 ? '이 프로젝트에 등록된 RFP가 없습니다. RFP 관리에서 먼저 등록하세요.' : ''} wide>
          <select
            value={p.rfpId}
            disabled={!isNew}
            onChange={(e) => {
              const r = rfps.find((x) => x.id === e.target.value)
              setP((prev) => ({ ...prev, rfpId: e.target.value, dueDate: r?.dueDate || prev.dueDate }))
            }}
          >
            <option value="">선택하세요</option>
            {projectRfps.map((r) => (
              <option key={r.id} value={r.id}>
                {r.id} · {r.title} ({r.round})
              </option>
            ))}
          </select>
        </Field>
        <Field label="제안서명" required error={errors.title} wide>
          <input value={p.title} onChange={(e) => set('title', e.target.value)} />
        </Field>
        <Field label="작성 책임자" required error={errors.owner}>
          <select value={p.owner} onChange={(e) => set('owner', e.target.value)}>
            <option value="">선택하세요</option>
            {MEMBERS.map((m) => (
              <option key={m.name} value={m.name}>
                {m.name} ({m.role})
              </option>
            ))}
          </select>
        </Field>
        <Field label="상태" required>
          <select value={p.status} onChange={(e) => set('status', e.target.value)}>
            {PROPOSAL_STATUSES.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </Field>
        <Field label="제출 마감일" required error={errors.dueDate} hint="RFP의 제출 마감일이 기본값입니다">
          <input type="date" value={p.dueDate} onChange={(e) => set('dueDate', e.target.value)} />
        </Field>
        <Field label="제출일" hint="상태를 '제출 완료'로 바꾸면 자동 입력">
          <input type="date" value={p.submittedDate} onChange={(e) => set('submittedDate', e.target.value)} />
        </Field>
        <Field label="제안 금액 (원)" hint={p.price ? `≈ ${formatKoreanWon(p.price)}${project?.amount ? ` · 발주 추정금액 ${formatKoreanWon(project.amount)}` : ''}` : ''}>
          {moneyInput('price')}
        </Field>
        <Field label="예상 원가 (원)" hint="RFP 평가 M/D 합계 연동 예정 — 지금은 직접 입력">
          {moneyInput('costEstimate')}
        </Field>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <button className="primary" style={{ flex: 1 }} onClick={submit}>
          저장
        </button>
        <button className="secondary" style={{ marginTop: 0 }} onClick={onClose}>
          취소
        </button>
      </div>
    </Modal>
  )
}

/* ---------- 새 버전 업로드 ---------- */
function VersionForm({ proposal, onClose }) {
  const [v, setV] = useState({ ver: nextVersion(proposal.versions), summary: '', fileName: '', fileSize: '' })
  const [error, setError] = useState('')
  return (
    <Modal title="제안서 새 버전 업로드" onClose={onClose} width={560}>
      <div className="pms-form-grid">
        <Field label="버전" required hint="마지막 버전 다음 번호가 기본값입니다 (제출본은 v1.0 등으로 직접 지정)">
          <input value={v.ver} onChange={(e) => setV((p) => ({ ...p, ver: e.target.value }))} />
        </Field>
        <Field label="작성자">
          <input value={proposal.owner} disabled />
        </Field>
        <Field label="변경 요약" required wide>
          <input value={v.summary} onChange={(e) => setV((p) => ({ ...p, summary: e.target.value }))} placeholder="예) 보안 요구사항 대응 보완" />
        </Field>
        <Field label="파일" required wide hint="목업이라 파일명만 저장합니다">
          <input
            type="file"
            accept=".pdf,.ppt,.pptx,.doc,.docx,.hwp,.hwpx"
            onChange={(e) => {
              const f = e.target.files?.[0]
              setV((p) => ({ ...p, fileName: f?.name ?? '', fileSize: f ? `${(f.size / 1048576).toFixed(1)}MB` : '' }))
            }}
          />
        </Field>
      </div>
      {error && <div className="pms-field-error">{error}</div>}
      <button
        className="primary"
        onClick={() => {
          if (!v.ver.trim() || !v.summary.trim() || !v.fileName) {
            setError('버전, 변경 요약, 파일을 모두 입력하세요.')
            return
          }
          addVersion(proposal.id, { ...v, date: TODAY, author: proposal.owner })
          onClose()
        }}
      >
        업로드
      </button>
    </Modal>
  )
}

/* ---------- 탭 1: 버전 이력 ---------- */
function VersionTab({ proposal }) {
  const [form, setForm] = useState(false)
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
        <span className="muted">제안서 원본은 버전별로 보관됩니다. 제출본은 별도 표시됩니다.</span>
        <button className="primary small" onClick={() => setForm(true)}>
          + 새 버전 업로드
        </button>
      </div>
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>버전</th>
            <th>일자</th>
            <th>작성자</th>
            <th>변경 요약</th>
            <th>파일</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {[...proposal.versions].reverse().map((v, i) => (
            <tr key={v.ver}>
              <td>
                <b>{v.ver}</b> {i === 0 && <span className="pms-tag doing">최신</span>}{' '}
                {proposal.submittedDate && v.date === proposal.submittedDate && <span className="pms-tag done">제출본</span>}
              </td>
              <td>{v.date}</td>
              <td>{v.author}</td>
              <td className="name-cell">{v.summary}</td>
              <td>
                📎 {v.fileName} <span className="muted">({v.fileSize})</span>
              </td>
              <td>
                <button className="link-btn" onClick={() => window.alert('목업: 파일 다운로드')}>
                  다운로드
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {form && <VersionForm proposal={proposal} onClose={() => setForm(false)} />}
    </div>
  )
}

/* ---------- 탭 2: RFP 항목 대응표 ---------- */
function MappingTab({ proposal, rfp }) {
  const [keyword, setKeyword] = useState('')
  const [onlyGap, setOnlyGap] = useState(false)
  if (!rfp || rfp.items.length === 0) {
    return <div className="pms-empty">대응할 RFP 항목이 없습니다. RFP 관리에서 항목을 먼저 등록하세요.</div>
  }
  const mapped = (i) => proposal.mapping[i.id]?.response
  const done = rfp.items.filter(mapped).length
  const gapMandatory = rfp.items.filter((i) => i.mandatory && !mapped(i)).length
  const count = (r) => rfp.items.filter((i) => mapped(i) === r).length
  const categories = REQ_CATEGORIES.filter((c) => rfp.items.some((i) => i.category === c.code))
  const visible = (i) =>
    (!onlyGap || !mapped(i)) && (!keyword || `${i.code} ${i.name} ${i.content}`.toLowerCase().includes(keyword.toLowerCase()))
  const locked = proposal.status === '제출 완료' || proposal.status === '결과 확정'

  return (
    <div>
      <div className="pms-summary">
        <div className="pms-stat">
          <div className="label">대응 완료</div>
          <div className="value">
            {done}/{rfp.items.length}
          </div>
        </div>
        <div className={`pms-stat ${gapMandatory ? 'danger' : 'ok'}`}>
          <div className="label">필수 항목 미대응</div>
          <div className="value">{gapMandatory}</div>
        </div>
        <div className="pms-stat">
          <div className="label">수용 / 일부 수용</div>
          <div className="value" style={{ fontSize: 20 }}>
            {count('수용')} / {count('일부 수용')}
          </div>
        </div>
        <div className="pms-stat">
          <div className="label">대안 제시 / 미수용</div>
          <div className="value" style={{ fontSize: 20 }}>
            {count('대안 제시')} / {count('미수용')}
          </div>
        </div>
      </div>

      <div className="search-bar" style={{ boxShadow: 'none', background: '#fafbfd', padding: '10px 14px' }}>
        <div className="search-field">
          <label>검색 (코드 · 항목명 · 내용)</label>
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} />
        </div>
        <label style={{ fontSize: 13, display: 'flex', gap: 6, alignItems: 'center', paddingBottom: 8 }}>
          <input type="checkbox" checked={onlyGap} onChange={(e) => setOnlyGap(e.target.checked)} />
          미대응 항목만
        </label>
        {locked && <span className="muted" style={{ marginLeft: 'auto' }}>제출 완료된 제안서는 대응표를 수정할 수 없습니다.</span>}
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>RFP 코드</th>
            <th>항목명 / 요구사항</th>
            <th>필수</th>
            <th>제안서 목차(절)</th>
            <th>대응</th>
            <th>대응 내용 요약</th>
            <th>담당</th>
          </tr>
        </thead>
        <tbody>
          {categories.flatMap((c) => {
            const rows = rfp.items.filter((i) => i.category === c.code && visible(i))
            if (rows.length === 0) return []
            return [
              <tr key={`h-${c.code}`} className="phase-row">
                <td colSpan={7}>
                  {c.code} · {c.name} <span className="muted">({rows.length}건)</span>
                </td>
              </tr>,
              ...rows.map((i) => {
                const m = proposal.mapping[i.id] ?? {}
                return (
                  <tr key={i.id}>
                    <td>
                      <span className="pms-tag req">{i.code}</span>
                    </td>
                    <td className="name-cell" style={{ maxWidth: 300 }}>
                      <b>{i.name}</b>
                      <div className="muted">{i.content}</div>
                    </td>
                    <td>{i.mandatory ? <span className="pms-tag late">필수</span> : <span className="pms-tag">선택</span>}</td>
                    <td>
                      <input
                        className="pms-cell-input"
                        style={{ minWidth: 70, width: 90 }}
                        disabled={locked}
                        value={m.section ?? ''}
                        placeholder="예) 3.2.1"
                        onChange={(e) => setMapping(proposal.id, i.id, { section: e.target.value })}
                      />
                    </td>
                    <td>
                      <select
                        className="pms-cell-input"
                        style={{ minWidth: 90 }}
                        disabled={locked}
                        value={m.response ?? ''}
                        onChange={(e) => setMapping(proposal.id, i.id, e.target.value ? { response: e.target.value } : null)}
                      >
                        <option value="">미대응</option>
                        {RESPONSES.map((r) => (
                          <option key={r}>{r}</option>
                        ))}
                      </select>
                      {m.response && <div style={{ marginTop: 3 }}><span className={`pms-tag ${RESPONSE_CLASS[m.response]}`}>{m.response}</span></div>}
                    </td>
                    <td>
                      <input
                        className="pms-cell-input"
                        style={{ minWidth: 220 }}
                        disabled={locked || !m.response}
                        value={m.summary ?? ''}
                        placeholder={m.response ? '대응 내용 요약' : '-'}
                        onChange={(e) => setMapping(proposal.id, i.id, { summary: e.target.value })}
                      />
                    </td>
                    <td>
                      <select
                        className="pms-cell-input"
                        style={{ minWidth: 80 }}
                        disabled={locked || !m.response}
                        value={m.owner ?? ''}
                        onChange={(e) => setMapping(proposal.id, i.id, { owner: e.target.value })}
                      >
                        <option value="">-</option>
                        {MEMBERS.map((x) => (
                          <option key={x.name}>{x.name}</option>
                        ))}
                      </select>
                    </td>
                  </tr>
                )
              }),
            ]
          })}
        </tbody>
      </table>
    </div>
  )
}

/* ---------- 탭 3: 제출 체크리스트 ---------- */
function ChecklistTab({ proposal }) {
  const [name, setName] = useState('')
  const done = proposal.checklist.filter((c) => c.done).length
  return (
    <div style={{ maxWidth: 560 }}>
      <p className="muted" style={{ marginTop: 0 }}>
        제출 서류 준비 현황 — {done}/{proposal.checklist.length} 완료
      </p>
      {proposal.checklist.map((c) => (
        <div key={c.id} className="pms-check">
          <label>
            <input type="checkbox" checked={c.done} onChange={() => toggleCheck(proposal.id, c.id)} /> <span style={{ textDecoration: c.done ? 'line-through' : 'none', color: c.done ? '#999' : 'inherit' }}>{c.name}</span>
          </label>
          <button className="link-btn danger" onClick={() => removeCheck(proposal.id, c.id)}>
            삭제
          </button>
        </div>
      ))}
      <div className="mapping-add-row" style={{ marginTop: 12 }}>
        <input value={name} onChange={(e) => setName(e.target.value)} placeholder="제출 서류 추가 (예: 재무제표)" />
        <button
          className="primary small"
          onClick={() => {
            if (name.trim()) {
              addCheck(proposal.id, name.trim())
              setName('')
            }
          }}
        >
          추가
        </button>
      </div>
    </div>
  )
}

function ProposalDetail({ proposal, project, rfp, onEdit, onDeleted }) {
  const [tab, setTab] = useState('mapping')
  const margin = marginOf(proposal)
  const done = rfp ? rfp.items.filter((i) => proposal.mapping[i.id]?.response).length : 0

  return (
    <div className="pms-panel" style={{ marginTop: 18 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <h3 style={{ fontSize: 16, marginBottom: 4 }}>
            {proposal.title} <span className={`pms-tag ${STATUS_CLASS[proposal.status]}`}>{proposal.status}</span>
          </h3>
          <div className="muted">
            {proposal.id} · {project ? `${project.id} ${project.name} (${project.client})` : '-'} · 대응 RFP {rfp ? `${rfp.id} (${rfp.round})` : '-'} · 책임 {proposal.owner}
          </div>
          <div className="muted" style={{ marginTop: 4 }}>
            제출 마감 {proposal.dueDate} {proposal.status === '작성 중' || proposal.status === '내부 검토' ? `(${dday(proposal.dueDate)})` : ''}
            {proposal.submittedDate && ` · 제출일 ${proposal.submittedDate}`}
          </div>
        </div>
        <div style={{ display: 'flex', gap: 6, alignItems: 'flex-start' }}>
          <button className="secondary small" onClick={() => onEdit(proposal)}>
            정보 수정
          </button>
          <button
            className="secondary small"
            style={{ color: '#d32f2f' }}
            onClick={() => {
              if (window.confirm(`"${proposal.title}" 제안서를 삭제할까요?`)) {
                deleteProposal(proposal.id)
                onDeleted()
              }
            }}
          >
            삭제
          </button>
        </div>
      </div>

      <div className="pms-summary" style={{ marginTop: 14 }}>
        <div className="pms-stat">
          <div className="label">제안 금액</div>
          <div className="value" style={{ fontSize: 18 }}>{formatKoreanWon(proposal.price)}</div>
          <div className="muted">{project?.amount ? `발주 추정 ${formatKoreanWon(project.amount)}` : ''}</div>
        </div>
        <div className="pms-stat">
          <div className="label">예상 원가</div>
          <div className="value" style={{ fontSize: 18 }}>{formatKoreanWon(proposal.costEstimate)}</div>
        </div>
        <div className={`pms-stat ${margin !== null && margin < 15 ? 'warn' : 'ok'}`}>
          <div className="label">예상 이익률</div>
          <div className="value">{margin !== null ? `${margin}%` : '-'}</div>
        </div>
        <div className="pms-stat">
          <div className="label">RFP 항목 대응</div>
          <div className="value">
            {done}/{rfp?.items.length ?? 0}
          </div>
        </div>
        <div className="pms-stat">
          <div className="label">최신 버전</div>
          <div className="value">{proposal.versions[proposal.versions.length - 1]?.ver ?? '-'}</div>
        </div>
      </div>

      <div className="tabs-bar">
        {[
          ['mapping', 'RFP 항목 대응표'],
          ['versions', `버전 이력 (${proposal.versions.length})`],
          ['checklist', `제출 체크리스트 (${proposal.checklist.filter((c) => c.done).length}/${proposal.checklist.length})`],
        ].map(([k, label]) => (
          <button key={k} className={`tab-btn ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>
            {label}
          </button>
        ))}
      </div>
      <div className="tab-content">
        {tab === 'mapping' && <MappingTab proposal={proposal} rfp={rfp} />}
        {tab === 'versions' && <VersionTab proposal={proposal} />}
        {tab === 'checklist' && <ChecklistTab proposal={proposal} />}
      </div>
    </div>
  )
}

export default function ProposalPage() {
  const proposals = useProposals()
  const rfps = useRfps()
  const projects = useProjects()
  const [selectedId, setSelectedId] = useState(proposals[0]?.id ?? null)
  const [form, setForm] = useState(null)
  const [statusFilter, setStatusFilter] = useState('')

  const filtered = proposals.filter((p) => !statusFilter || p.status === statusFilter)
  const selected = proposals.find((p) => p.id === selectedId)
  const projectOf = (id) => projects.find((p) => p.id === id)
  const rfpOf = (id) => rfps.find((r) => r.id === id)

  const newProposal = () => ({
    id: nextProposalId(proposals),
    projectId: '',
    rfpId: '',
    title: '',
    status: PROPOSAL_STATUSES[0],
    owner: '',
    dueDate: '',
    submittedDate: '',
    price: null,
    costEstimate: null,
    versions: [],
    mapping: {},
    checklist: DEFAULT_CHECKLIST.map((name, i) => ({ id: `C${i + 1}`, name, done: false })),
  })

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            제안서 관리 <span className="pms-mock-badge">목업 · 더미 데이터</span>
          </h2>
          <p className="page-description">
            RFP별 제안서를 버전으로 관리하고, RFP 항목이 제안서 목차에 빠짐없이 대응되는지 점검합니다. 제출 서류 체크리스트로 제출 준비
            상태를 확인합니다.
          </p>
        </div>
        <button className="primary small" onClick={() => setForm({ proposal: newProposal(), isNew: true })}>
          + 제안서 등록
        </button>
      </div>

      <div className="pms-chips">
        <button className={!statusFilter ? 'on' : ''} onClick={() => setStatusFilter('')}>
          전체 <span>{proposals.length}</span>
        </button>
        {PROPOSAL_STATUSES.map((s) => (
          <button key={s} className={statusFilter === s ? 'on' : ''} onClick={() => setStatusFilter(s)}>
            {s} <span>{proposals.filter((p) => p.status === s).length}</span>
          </button>
        ))}
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>제안서 번호</th>
            <th>프로젝트</th>
            <th>제안서명</th>
            <th>최신 버전</th>
            <th>책임</th>
            <th>제출 마감</th>
            <th style={{ textAlign: 'right' }}>제안 금액(원)</th>
            <th>이익률</th>
            <th>RFP 대응</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          {filtered.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={10}>
                제안서가 없습니다.
              </td>
            </tr>
          )}
          {filtered.map((p) => {
            const rfp = rfpOf(p.rfpId)
            const total = rfp?.items.length ?? 0
            const done = rfp ? rfp.items.filter((i) => p.mapping[i.id]?.response).length : 0
            const margin = marginOf(p)
            return (
              <tr key={p.id} className={p.id === selectedId ? 'row-selected' : ''} style={{ cursor: 'pointer' }} onClick={() => setSelectedId(p.id)}>
                <td>{p.id}</td>
                <td className="name-cell">{projectOf(p.projectId)?.name ?? '-'}</td>
                <td className="name-cell">
                  <b>{p.title}</b>
                </td>
                <td>{p.versions[p.versions.length - 1]?.ver ?? '-'}</td>
                <td>{p.owner}</td>
                <td>
                  {p.dueDate.slice(5)}{' '}
                  {['작성 중', '내부 검토'].includes(p.status) && <span className={`pms-tag ${dday(p.dueDate).startsWith('D+') ? 'late' : ''}`}>{dday(p.dueDate)}</span>}
                </td>
                <td style={{ textAlign: 'right' }}>{won(p.price)}</td>
                <td>{margin !== null ? `${margin}%` : '-'}</td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <div className="pms-bar" style={{ width: 60 }}>
                      <span style={{ width: `${total ? Math.round((done / total) * 100) : 0}%` }} />
                    </div>
                    {done}/{total}
                  </div>
                </td>
                <td>
                  <span className={`pms-tag ${STATUS_CLASS[p.status]}`}>{p.status}</span>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>

      {selected ? (
        <ProposalDetail
          key={selected.id}
          proposal={selected}
          project={projectOf(selected.projectId)}
          rfp={rfpOf(selected.rfpId)}
          onEdit={(proposal) => setForm({ proposal, isNew: false })}
          onDeleted={() => setSelectedId(null)}
        />
      ) : (
        <p className="muted" style={{ marginTop: 18 }}>
          위 목록에서 제안서를 선택하면 상세가 표시됩니다.
        </p>
      )}

      {form && (
        <ProposalForm
          key={form.proposal.id}
          initial={form.proposal}
          isNew={form.isNew}
          projects={projects.filter((p) => !['수행 종료', '운영 중', '실패', '보류/취소'].includes(p.status))}
          rfps={rfps}
          onClose={() => setForm(null)}
          onSaved={(id) => setSelectedId(id)}
        />
      )}
    </div>
  )
}
