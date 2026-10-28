import { useState } from 'react'
import './pms.css'
import { confirmAllWith, confirmEvaluator, confirmedOf, getEval, isComplete, isSubmitted, mdOf, reopenEvals, setConfirmedMd, useEvals } from './evalStore'
import { REQ_CATEGORIES, rfpStatusOf, useRfps } from './rfpStore'

const fmt = (n) => (Number.isInteger(n) ? `${n}.0` : `${n}`)
const STATUS_CLASS = { 접수: '', '항목 정리 중': 'manual', '평가 중': 'doing', '평가 완료': 'done' }

/** 평가자 한 명의 한 항목 평가를 칸 하나에 요약해서 보여준다. 제출 전이면 내용을 숨긴다(블라인드 유지). */
function EvalCell({ ev, submitted }) {
  if (!submitted) return <span className="muted">미제출</span>
  if (!ev || !ev.kind) return <span className="muted">-</span>
  if (ev.kind === '비기능')
    return (
      <>
        <span className="pms-tag">비기능</span>{' '}
        {ev.difficulty && <span className={`pms-tag ${ev.difficulty === '상' ? 'late' : ''}`}>난이도 {ev.difficulty}</span>}
      </>
    )
  const implClass = ev.impl === '구현' ? 'done' : ev.impl === '미구현' ? 'late' : 'manual'
  return (
    <div title={ev.comment || ''}>
      <span className="pms-tag doing">기능</span> <span className={`pms-tag ${implClass}`}>{ev.impl ?? '-'}</span>{' '}
      {ev.difficulty && <span className={`pms-tag ${ev.difficulty === '상' ? 'late' : ''}`}>난이도 {ev.difficulty}</span>}
      <div style={{ marginTop: 3, fontWeight: 700 }}>{ev.impl === '미구현' ? '0.0' : ev.md ? fmt(ev.md) : '-'} M/D</div>
    </div>
  )
}

/** 항목 하나의 확정 M/D — 확정 평가자 선택 + 값 직접 수정. */
function ConfirmCell({ rfp, item, names }) {
  const c = confirmedOf(rfp, item)
  return (
    <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
      <select
        className="pms-cell-input"
        style={{ minWidth: 0, width: 96 }}
        value={c?.evaluator ?? ''}
        disabled={names.length === 0}
        onChange={(e) => confirmEvaluator(rfp.id, item.id, e.target.value)}
        title={names.length === 0 ? '제출된 평가가 없습니다' : '이 평가자의 M/D를 확정 값으로 사용'}
      >
        <option value="">{c && !c.evaluator ? '(직접 입력)' : '확정 평가자'}</option>
        {names.map((n) => (
          <option key={n}>{n}</option>
        ))}
      </select>
      <input
        type="number"
        min="0"
        step="0.5"
        className="pms-cell-input"
        style={{ width: 64, minWidth: 0 }}
        value={c ? c.md : ''}
        placeholder="M/D"
        onChange={(e) => setConfirmedMd(rfp, item, e.target.value)}
      />
      {c?.edited && <span className="pms-tag manual" title="확정 평가자의 값에서 수정됨">수정됨</span>}
    </div>
  )
}
/**
 * RFP 평가 관리 (평가 관리자 화면) — RFP 관리처럼 항목을 열거하고, 평가자별 평가 현황을 나란히 보여준다.
 * M/D는 자동으로 합치지 않는다(평균/최댓값 없음) — 평가자별 값과 분류별 소계, 총합만 보여준다.
 * 블라인드 유지를 위해 제출된 평가만 표시한다.
 */
