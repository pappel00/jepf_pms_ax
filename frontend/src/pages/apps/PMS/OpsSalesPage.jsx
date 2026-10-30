import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { SortTh, useSort } from './sortUtil'
import { customerOf, useCustomerState } from './customerStore'
import { dday, useOpsProject } from './OpsProjectBar'
import { MEMBERS, ME, TODAY } from './pmsStore'
import { formatKoreanWon } from './projectStore'
import { OPP_KINDS, OPP_STAGES, OPP_STAGE_PROB, deleteOpp, emptyOpp, saveOpp, useOpsOpps } from './opsStore'

const STAGE_CLASS = { 발굴: '', 제안: 'doing', 협상: 'manual', 수주: 'done', '실패/보류': 'late' }

function OppForm({ initial, project, onClose }) {
  const { customers, contacts } = useCustomerState()
  const cid = customerOf(customers, project ?? {})?.id
  const [o, setO] = useState(initial)
  const set = (patch) => setO((p) => ({ ...p, ...patch }))
  const err = !o.title.trim() ? '건명을 입력하세요.' : !o.amount ? '예상 금액(추정 가능)을 입력하세요.' : o.nextDate && !o.nextAction.trim() ? '다음 할 일 내용을 입력하세요.' : o.stage === '실패/보류' && !o.memo.trim() ? '실패/보류 사유를 비고에 적어 주세요.' : ''
  return (
    <Modal title={o.id ? `영업 기회 수정 — ${o.id}` : '영업 기회 등록'} onClose={onClose} width={640}>
      <div className="field">
        <label>건명<span className="req-mark"> *</span></label>
        <input value={o.title} onChange={(e) => set({ title: e.target.value })} placeholder="예) 2027년 유지보수 재계약" />
      </div>
      <div className="pms-form-grid">
        <div className="field">
          <label>유형</label>
          <select value={o.kind} onChange={(e) => set({ kind: e.target.value })}>
            {OPP_KINDS.map((k) => <option key={k}>{k}</option>)}
          </select>
        </div>
        <div className="field">
          <label>단계</label>
          <select value={o.stage} onChange={(e) => set({ stage: e.target.value, probability: OPP_STAGE_PROB[e.target.value] })}>
            {OPP_STAGES.map((k) => <option key={k}>{k}</option>)}
          </select>
        </div>
        <div className="field">
          <label>예상 금액 (원)<span className="req-mark"> *</span></label>
          <input inputMode="numeric" value={o.amount ? Number(o.amount).toLocaleString() : ''} onChange={(e) => { const d = e.target.value.replace(/[^\d]/g, ''); set({ amount: d ? Number(d) : null }) }} />
        </div>
        <div className="field">
          <label>확률도 (%)</label>
          <input type="number" min="0" max="100" value={o.probability} onChange={(e) => set({ probability: Math.min(100, Math.max(0, Number(e.target.value) || 0)) })} />
        </div>
        <div className="field">
          <label>영업 담당자</label>
          <select value={o.owner} onChange={(e) => set({ owner: e.target.value })}>
            {MEMBERS.map((m) => <option key={m.name}>{m.name}</option>)}
          </select>
        </div>
        <div className="field">
          <label>예상 계약일</label>
          <input type="date" value={o.expectedDate} onChange={(e) => set({ expectedDate: e.target.value })} />
        </div>
        <div className="field">
          <label>다음 할 일</label>
          <input value={o.nextAction} onChange={(e) => set({ nextAction: e.target.value })} />
        </div>
        <div className="field">
          <label>예정일</label>
          <input type="date" value={o.nextDate} onChange={(e) => set({ nextDate: e.target.value })} />
        </div>
      </div>
      <div className="field">
        <label>비고</label>
        <textarea rows={3} value={o.memo} onChange={(e) => set({ memo: e.target.value })} />
      </div>
      <div className="field">
        <label>고객사 담당자 (요청자)</label>
        <select value={o.contactId} onChange={(e) => set({ contactId: e.target.value })}>
          <option value="">선택 안 함</option>
          {contacts.filter((t) => t.customerId === cid).map((t) => (
            <option key={t.id} value={t.id}>{t.name}{t.title ? ` ${t.title}` : ''} · {t.phone || t.email}</option>
          ))}
        </select>
      </div>
      {o.sourceIncidentId && <p className="muted">출처 운영 이슈: {o.sourceIncidentId}</p>}
      {o.sourceNextId && <p className="muted">출처 수행 단계 차기 사업 기회: {o.sourceNextId}</p>}
      {err && <p style={{ color: '#c0392b' }}>{err}</p>}
      <button className="primary" disabled={!!err} onClick={() => { saveOpp({ ...o, title: o.title.trim() }); onClose() }}>저장</button>
    </Modal>
  )
}

