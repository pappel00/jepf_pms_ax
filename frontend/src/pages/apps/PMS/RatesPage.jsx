import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { collectAssignments } from './assignmentStore'
import { MEMBERS, TODAY } from './pmsStore'
import { formatKoreanWon, saveProject, useProjects } from './projectStore'
import {
  DIFFICULTIES,
  GRADES,
  RATE_JOBS,
  costOf,
  deleteRate,
  overlaps,
  rateFor,
  saveRate,
  setDifficulty,
  setMemberGrade,
  useRateState,
} from './rateStore'
import { orgOf } from './teamConfig'

const won = (n) => `${Math.round(n).toLocaleString()}원`
const fmt1 = (n) => (Math.round(n * 10) / 10).toString()
const NOW = TODAY.slice(0, 7)
const isActive = (r) => r.from <= TODAY && (!r.to || r.to >= TODAY)

/* ---------- 단가 등록/수정 ---------- */
function RateForm({ initial, rates, onClose }) {
  const [form, setForm] = useState(initial ?? { job: '개발', grade: '중급', unit: '', from: TODAY, to: '' })
  const set = (patch) => setForm((f) => ({ ...f, ...patch }))
  const conflict = overlaps(rates, form)
  const error =
    !(Number(form.unit) > 0) ? '단가를 입력하세요.' : !form.from ? '적용 시작일을 입력하세요.' : form.to && form.to < form.from ? '종료일이 시작일보다 빠릅니다.' : conflict.length ? `같은 직무·등급의 적용 기간이 겹칩니다 (${conflict[0].from} ~ ${conflict[0].to || '계속'}).` : ''

  return (
    <Modal title={initial ? '단가 수정' : '단가 등록'} onClose={onClose} width={520}>
      <div className="field">
        <label>직무<span className="req-mark"> *</span></label>
        <select value={form.job} onChange={(e) => set({ job: e.target.value })}>
          {RATE_JOBS.map((j) => <option key={j}>{j}</option>)}
        </select>
      </div>
      <div className="field">
        <label>등급<span className="req-mark"> *</span></label>
        <select value={form.grade} onChange={(e) => set({ grade: e.target.value })}>
          {GRADES.map((g) => <option key={g}>{g}</option>)}
        </select>
      </div>
      <div className="field">
        <label>M/M 단가 (원)<span className="req-mark"> *</span></label>
        <input type="number" min="0" step="10000" value={form.unit} onChange={(e) => set({ unit: e.target.value })} />
        <div className="pms-field-hint">1 M/M(1인 1개월) 당 금액. 직무·등급별로 적용 기간마다 하나씩 둡니다.</div>
      </div>
      <div className="field">
        <label>적용 시작일<span className="req-mark"> *</span></label>
        <input type="date" value={form.from} onChange={(e) => set({ from: e.target.value })} />
      </div>
      <div className="field">
        <label>적용 종료일</label>
        <input type="date" value={form.to} onChange={(e) => set({ to: e.target.value })} />
        <div className="pms-field-hint">비우면 계속 적용합니다. 단가를 올릴 때는 기존 단가를 종료하고 새 단가를 등록하세요(과거 원가는 그대로 유지).</div>
      </div>
      {error && <p style={{ color: '#c0392b' }}>{error}</p>}
      <button
        className="primary"
        disabled={!!error}
        onClick={() => {
          saveRate({ ...form, unit: Number(form.unit) })
          onClose()
        }}
      >
        저장
      </button>
    </Modal>
  )
}

