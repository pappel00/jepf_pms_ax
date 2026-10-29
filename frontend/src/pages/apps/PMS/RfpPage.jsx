import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { evalDoneOf, useEvals } from './evalStore'
import { MEMBERS, TODAY } from './pmsStore'
import { useProjects } from './projectStore'
import {
  INTAKE_METHODS,
  REQ_CATEGORIES,
  ROUNDS,
  categoryName,
  deleteItem,
  deleteRfp,
  evalProgressOf,
  importParsedItems,
  importSampleItems,
  parseRfpDocument,
  nextItemCode,
  nextRfpId,
  rfpStatusOf,
  saveItem,
  saveRfp,
  setEvaluators,
  useRfps,
} from './rfpStore'

const STATUS_CLASS = { 접수: '', '항목 정리 중': 'manual', '평가 중': 'doing', '평가 완료': 'done' }
const dday = (date) => {
  if (!date) return ''
  const diff = Math.round((new Date(date) - new Date(TODAY)) / 86400000)
  return diff === 0 ? 'D-Day' : diff > 0 ? `D-${diff}` : `D+${-diff}`
}

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

/* ---------- RFP 등록(문서 업로드) ---------- */
function RfpForm({ initial, isNew, projects, onClose, onSaved }) {
  const [r, setR] = useState(initial)
  const [method, setMethod] = useState('excel')
  const [errors, setErrors] = useState({})
  const set = (k, v) => setR((p) => ({ ...p, [k]: v }))
  // 문서 자동 분해: 파일 → 분석(미리보기) → 분류별로 포함 여부를 고르고 저장
  const [file, setFile] = useState(null)
  const [split, setSplit] = useState(false)
  const [parsing, setParsing] = useState(false)
  const [preview, setPreview] = useState(null)
  const [catOff, setCatOff] = useState(new Set())
  const analyze = async () => {
    if (!file) return window.alert('hwpx 문서를 선택하세요.')
    setParsing(true)
    try {
      setPreview(await parseRfpDocument(file, split))
      setCatOff(new Set())
    } catch (err) {
      setPreview(null)
      window.alert(`문서를 분석하지 못했습니다: ${err.message}`)
    } finally {
      setParsing(false)
    }
  }
  const selectedItems = preview ? preview.items.filter((i) => !catOff.has(i.categoryCode)) : []

  const submit = async () => {
    const e = {}
    if (!r.projectId) e.projectId = '프로젝트를 선택하세요.'
    if (!r.title.trim()) e.title = 'RFP명을 입력하세요.'
    if (!r.receivedDate) e.receivedDate = '접수일을 선택하세요.'
    if (isNew && method === 'auto' && !preview) e.intake = '문서를 선택하고 「문서 분석」으로 항목을 확인하세요.'
    setErrors(e)
    if (Object.keys(e).length) return
    let saved
    try {
      saved = await saveRfp({ ...r, title: r.title.trim() })
    } catch (err) {
      window.alert(`RFP를 저장하지 못했습니다: ${err.message}`)
      return
    }
    try {
      if (isNew && method === 'excel') await importSampleItems(saved.id)
      if (isNew && method === 'auto' && selectedItems.length) await importParsedItems(saved.id, selectedItems)
    } catch (err) {
      // RFP 는 이미 등록되어 있다 — 등록된 항목까지만 남고, 나머지는 RFP 상세에서 다시 추가할 수 있다.
      window.alert(`RFP는 등록했지만 항목 등록 중 오류가 발생했습니다.
${err.message}`)
    }
    onSaved(saved.id)
    onClose()
  }

  return (
    <Modal title={isNew ? 'RFP 등록 (문서 업로드)' : `RFP 수정 — ${r.id}`} onClose={onClose} width={720}>
      <div className="pms-form-grid">
        <Field label="프로젝트" required error={errors.projectId} hint="영업 단계 프로젝트에 연결됩니다 (1 프로젝트 : N RFP)" wide>
          <select value={r.projectId} onChange={(e) => set('projectId', e.target.value)}>
            <option value="">선택하세요</option>
            {projects.map((p) => (
              <option key={p.id} value={p.id}>
                {p.id} · {p.name} ({p.status})
              </option>
            ))}
          </select>
        </Field>
        <Field label="RFP명" required error={errors.title} wide>
          <input value={r.title} onChange={(e) => set('title', e.target.value)} placeholder="예) ○○ 구축 제안요청서" />
        </Field>
        <Field label="차수">
          <select value={r.round} onChange={(e) => set('round', e.target.value)}>
            {ROUNDS.map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
        </Field>
        <Field label="접수일" required error={errors.receivedDate}>
          <input type="date" value={r.receivedDate} onChange={(e) => set('receivedDate', e.target.value)} />
        </Field>
        <Field label="제안서 제출 마감일">
          <input type="date" value={r.dueDate} onChange={(e) => set('dueDate', e.target.value)} />
        </Field>
      </div>

      {isNew && (
        <>
          <div className="pms-form-section">항목 정리 방식</div>
          {INTAKE_METHODS.map((m) => (
            <label key={m.value} className={`pms-radio ${m.disabled ? 'disabled' : ''}`}>
              <input type="radio" name="intake" disabled={m.disabled} checked={method === m.value} onChange={() => setMethod(m.value)} />
              <span>
                <b>{m.label}</b>
                <small>{m.desc}</small>
              </span>
            </label>
          ))}
          {method === 'excel' && (
            <div className="pms-hint" style={{ boxShadow: 'none', background: '#f4f5f7' }}>
              목업: 저장하면 샘플 항목 6건이 엑셀에서 읽혀 등록된 것으로 처리합니다.
            </div>
          )}
          {method === 'auto' && (
            <div className="pms-hint" style={{ boxShadow: 'none', background: '#f4f5f7' }}>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                <input
                  type="file"
                  accept=".hwpx"
                  onChange={(e) => {
                    setFile(e.target.files?.[0] ?? null)
                    setPreview(null)
                  }}
                />
                <label style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                  <input type="checkbox" checked={split} onChange={(e) => { setSplit(e.target.checked); setPreview(null) }} />
                  세부내용을 줄 단위로 분해
                </label>
                <button className="secondary small" disabled={!file || parsing} onClick={analyze}>
                  {parsing ? '분석 중…' : '문서 분석'}
                </button>
              </div>
              <div className="pms-field-hint">
                요구사항 상세표(구분·고유번호·명칭·정의·세부내용)를 읽습니다. 줄 단위 분해를 켜면 세부내용의 줄마다 항목이 만들어져 건수가 크게 늘어납니다. 원문 파일은 저장하지 않습니다.
              </div>
              {errors.intake && <div className="pms-field-error">{errors.intake}</div>}
              {preview && (
                <div style={{ marginTop: 8 }}>
                  <b>
                    요구사항 {preview.requirementCount}건 → 항목 {preview.itemCount}건
                  </b>{' '}
                  <span className="muted">(등록 대상 {selectedItems.length}건)</span>
                  {preview.warnings.length > 0 && (
                    <ul style={{ margin: '4px 0', color: '#b3261e', fontSize: 12 }}>
                      {preview.warnings.slice(0, 8).map((w) => (
                        <li key={w}>{w}</li>
                      ))}
                    </ul>
                  )}
                  <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', margin: '6px 0' }}>
                    {[...new Set(preview.items.map((i) => i.categoryCode))].map((c) => (
                      <label key={c} style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
                        <input
                          type="checkbox"
                          checked={!catOff.has(c)}
                          onChange={() =>
                            setCatOff((prev) => {
                              const next = new Set(prev)
                              next.has(c) ? next.delete(c) : next.add(c)
                              return next
                            })
                          }
                        />
                        {c} {categoryName(c) !== c ? `· ${categoryName(c)}` : ''} ({preview.items.filter((i) => i.categoryCode === c).length})
                      </label>
                    ))}
                  </div>
                  <div style={{ maxHeight: 220, overflow: 'auto', background: '#fff', border: '1px solid #e3e6ee' }}>
                    <table className="data-table">
                      <thead>
                        <tr>
                          <th>코드</th>
                          <th>분류</th>
                          <th>항목명</th>
                        </tr>
                      </thead>
                      <tbody>
                        {selectedItems.slice(0, 100).map((i) => (
                          <tr key={i.itemCode}>
                            <td>{i.itemCode}</td>
                            <td>{i.categoryCode}</td>
                            <td>{i.itemName}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                  {selectedItems.length > 100 && <div className="pms-field-hint">미리보기는 처음 100건만 표시합니다. 등록 후 항목 목록에서 수정·삭제할 수 있습니다.</div>}
                </div>
              )}
            </div>
          )}
        </>
      )}

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

/* ---------- 항목 추가/수정 ---------- */
function ItemForm({ rfp, initial, onClose }) {
  const isNew = !initial.id
  const [it, setIt] = useState(initial)
  const [errors, setErrors] = useState({})
  const set = (k, v) => setIt((p) => ({ ...p, [k]: v }))

  const submit = () => {
    const e = {}
    if (!it.name.trim()) e.name = '항목명을 입력하세요.'
    if (!it.content.trim()) e.content = '요구사항 내용을 입력하세요.'
    setErrors(e)
    if (Object.keys(e).length) return
    saveItem(rfp.id, {
      ...it,
      id: it.id || `${rfp.id}-I${Date.now()}`,
      code: it.code || nextItemCode(rfp, it.category),
      page: it.page === '' ? null : Number(it.page),
    })
    onClose()
  }

  return (
    <Modal title={isNew ? '항목 추가' : `항목 수정 — ${it.code}`} onClose={onClose} width={640}>
      <div className="pms-form-grid">
        <Field label="요구사항 분류" required hint="발주사 RFP 원문 기준 분류 (기능/비기능 구분은 평가 단계에서 PM이 정합니다)">
          <select
            value={it.category}
            disabled={!isNew}
            onChange={(e) => set('category', e.target.value)}
          >
            {REQ_CATEGORIES.map((c) => (
              <option key={c.code} value={c.code}>
                {c.code} · {c.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="영역(중분류)">
          <input value={it.area} onChange={(e) => set('area', e.target.value)} placeholder="예) 문서 관리" />
        </Field>
        <Field label="항목명" required error={errors.name} wide>
          <input value={it.name} onChange={(e) => set('name', e.target.value)} />
        </Field>
        <Field label="요구사항 내용 (원문)" required error={errors.content} wide>
          <textarea rows={3} value={it.content} onChange={(e) => set('content', e.target.value)} />
        </Field>
        <Field label="원문 위치 (페이지)">
          <input type="number" min="1" value={it.page ?? ''} onChange={(e) => set('page', e.target.value)} />
        </Field>
        <Field label="필수 여부">
          <select value={it.mandatory ? 'Y' : 'N'} onChange={(e) => set('mandatory', e.target.value === 'Y')}>
            <option value="Y">필수</option>
            <option value="N">선택</option>
          </select>
        </Field>
      </div>
      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <button className="primary" style={{ flex: 1 }} onClick={submit}>
          저장
        </button>
        <button className="secondary" style={{ marginTop: 0 }} onClick={onClose}>
          취소
        </button>
        {!isNew && (
          <button
            className="secondary"
            style={{ marginTop: 0, color: '#d32f2f' }}
            onClick={() => {
              if (window.confirm(`"${it.name}" 항목을 삭제할까요?`)) {
                deleteItem(rfp.id, it.id)
                onClose()
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

/* ---------- 평가자 지정 ---------- */
function EvaluatorForm({ rfp, onClose }) {
  const [picked, setPicked] = useState(new Set(rfp.evaluators))
  const toggle = (n) =>
    setPicked((prev) => {
      const next = new Set(prev)
      next.has(n) ? next.delete(n) : next.add(n)
      return next
    })
  return (
    <Modal title="평가자 지정" onClose={onClose} width={520}>
      <p style={{ marginTop: 0, fontSize: 13 }}>
        RFP 항목을 평가할 수행 조직 PM을 지정합니다. <b>1명도, 여러 명도</b> 가능합니다. 지정된 평가자는 「RFP 평가」 화면에서
        항목별로 기능/비기능 구분, 구현 여부, 예상 M/D를 입력합니다.
      </p>
      {MEMBERS.map((m) => (
        <label key={m.name} className="pms-radio">
          <input type="checkbox" checked={picked.has(m.name)} onChange={() => toggle(m.name)} />
          <span>
            <b>{m.name}</b>
            <small>
              {m.role} · {m.org}
            </small>
          </span>
        </label>
      ))}
      <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
        <button
          className="primary"
          style={{ flex: 1 }}
          onClick={() => {
            setEvaluators(rfp.id, MEMBERS.map((m) => m.name).filter((n) => picked.has(n)))
            onClose()
          }}
        >
          저장 ({picked.size}명)
        </button>
        <button className="secondary" style={{ marginTop: 0 }} onClick={onClose}>
          취소
        </button>
      </div>
    </Modal>
  )
}

/* ---------- RFP 상세: 항목 목록 ---------- */
function RfpDetail({ rfp, project, onDeleted, onEditRfp }) {
  const [catFilter, setCatFilter] = useState('')
  const [keyword, setKeyword] = useState('')
  const [mandatoryOnly, setMandatoryOnly] = useState(false)
  const [collapsed, setCollapsed] = useState(new Set())
  const [itemForm, setItemForm] = useState(null)
  const [evalForm, setEvalForm] = useState(false)

  const status = rfpStatusOf(rfp)
  const n = rfp.evaluators.length
  const items = rfp.items.filter(
    (i) =>
      (!catFilter || i.category === catFilter) &&
      (!mandatoryOnly || i.mandatory) &&
      (!keyword || `${i.code} ${i.name} ${i.content} ${i.area}`.toLowerCase().includes(keyword.toLowerCase())),
  )
  const usedCategories = REQ_CATEGORIES.filter((c) => rfp.items.some((i) => i.category === c.code))

  const toggleCat = (code) =>
    setCollapsed((prev) => {
      const next = new Set(prev)
      next.has(code) ? next.delete(code) : next.add(code)
      return next
    })

  return (
    <div className="pms-panel" style={{ marginTop: 18 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <h3 style={{ fontSize: 16, marginBottom: 4 }}>
            {rfp.title} <span className={`pms-tag ${STATUS_CLASS[status]}`}>{status}</span>
          </h3>
          <div className="muted">
            {rfp.id} · {project ? `${project.id} ${project.name} (${project.client})` : '프로젝트 미지정'} · {rfp.round} · 접수{' '}
            {rfp.receivedDate} · 제출 마감 {rfp.dueDate || '-'} {rfp.dueDate && `(${dday(rfp.dueDate)})`}
          </div>
        </div>
        <div style={{ display: 'flex', gap: 6, alignItems: 'flex-start', flexWrap: 'wrap' }}>
          <button className="secondary small" onClick={() => onEditRfp(rfp)}>
            RFP 정보 수정
          </button>
          <button className="secondary small" onClick={() => window.alert('목업: 항목 엑셀 양식 다운로드')}>
            엑셀 양식 ⬇
          </button>
          <button
            className="secondary small"
            onClick={() => {
              if (window.confirm('목업: 샘플 항목 6건을 엑셀에서 읽어 추가합니다.')) importSampleItems(rfp.id)
            }}
          >
            항목 엑셀 업로드 ⬆
          </button>
          <button
            className="secondary small"
            style={{ color: '#d32f2f' }}
            onClick={() => {
              if (window.confirm(`"${rfp.title}" RFP와 항목 ${rfp.items.length}건을 삭제할까요?`)) {
                deleteRfp(rfp.id)
                onDeleted()
              }
            }}
          >
            RFP 삭제
          </button>
        </div>
      </div>

      <div className="pms-summary" style={{ marginTop: 14 }}>
        <div className="pms-stat">
          <div className="label">총 항목</div>
          <div className="value">{rfp.items.length}</div>
        </div>
        <div className="pms-stat">
          <div className="label">필수 항목</div>
          <div className="value">{rfp.items.filter((i) => i.mandatory).length}</div>
        </div>
        <div className="pms-stat">
          <div className="label">평가자</div>
          <div className="value">{n}</div>
          <div className="muted">{n ? rfp.evaluators.join(', ') : '미지정'}</div>
          <button className="link-btn" style={{ marginTop: 6 }} onClick={() => setEvalForm(true)}>
            평가자 지정 ›
          </button>
        </div>
        <div className="pms-stat ok">
          <div className="label">평가 진행</div>
          <div className="value">{evalProgressOf(rfp)}%</div>
          <div className="muted">항목 × 평가자 기준</div>
        </div>
      </div>

      {rfp.items.length === 0 ? (
        <div className="pms-empty">
          <p>
            <b>등록된 항목이 없습니다.</b>
          </p>
          <p className="muted">RFP 원문을 항목 단위로 세분화해 등록하세요. 엑셀 템플릿 업로드를 권장합니다.</p>
          <button className="primary small" onClick={() => importSampleItems(rfp.id)}>
            항목 엑셀 업로드 (샘플)
          </button>{' '}
          <button className="secondary small" onClick={() => setItemForm({ category: 'SFR', area: '', name: '', content: '', page: '', mandatory: true, evalDone: 0 })}>
            + 항목 직접 추가
          </button>
        </div>
      ) : (
        <>
          <div className="pms-chips">
            <button className={!catFilter ? 'on' : ''} onClick={() => setCatFilter('')}>
              전체 <span>{rfp.items.length}</span>
            </button>
            {usedCategories.map((c) => (
              <button key={c.code} className={catFilter === c.code ? 'on' : ''} onClick={() => setCatFilter(c.code)} title={c.name}>
                {c.code} <span>{rfp.items.filter((i) => i.category === c.code).length}</span>
              </button>
            ))}
          </div>

          <div className="search-bar" style={{ boxShadow: 'none', background: '#fafbfd', padding: '10px 14px' }}>
            <div className="search-field">
              <label>검색 (코드 · 항목명 · 내용)</label>
              <input value={keyword} onChange={(e) => setKeyword(e.target.value)} />
            </div>
            <label style={{ fontSize: 13, display: 'flex', gap: 6, alignItems: 'center', paddingBottom: 8 }}>
              <input type="checkbox" checked={mandatoryOnly} onChange={(e) => setMandatoryOnly(e.target.checked)} />
              필수 항목만
            </label>
            <div style={{ marginLeft: 'auto' }}>
              <button
                className="primary small"
                onClick={() =>
                  setItemForm({ category: catFilter || 'SFR', area: '', name: '', content: '', page: '', mandatory: true, evalDone: 0 })
                }
              >
                + 항목 추가
              </button>
            </div>
          </div>

          <table className="data-table pms-wbs">
            <thead>
              <tr>
                <th>코드</th>
                <th>영역</th>
                <th>항목명</th>
                <th>요구사항 내용 (원문)</th>
                <th>페이지</th>
                <th>필수</th>
                <th>평가 현황</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {usedCategories.flatMap((c) => {
                const rows = items.filter((i) => i.category === c.code)
                if (rows.length === 0) return []
                const open = !collapsed.has(c.code)
                return [
                  <tr key={`h-${c.code}`} className="phase-row">
                    <td colSpan={8}>
                      <button className="toggle" onClick={() => toggleCat(c.code)}>
                        {open ? '▼' : '▶'}
                      </button>{' '}
                      {c.code} · {c.name} <span className="muted">({rows.length}건)</span>
                    </td>
                  </tr>,
                  ...(open
                    ? rows.map((i) => (
                        <tr key={i.id}>
                          <td>
                            <span className="pms-tag req">{i.code}</span>
                          </td>
                          <td>{i.area}</td>
                          <td className="name-cell">
                            <b>{i.name}</b>
                          </td>
                          <td className="name-cell" style={{ maxWidth: 380, color: '#555' }}>
                            {i.content}
                          </td>
                          <td>{i.page ? `p.${i.page}` : '-'}</td>
                          <td>{i.mandatory ? <span className="pms-tag late">필수</span> : <span className="pms-tag">선택</span>}</td>
                          <td>
                            {n === 0 ? (
                              <span className="muted">평가자 미지정</span>
                            ) : (
                              <span className={`pms-tag ${evalDoneOf(rfp, i) >= n ? 'done' : evalDoneOf(rfp, i) > 0 ? 'doing' : ''}`}>
                                {Math.min(evalDoneOf(rfp, i), n)}/{n} 완료
                              </span>
                            )}
                          </td>
                          <td>
                            <button className="link-btn" onClick={() => setItemForm(i)}>
                              수정
                            </button>
                          </td>
                        </tr>
                      ))
                    : []),
                ]
              })}
              {items.length === 0 && (
                <tr>
                  <td className="empty-cell" colSpan={8}>
                    조건에 맞는 항목이 없습니다.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </>
      )}

      {itemForm && <ItemForm key={itemForm.id ?? 'new'} rfp={rfp} initial={itemForm} onClose={() => setItemForm(null)} />}
      {evalForm && <EvaluatorForm rfp={rfp} onClose={() => setEvalForm(false)} />}
    </div>
  )
}

export default function RfpPage() {
  const rfps = useRfps()
  useEvals() // 평가 입력이 바뀌면 평가 진행률/상태도 다시 그린다
  const projects = useProjects()
  const [selectedId, setSelectedId] = useState(rfps[0]?.id ?? null)
  const [form, setForm] = useState(null) // { rfp, isNew }

  const selected = rfps.find((r) => r.id === selectedId)
  const projectOf = (id) => projects.find((p) => p.id === id)
  const salesProjects = projects.filter((p) => !['수행 종료', '운영 중', '실패', '보류/취소'].includes(p.status))

  const newRfp = () => ({
    id: nextRfpId(rfps),
    projectId: '',
    title: '',
    round: ROUNDS[0],
    receivedDate: TODAY,
    dueDate: '',
    fileName: '',
    fileSize: '',
    evaluators: [],
    items: [],
  })

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            RFP 관리
          </h2>
          <p className="page-description">
            발주사 RFP 문서를 접수해 항목 단위로 세분화하고, 평가할 PM(평가자)을 지정합니다. 항목별 기능/비기능 구분, 구현 여부,
            예상 M/D 평가는 「RFP 평가」 화면에서 진행합니다.
          </p>
        </div>
        <button className="primary small" onClick={() => setForm({ rfp: newRfp(), isNew: true })}>
          + RFP 등록 (문서 업로드)
        </button>
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>RFP 번호</th>
            <th>프로젝트</th>
            <th>RFP명</th>
            <th>차수</th>
            <th>접수일</th>
            <th>제출 마감</th>
            <th style={{ textAlign: 'right' }}>항목</th>
            <th>평가자</th>
            <th>평가 진행</th>
            <th>상태</th>
          </tr>
        </thead>
        <tbody>
          {rfps.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={10}>
                등록된 RFP가 없습니다.
              </td>
            </tr>
          )}
          {rfps.map((r) => {
            const st = rfpStatusOf(r)
            return (
              <tr key={r.id} className={r.id === selectedId ? 'row-selected' : ''} style={{ cursor: 'pointer' }} onClick={() => setSelectedId(r.id)}>
                <td>{r.id}</td>
                <td className="name-cell">{projectOf(r.projectId)?.name ?? '-'}</td>
                <td className="name-cell">
                  <b>{r.title}</b>
                </td>
                <td>{r.round}</td>
                <td>{r.receivedDate}</td>
                <td>{r.dueDate ? `${r.dueDate.slice(5)} (${dday(r.dueDate)})` : '-'}</td>
                <td style={{ textAlign: 'right' }}>{r.items.length}</td>
                <td>{r.evaluators.length ? `${r.evaluators.length}명` : '-'}</td>
                <td>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <div className="pms-bar" style={{ width: 60 }}>
                      <span style={{ width: `${evalProgressOf(r)}%` }} />
                    </div>
                    {evalProgressOf(r)}%
                  </div>
                </td>
                <td>
                  <span className={`pms-tag ${STATUS_CLASS[st]}`}>{st}</span>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>

      {selected ? (
        <RfpDetail
          key={selected.id}
          rfp={selected}
          project={projectOf(selected.projectId)}
          onDeleted={() => setSelectedId(null)}
          onEditRfp={(rfp) => setForm({ rfp, isNew: false })}
        />
      ) : (
        <p className="muted" style={{ marginTop: 18 }}>
          위 목록에서 RFP를 선택하면 항목 목록이 표시됩니다.
        </p>
      )}

      {form && (
        <RfpForm
          key={form.rfp.id}
          initial={form.rfp}
          isNew={form.isNew}
          projects={salesProjects}
          onClose={() => setForm(null)}
          onSaved={(id) => setSelectedId(id)}
        />
      )}
    </div>
  )
}