/** 운영 영업 관리 — 운영 중 생기는 재계약·추가 개발·유상 개선 같은 영업 기회를 단계별로 관리한다. */
export default function OpsSalesPage() {
  const { project, bar } = useOpsProject()
  const all = useOpsOpps()
  const { contacts } = useCustomerState()
  const [stage, setStage] = useState('')
  const [edit, setEdit] = useState(null)

  const list = all.filter((o) => o.projectId === project?.id)
  const rows = list.filter((o) => !stage || o.stage === stage)
  const { sort, onSort, sorted } = useSort(rows, {
    id: (o) => o.id,
    kind: (o) => o.kind,
    title: (o) => o.title,
    stage: (o) => OPP_STAGES.indexOf(o.stage),
    amount: (o) => o.amount || 0,
    probability: (o) => o.probability,
    owner: (o) => o.owner,
    expected: (o) => o.expectedDate,
    next: (o) => o.nextDate,
  })
  const active = list.filter((o) => !['수주', '실패/보류'].includes(o.stage))
  const won = list.filter((o) => o.stage === '수주')
  const decided = list.filter((o) => ['수주', '실패/보류'].includes(o.stage))
  const endIn = project ? Math.round((new Date(project.operation.endDate) - new Date(TODAY)) / 86400000) : null
  const hasRenewal = list.some((o) => o.kind === '재계약·갱신' && o.stage !== '실패/보류')

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>운영 영업 관리</h2>
          <p className="page-description">운영 중에 생기는 재계약·갱신, 추가 개발, 유상 개선, 증설 같은 영업 기회를 발굴 → 제안 → 협상 → 수주 단계로 관리합니다. 운영 이슈의 개선 요청에서 바로 등록할 수 있고, 운영 종료일이 다가오면 재계약 등록을 안내합니다.</p>
        </div>
        <button className="primary small" disabled={!project} onClick={() => setEdit(emptyOpp(project.id, ME))}>＋ 영업 기회 등록</button>
      </div>
      {bar}
      {edit && <OppForm initial={edit} project={project} onClose={() => setEdit(null)} />}
      {project && (
        <>
          {endIn !== null && endIn <= 120 && !hasRenewal && (
            <p style={{ background: '#fff4e5', border: '1px solid #f0c27a', borderRadius: 8, padding: '8px 12px' }}>
              ⚠ 운영 종료일 {project.operation.endDate}까지 {endIn}일 남았습니다. 재계약·갱신 영업 기회가 아직 없습니다.{' '}
              <button className="link-btn" onClick={() => setEdit({ ...emptyOpp(project.id, ME), kind: '재계약·갱신', title: `${project.operation.endDate.slice(0, 4) * 1 + 1}년 유지보수 재계약`, expectedDate: project.operation.endDate, amount: project.operation.contractAmount })}>재계약 등록</button>
            </p>
          )}
          <div className="pms-summary">
            <div className="pms-stat"><div className="label">진행 중 영업 기회</div><div className="value">{active.length}</div></div>
            <div className="pms-stat"><div className="label">진행 금액</div><div className="value" style={{ fontSize: 18 }}>{formatKoreanWon(active.reduce((s, o) => s + (o.amount ?? 0), 0))}</div></div>
            <div className="pms-stat"><div className="label">가중 예상 금액 (금액 × 확률)</div><div className="value" style={{ fontSize: 18 }}>{formatKoreanWon(Math.round(active.reduce((s, o) => s + (o.amount ?? 0) * (o.probability / 100), 0)))}</div></div>
            <div className="pms-stat ok"><div className="label">수주 금액</div><div className="value" style={{ fontSize: 18 }}>{formatKoreanWon(won.reduce((s, o) => s + (o.amount ?? 0), 0))}</div></div>
            <div className="pms-stat"><div className="label">수주율 (결과 확정 기준)</div><div className="value">{decided.length ? `${Math.round((won.length / decided.length) * 100)}%` : '-'}</div></div>
          </div>
          <div className="pms-seg" style={{ margin: '8px 0' }}>
            <button className={stage === '' ? 'on' : ''} onClick={() => setStage('')}>전체 {list.length}</button>
            {OPP_STAGES.map((s) => (
              <button key={s} className={stage === s ? 'on' : ''} onClick={() => setStage(s)}>{s} {list.filter((o) => o.stage === s).length}</button>
            ))}
          </div>
          <table className="data-table pms-wbs">
            <thead>
              <tr><SortTh k="id" sort={sort} onSort={onSort}>코드</SortTh><SortTh k="kind" sort={sort} onSort={onSort}>유형</SortTh><SortTh k="title" sort={sort} onSort={onSort}>건명</SortTh><SortTh k="stage" sort={sort} onSort={onSort}>단계</SortTh><SortTh k="amount" sort={sort} onSort={onSort} style={{ textAlign: 'right' }}>예상 금액(원)</SortTh><SortTh k="probability" sort={sort} onSort={onSort}>확률</SortTh><SortTh k="owner" sort={sort} onSort={onSort}>담당</SortTh><SortTh k="expected" sort={sort} onSort={onSort}>예상 계약일</SortTh><SortTh k="next" sort={sort} onSort={onSort}>다음 할 일</SortTh><th></th></tr>
            </thead>
            <tbody>
              {rows.length === 0 && <tr><td className="empty-cell" colSpan={10}>영업 기회가 없습니다.</td></tr>}
              {sorted.map((o) => (
                <tr key={o.id}>
                  <td>{o.id}</td>
                  <td><span className="pms-tag copy">{o.kind}</span></td>
                  <td className="name-cell">
                    <b>{o.title}</b>
                    {o.sourceIncidentId && <span className="pms-tag manual" style={{ marginLeft: 6 }}>이슈 {o.sourceIncidentId}</span>}
                    {o.sourceNextId && <span className="pms-tag done" style={{ marginLeft: 6 }}>수행 단계 {o.sourceNextId}</span>}
                    {o.contactId && <div className="muted">고객 담당 {contacts.find((t) => t.id === o.contactId)?.name} · {contacts.find((t) => t.id === o.contactId)?.phone}</div>}
                    {o.memo && <div className="muted">{o.memo}</div>}
                  </td>
                  <td><span className={`pms-tag ${STAGE_CLASS[o.stage]}`}>{o.stage}</span></td>
                  <td style={{ textAlign: 'right' }}>{o.amount ? o.amount.toLocaleString() : '-'}</td>
                  <td>{o.probability}%</td>
                  <td>{o.owner}</td>
                  <td>{o.expectedDate || '-'}</td>
                  <td>
                    {o.nextAction ? <>{o.nextAction} {o.nextDate && <span className={`pms-tag ${o.nextDate < TODAY ? 'late' : ''}`}>{o.nextDate.slice(5)} ({dday(o.nextDate)})</span>}</> : <span className="muted">-</span>}
                  </td>
                  <td>
                    <button className="link-btn" onClick={() => setEdit(o)}>수정</button>{' '}
                    <button className="link-btn" onClick={() => window.confirm('이 영업 기회를 삭제할까요?') && deleteOpp(o.id)}>삭제</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="pms-field-hint">수주한 영업 기회가 신규 프로젝트로 이어지는 연결(영업단계 프로젝트 자동 생성)은 후속 과제입니다.</div>
        </>
      )}
    </div>
  )
}