export default function RfpEvalAdminPage() {
  const rfps = useRfps()
  useEvals()
  const targets = rfps.filter((r) => r.evaluators.length > 0 && r.items.length > 0)
  const [rfpId, setRfpId] = useState(targets[0]?.id ?? '')
  const [collapsed, setCollapsed] = useState(new Set())
  const rfp = targets.find((r) => r.id === rfpId) ?? targets[0]

  if (!rfp) {
    return (
      <div>
        <div className="page-header">
          <h2>RFP 평가 관리</h2>
        </div>
        <p className="muted">평가자가 지정된 RFP가 없습니다.</p>
      </div>
    )
  }

  const evaluators = rfp.evaluators
  const usedCategories = REQ_CATEGORIES.filter((c) => rfp.items.some((i) => i.category === c.code))
  const evalOf = (item, name) => getEval(rfp.id, item.id, name)
  const visibleMd = (item, name) => (isSubmitted(rfp.id, name) ? mdOf(evalOf(item, name)) : 0)
  const sumFor = (items, name) => items.reduce((s, i) => s + visibleMd(i, name), 0)
  const countKind = (name, kind) =>
    isSubmitted(rfp.id, name) ? rfp.items.filter((i) => evalOf(i, name)?.kind === kind).length : 0

  const toggle = (code) =>
    setCollapsed((prev) => {
      const next = new Set(prev)
      next.has(code) ? next.delete(code) : next.add(code)
      return next
    })

  const kindsDiffer = (item) => {
    const kinds = evaluators.filter((n) => isSubmitted(rfp.id, n)).map((n) => evalOf(item, n)?.kind).filter(Boolean)
    return new Set(kinds).size > 1
  }
  const status = rfpStatusOf(rfp)
  const submittedNames = evaluators.filter((n) => isSubmitted(rfp.id, n))
  const confMd = (item) => confirmedOf(rfp, item)?.md ?? 0
  const sumConfirmed = (items) => items.reduce((s, i) => s + confMd(i), 0)
  const confirmedCount = rfp.items.filter((i) => confirmedOf(rfp, i)).length

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            RFP 평가 관리
          </h2>
          <p className="page-description">
            평가자별 평가 현황을 항목 단위로 비교합니다. M/D는 자동으로 합산·평균하지 않고 평가자별 값과 소계·총합만 보여줍니다.
          </p>
        </div>
      </div>

      <div className="pms-blind">
        🔒 평가자 간 <b>블라인드</b> — 평가자는 서로의 평가를 볼 수 없고, 이 화면에는 <b>제출 완료된 평가만</b> 표시됩니다(작성 중인 초안은
        보이지 않음).
      </div>

      <div className="pms-toolbar">
        <select value={rfp.id} onChange={(e) => setRfpId(e.target.value)}>
          {targets.map((r) => (
            <option key={r.id} value={r.id}>
              {r.id} · {r.title}
            </option>
          ))}
        </select>
        <span className={`pms-tag ${STATUS_CLASS[status]}`}>{status}</span>
      </div>

      <div className="pms-summary">
        {evaluators.map((n) => {
          const sub = isSubmitted(rfp.id, n)
          const done = rfp.items.filter((i) => isComplete(evalOf(i, n))).length
          return (
            <div key={n} className={`pms-stat ${sub ? 'ok' : ''}`}>
              <div className="label">평가자 · {n}</div>
              <div className="value" style={{ fontSize: 18 }}>
                {sub ? '제출 완료' : '작성 중'}
              </div>
              <div className="muted">
                입력 {done}/{rfp.items.length}
                {sub && ` · M/D ${fmt(sumFor(rfp.items, n))}`}
              </div>
              {sub && (
                <button className="link-btn" style={{ marginTop: 6 }} onClick={() => reopenEvals(rfp.id, n)}>
                  재평가 요청 ›
                </button>
              )}
            </div>
          )
        })}
      </div>

      <div className="pms-toolbar">
        <b>확정 M/D</b>
        <span className="muted">원가 산정에 쓰는 값입니다. 항목마다 확정 평가자를 고르면 그 평가자의 M/D가 들어가고, 숫자는 직접 수정할 수 있습니다.</span>
        {submittedNames.length > 1 && (
          <select
            value=""
            onChange={(e) => e.target.value && window.confirm(`모든 항목의 확정 평가자를 ${e.target.value}(으)로 일괄 지정할까요? 기존 확정·수정 값은 덮어씁니다.`) && confirmAllWith(rfp, e.target.value)}
          >
            <option value="">전체 항목 일괄 지정…</option>
            {submittedNames.map((n) => (
              <option key={n} value={n}>{n} 값으로</option>
            ))}
          </select>
        )}
      </div>
      <table className="data-table pms-wbs pms-eval-matrix">
        <thead>
          <tr>
            <th>코드</th>
            <th>항목명</th>
            {evaluators.map((n) => (
              <th key={n}>
                {n} {isSubmitted(rfp.id, n) ? '' : <span className="muted">(미제출)</span>}
              </th>
            ))}
            <th></th>
            <th style={{ minWidth: 190 }}>확정 M/D (원가 산정용)</th>
          </tr>
        </thead>
        <tbody>
          {usedCategories.flatMap((c) => {
            const rows = rfp.items.filter((i) => i.category === c.code)
            const open = !collapsed.has(c.code)
            return [
              <tr key={`h-${c.code}`} className="phase-row">
                <td colSpan={4 + evaluators.length}>
                  <button className="toggle" onClick={() => toggle(c.code)}>
                    {open ? '▼' : '▶'}
                  </button>{' '}
                  {c.code} · {c.name} <span className="muted">({rows.length}건)</span>
                </td>
              </tr>,
              ...(open
                ? rows.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <span className="pms-tag req">{item.code}</span>
                      </td>
                      <td className="name-cell">
                        <b>{item.name}</b>
                        <div className="muted">{item.area}</div>
                      </td>
                      {evaluators.map((n) => (
                        <td key={n} className="matrix-cell">
                          <EvalCell ev={evalOf(item, n)} submitted={isSubmitted(rfp.id, n)} />
                        </td>
                      ))}
                      <td>{kindsDiffer(item) && <span className="pms-tag late">구분 상이</span>}</td>
                      <td>
                        <ConfirmCell rfp={rfp} item={item} names={submittedNames} />
                      </td>
                    </tr>
                  ))
                : []),
              <tr key={`s-${c.code}`} className="subtotal-row">
                <td colSpan={2}>{c.code} 소계 (M/D)</td>
                {evaluators.map((n) => (
                  <td key={n}>{isSubmitted(rfp.id, n) ? fmt(sumFor(rows, n)) : '-'}</td>
                ))}
                <td />
                <td><b>{fmt(sumConfirmed(rows))}</b></td>
              </tr>,
            ]
          })}
          <tr className="total-row">
            <td colSpan={2}>M/D 총합 (기능 항목 기준)</td>
            {evaluators.map((n) => (
              <td key={n}>{isSubmitted(rfp.id, n) ? fmt(sumFor(rfp.items, n)) : '-'}</td>
            ))}
            <td />
            <td><b>{fmt(sumConfirmed(rfp.items))}</b></td>
          </tr>
          <tr className="subtotal-row">
            <td colSpan={2}>기능 / 비기능 항목 수</td>
            {evaluators.map((n) => (
              <td key={n}>{isSubmitted(rfp.id, n) ? `${countKind(n, '기능')} / ${countKind(n, '비기능')}` : '-'}</td>
            ))}
            <td />
            <td className="muted">확정 {confirmedCount}/{rfp.items.length}건</td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}