/* ---------- 탭 1: 단가표 ---------- */
function RateTab({ state }) {
  const [job, setJob] = useState('')
  const [onlyActive, setOnlyActive] = useState(true)
  const [edit, setEdit] = useState(null)
  const rows = state.rates.filter((r) => (!job || r.job === job) && (!onlyActive || isActive(r)))
  const d = state.difficulty

  return (
    <div>
      <div className="search-bar">
        <div className="search-field">
          <label>직무</label>
          <select value={job} onChange={(e) => setJob(e.target.value)}>
            <option value="">전체</option>
            {RATE_JOBS.map((j) => <option key={j}>{j}</option>)}
          </select>
        </div>
        <label style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" checked={onlyActive} onChange={(e) => setOnlyActive(e.target.checked)} /> 현재 적용 중만
        </label>
        <button className="primary small" onClick={() => setEdit({})}>＋ 단가 등록</button>
      </div>
      {edit && <RateForm initial={edit.id ? edit : null} rates={state.rates} onClose={() => setEdit(null)} />}

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>직무</th>
            <th>등급</th>
            <th style={{ textAlign: 'right' }}>M/M 단가</th>
            <th>적용 기간</th>
            <th>상태</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 && <tr><td className="empty-cell" colSpan={6}>단가가 없습니다.</td></tr>}
          {rows.map((r) => (
            <tr key={r.id}>
              <td><b>{r.job}</b></td>
              <td>{r.grade}</td>
              <td style={{ textAlign: 'right' }}>{won(r.unit)}</td>
              <td>{r.from} ~ {r.to || '계속'}</td>
              <td>{isActive(r) ? <span className="pms-tag done">적용 중</span> : r.from > TODAY ? <span className="pms-tag manual">적용 예정</span> : <span className="pms-tag">종료</span>}</td>
              <td>
                <button className="link-btn" onClick={() => setEdit(r)}>수정</button>{' '}
                <button className="link-btn" onClick={() => window.confirm('이 단가를 삭제할까요? 이미 계산된 원가에도 영향을 줍니다.') && deleteRate(r.id)}>삭제</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="pms-panel" style={{ marginTop: 16 }}>
        <h3>난이도 가중치 <span className="muted" style={{ fontWeight: 400, fontSize: 12 }}>· 적용 여부 협의 중</span></h3>
        <label style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <input type="checkbox" checked={d.enabled} onChange={(e) => setDifficulty({ enabled: e.target.checked })} />
          RFP 평가 확정 M/D로 원가를 산정할 때 항목 난이도(하/중/상)별 배수를 곱한다
        </label>
        <div style={{ display: 'flex', gap: 14, marginTop: 8, opacity: d.enabled ? 1 : 0.5 }}>
          {DIFFICULTIES.map((k) => (
            <div key={k} className="search-field">
              <label>난이도 {k}</label>
              <input type="number" min="0.1" step="0.1" disabled={!d.enabled} style={{ width: 80 }} value={d[k]} onChange={(e) => setDifficulty({ [k]: Number(e.target.value) || 1 })} />
            </div>
          ))}
        </div>
        <div className="pms-field-hint">
          RFP 평가 난이도는 현재 참고 정보입니다. 켜면 「확정 M/D × 난이도 배수」로 가중합니다(RFP 평가 확정 M/D 단계와 함께 구현 예정이라 이 화면의 투입 기준 원가에는 영향이 없습니다).
        </div>
      </div>
    </div>
  )
}

/* ---------- 탭 2: 인력 등급 ---------- */
function GradeTab({ state, assignments }) {
  return (
    <div>
      <p className="muted" style={{ marginTop: 0 }}>
        인력별 등급을 지정해야 투입 계획에 단가가 적용됩니다. 등급은 인력 마스터 정보이며, 등급이 바뀌면 이후 월부터 적용하는 이력 관리가 필요합니다(현재는 단일 값).
      </p>
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>인력</th>
            <th>소속</th>
            <th>기본 직무</th>
            <th>등급</th>
            <th>투입 중 프로젝트</th>
            <th>단가 적용 현황</th>
          </tr>
        </thead>
        <tbody>
          {MEMBERS.map((m) => {
            const mine = assignments.filter((a) => a.name === m.name)
            const grade = state.grades[m.name] ?? ''
            const missing = mine.filter((a) => costOf(a, state.rates, state.grades).missing > 0)
            return (
              <tr key={m.name}>
                <td><b>{m.name}</b></td>
                <td>{orgOf(m.name)}</td>
                <td>{m.role}</td>
                <td>
                  <select value={grade} onChange={(e) => setMemberGrade(m.name, e.target.value)}>
                    <option value="">미지정</option>
                    {GRADES.map((g) => <option key={g}>{g}</option>)}
                  </select>
                </td>
                <td>{mine.length ? mine.map((a) => `${a.projectName}(${a.role})`).join(', ') : <span className="muted">-</span>}</td>
                <td>
                  {mine.length === 0 ? <span className="muted">-</span> : !grade ? <span className="pms-tag late">등급 미지정</span> : missing.length ? <span className="pms-tag late">단가 없음 {missing.map((a) => a.role).join(', ')}</span> : <span className="pms-tag done">정상</span>}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

/* ---------- 탭 3: 프로젝트 원가 ---------- */
function CostTab({ state, projects, assignments }) {
  const [sel, setSel] = useState('')
  const ids = [...new Set(assignments.map((a) => a.projectId))]
  const pid = sel || ids[0] || ''
  const mine = assignments.filter((a) => a.projectId === pid)
  const project = projects.find((p) => p.id === mine[0]?.baseProjectId)
  const rows = mine.map((a) => ({ a, ...costOf(a, state.rates, state.grades) }))
  const totalMm = rows.reduce((s, r) => s + r.mm, 0)
  const totalCost = rows.reduce((s, r) => s + r.cost, 0)
  const missing = rows.filter((r) => r.missing > 0)
  const margin = project?.amount && totalCost ? Math.round(((project.amount - totalCost) / project.amount) * 1000) / 10 : null
  const byRole = {}
  rows.forEach((r) => {
    byRole[r.a.role] = (byRole[r.a.role] ?? 0) + r.cost
  })

  if (ids.length === 0) return <p className="muted">수행·운영 중인 프로젝트의 투입 인력이 없습니다.</p>

  return (
    <div>
      <div className="search-bar">
        <div className="search-field">
          <label>프로젝트</label>
          <select value={pid} onChange={(e) => setSel(e.target.value)}>
            {ids.map((id) => <option key={id} value={id}>{assignments.find((a) => a.projectId === id).projectName}</option>)}
          </select>
        </div>
      </div>

      <div className="pms-summary">
        <div className="pms-stat"><div className="label">계획 M/M 합계</div><div className="value">{fmt1(totalMm)}</div></div>
        <div className="pms-stat"><div className="label">예상 인건비</div><div className="value" style={{ fontSize: 18 }}>{formatKoreanWon(totalCost)}</div></div>
        <div className="pms-stat"><div className="label">발주금액</div><div className="value" style={{ fontSize: 18 }}>{formatKoreanWon(project?.amount)}</div></div>
        <div className={`pms-stat ${margin === null ? '' : margin < 10 ? 'danger' : margin < 20 ? 'warn' : 'ok'}`}>
          <div className="label">인건비 기준 이익률</div>
          <div className="value">{margin === null ? '-' : `${margin}%`}</div>
        </div>
        <div className={`pms-stat ${missing.length ? 'danger' : ''}`}><div className="label">단가 누락 인력</div><div className="value">{missing.length}</div></div>
      </div>

      {missing.length > 0 && (
        <p style={{ color: '#c0392b' }}>
          ⚠ {missing.map((r) => `${r.a.name}(${r.grade ?? '등급 미지정'}·${r.a.role})`).join(', ')} 은(는) 단가를 찾지 못해 인건비에서 제외되었습니다. 「인력 등급」 또는 「단가표」를 확인하세요.
        </p>
      )}

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>인력</th>
            <th>역할</th>
            <th>등급</th>
            <th>투입 기간</th>
            <th style={{ textAlign: 'right' }}>계획 M/M</th>
            <th style={{ textAlign: 'right' }}>현재 단가</th>
            <th style={{ textAlign: 'right' }}>예상 인건비</th>
          </tr>
        </thead>
        <tbody>
          {rows.map(({ a, mm, cost, missing: miss, grade }) => {
            const unit = rateFor(state.rates, a.role, grade, NOW)
            return (
              <tr key={a.key}>
                <td><b>{a.name}</b> {orgOf(a.name) === '협력사' && <span className="pms-tag manual">협력</span>}</td>
                <td>{a.role}</td>
                <td>{grade ?? <span className="pms-tag late">미지정</span>}</td>
                <td>{a.from} ~ {a.to}</td>
                <td style={{ textAlign: 'right' }}>{fmt1(mm)}</td>
                <td style={{ textAlign: 'right' }}>{unit == null ? <span className="pms-tag late">없음</span> : won(unit)}</td>
                <td style={{ textAlign: 'right' }}>{miss > 0 && <span className="pms-tag late" title={`${miss}개월 단가 없음`}>누락 {miss}개월</span>} {won(cost)}</td>
              </tr>
            )
          })}
          <tr className="group-row">
            <td colSpan={4}><b>합계</b></td>
            <td style={{ textAlign: 'right' }}><b>{fmt1(totalMm)}</b></td>
            <td></td>
            <td style={{ textAlign: 'right' }}><b>{won(totalCost)}</b></td>
          </tr>
        </tbody>
      </table>
      <div className="pms-field-hint">
        월별 계획 M/M × 그 달에 적용되는 단가의 합계입니다(M/M 미입력 인력은 투입률 100%로 가정). 직무별: {Object.entries(byRole).map(([k, v]) => `${k} ${formatKoreanWon(v)}`).join(' · ') || '-'}.
        {project?.costEstimate ? ` 프로젝트 관리의 예상 원가(수동 입력) ${formatKoreanWon(project.costEstimate)}과 비교해 반영 여부를 결정합니다.` : ''}
      </div>
      {project?.costSource && (
        <div className="pms-field-hint">현재 예상 원가 출처: <b>{project.costSource}</b></div>
      )}
      <button
        className="secondary small"
        style={{ marginTop: 8 }}
        disabled={!project || !totalCost}
        onClick={() => {
          if (missing.length && !window.confirm('단가가 없는 인력은 제외된 금액입니다. 그래도 반영할까요?')) return
          if (project.costEstimate && !window.confirm(`기존 예상 원가 ${formatKoreanWon(project.costEstimate)}을(를) ${formatKoreanWon(totalCost)}으로 바꿀까요?`)) return
          saveProject({ ...project, costEstimate: totalCost, costSource: `단가 환산 인건비 (${TODAY} 기준, 투입 ${fmt1(totalMm)} M/M)` })
        }}
      >
        예상 원가에 반영
      </button>
      <span className="pms-field-hint" style={{ marginLeft: 8 }}>프로젝트 관리·제안서의 예상 원가/이익률에 반영됩니다. 이후 수동 수정도 가능합니다.</span>
    </div>
  )
}

/**
 * 단가 관리 — 직무·등급별 M/M 단가(적용 기간), 인력 등급, 투입 계획 기준 프로젝트 예상 인건비.
 */
export default function RatesPage() {
  const state = useRateState()
  const projects = useProjects()
  const [tab, setTab] = useState('rate')
  const assignments = collectAssignments(projects)

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>단가 관리 <span className="pms-mock-badge">목업 · 더미 데이터</span></h2>
          <p className="page-description">
            직무·등급별 M/M 단가를 적용 기간과 함께 관리하고, 투입 인력의 계획 M/M에 단가를 곱해 프로젝트 예상 인건비(예상 원가)를 환산합니다.
          </p>
        </div>
      </div>
      <div className="tabs-bar">
        {[['rate', '단가표'], ['grade', '인력 등급'], ['cost', '프로젝트 원가']].map(([k, label]) => (
          <button key={k} className={`tab-btn ${tab === k ? 'active' : ''}`} onClick={() => setTab(k)}>{label}</button>
        ))}
      </div>
      <div className="tab-content">
        {tab === 'rate' && <RateTab state={state} />}
        {tab === 'grade' && <GradeTab state={state} assignments={assignments} />}
        {tab === 'cost' && <CostTab state={state} projects={projects} assignments={assignments} />}
      </div>
    </div>
  )
}
