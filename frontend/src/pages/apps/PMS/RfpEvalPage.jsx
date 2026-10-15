import { useState } from 'react'
import './pms.css'
import { ME } from './pmsStore'
import { DIFFICULTIES, IMPLS, KINDS, getEval, isComplete, isSubmitted, mdOf, saveEval, submitEvals, useEvals } from './evalStore'
import { REQ_CATEGORIES, useRfps } from './rfpStore'

const fmt = (n) => (Number.isInteger(n) ? `${n}.0` : `${n}`)

/**
 * RFP 평가 (평가자 화면) — 지정된 평가자(PM)가 항목마다 기능/비기능 구분, 구현 여부, 예상 M/D를 입력한다.
 * 블라인드: 이 화면에는 본인 평가만 나온다. 다른 평가자의 평가는 「RFP 평가 관리」에서만 확인한다.
 */
export default function RfpEvalPage() {
  const rfps = useRfps()
  useEvals()
  const targets = rfps.filter((r) => r.evaluators.length > 0 && r.items.length > 0)
  const [rfpId, setRfpId] = useState(targets[0]?.id ?? '')
  const rfp = targets.find((r) => r.id === rfpId) ?? targets[0]
  // 목업: 실제로는 로그인한 사용자가 평가자다. 화면 시연을 위해 평가자 전환을 둔다.
  const [asUser, setAsUser] = useState(null)
  const evaluator = rfp ? (rfp.evaluators.includes(asUser) ? asUser : rfp.evaluators.includes(ME) ? ME : rfp.evaluators[0]) : null

  if (!rfp) {
    return (
      <div>
        <div className="page-header">
          <h2>RFP 평가</h2>
        </div>
        <p className="muted">평가자가 지정된 RFP가 없습니다. RFP 관리에서 항목을 등록하고 평가자를 지정하세요.</p>
      </div>
    )
  }

  const submitted = isSubmitted(rfp.id, evaluator)
  const evs = rfp.items.map((i) => ({ item: i, ev: getEval(rfp.id, i.id, evaluator) }))
  const doneCount = evs.filter((x) => isComplete(x.ev)).length
  const funcCount = evs.filter((x) => x.ev?.kind === '기능').length
  const nonFuncCount = evs.filter((x) => x.ev?.kind === '비기능').length
  const totalMd = evs.reduce((s, x) => s + mdOf(x.ev), 0)
  const usedCategories = REQ_CATEGORIES.filter((c) => rfp.items.some((i) => i.category === c.code))

  const onSubmit = () => {
    const res = submitEvals(rfp, evaluator)
    if (!res.ok) window.alert(`아직 입력이 끝나지 않은 항목이 ${res.missing}건 있습니다.`)
    else window.alert('평가를 제출했습니다. 제출 후에는 수정할 수 없습니다(수정이 필요하면 평가관리자에게 재평가를 요청하세요).')
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            RFP 평가 <span className="pms-mock-badge">목업 · 더미 데이터</span>
          </h2>
          <p className="page-description">
            RFP 항목마다 <b>기능/비기능</b>을 구분하고, 기능 항목은 <b>구현 여부</b>와 구현 시 <b>예상 M/D</b>를 입력합니다.
          </p>
        </div>
      </div>

      <div className="pms-blind">
        🔒 <b>블라인드 평가</b> — 이 화면에는 <b>내 평가만</b> 표시됩니다. 다른 평가자의 평가는 볼 수 없으며, 제출된 평가는 평가 관리자가
        「RFP 평가 관리」에서 평가자별로 확인합니다.
      </div>

      <div className="pms-toolbar">
        <select value={rfp.id} onChange={(e) => setRfpId(e.target.value)}>
          {targets.map((r) => (
            <option key={r.id} value={r.id}>
              {r.id} · {r.title}
            </option>
          ))}
        </select>
        <span className="muted">평가자로 보기(목업)</span>
        <select value={evaluator} onChange={(e) => setAsUser(e.target.value)}>
          {rfp.evaluators.map((n) => (
            <option key={n}>{n}</option>
          ))}
        </select>
        <span className={`pms-tag ${submitted ? 'done' : 'doing'}`}>{submitted ? '제출 완료' : '작성 중'}</span>
        <button className="primary small" style={{ marginLeft: 'auto' }} disabled={submitted} onClick={onSubmit}>
          평가 제출
        </button>
      </div>

      <div className="pms-summary">
        <div className="pms-stat">
          <div className="label">내 입력 진행</div>
          <div className="value">
            {doneCount}/{rfp.items.length}
          </div>
        </div>
        <div className="pms-stat">
          <div className="label">기능 / 비기능</div>
          <div className="value">
            {funcCount} / {nonFuncCount}
          </div>
        </div>
        <div className="pms-stat ok">
          <div className="label">내 예상 M/D 총합</div>
          <div className="value">{fmt(totalMd)}</div>
        </div>
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>코드</th>
            <th>항목명</th>
            <th>요구사항 (원문)</th>
            <th>구분</th>
            <th>구현 여부</th>
            <th>난이도</th>
            <th style={{ textAlign: 'right' }}>예상 M/D</th>
            <th>의견</th>
            <th>입력</th>
          </tr>
        </thead>
        <tbody>
          {usedCategories.flatMap((c) => {
            const rows = evs.filter((x) => x.item.category === c.code)
            const sub = rows.reduce((s, x) => s + mdOf(x.ev), 0)
            return [
              <tr key={`h-${c.code}`} className="phase-row">
                <td colSpan={9}>
                  {c.code} · {c.name} <span className="muted">({rows.length}건)</span>
                </td>
              </tr>,
              ...rows.map(({ item, ev }) => {
                const isFunc = ev?.kind === '기능'
                const mdDisabled = submitted || !isFunc || ev?.impl === '미구현'
                return (
                  <tr key={item.id}>
                    <td>
                      <span className="pms-tag req">{item.code}</span>
                    </td>
                    <td className="name-cell">
                      <b>{item.name}</b>
                      <div className="muted">{item.area}</div>
                    </td>
                    <td className="name-cell" style={{ maxWidth: 320, color: '#555' }}>
                      {item.content}
                    </td>
                    <td>
                      <select
                        className="pms-cell-input"
                        disabled={submitted}
                        value={ev?.kind ?? ''}
                        onChange={(e) => saveEval(rfp.id, item.id, evaluator, { kind: e.target.value })}
                      >
                        <option value="">선택</option>
                        {KINDS.map((k) => (
                          <option key={k}>{k}</option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <select
                        className="pms-cell-input"
                        disabled={submitted || !isFunc}
                        value={ev?.impl ?? ''}
                        onChange={(e) => saveEval(rfp.id, item.id, evaluator, { impl: e.target.value })}
                      >
                        <option value="">{isFunc ? '선택' : '-'}</option>
                        {IMPLS.map((k) => (
                          <option key={k}>{k}</option>
                        ))}
                      </select>
                    </td>
                    <td>
                      <select
                        className="pms-cell-input"
                        style={{ minWidth: 70 }}
                        disabled={submitted || !ev?.kind}
                        value={ev?.difficulty ?? ''}
                        onChange={(e) => saveEval(rfp.id, item.id, evaluator, { difficulty: e.target.value || null })}
                      >
                        <option value="">{ev?.kind === '비기능' ? '(선택)' : '선택'}</option>
                        {DIFFICULTIES.map((d) => (
                          <option key={d}>{d}</option>
                        ))}
                      </select>
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <input
                        type="number"
                        min="0"
                        step="0.5"
                        className="pms-cell-input pms-md"
                        disabled={mdDisabled}
                        value={ev?.md ?? ''}
                        onChange={(e) =>
                          saveEval(rfp.id, item.id, evaluator, { md: e.target.value === '' ? null : Number(e.target.value) })
                        }
                      />
                    </td>
                    <td>
                      <input
                        className="pms-cell-input"
                        disabled={submitted}
                        value={ev?.comment ?? ''}
                        placeholder="의견(선택)"
                        onChange={(e) => saveEval(rfp.id, item.id, evaluator, { comment: e.target.value })}
                      />
                    </td>
                    <td>
                      <span className={`pms-tag ${isComplete(ev) ? 'done' : ''}`}>{isComplete(ev) ? '완료' : '미입력'}</span>
                    </td>
                  </tr>
                )
              }),
              <tr key={`s-${c.code}`} className="subtotal-row">
                <td colSpan={6}>{c.code} 소계</td>
                <td style={{ textAlign: 'right' }}>{fmt(sub)}</td>
                <td colSpan={2} />
              </tr>,
            ]
          })}
          <tr className="total-row">
            <td colSpan={6}>M/D 총합 (기능 항목 기준)</td>
            <td style={{ textAlign: 'right' }}>{fmt(totalMd)}</td>
            <td colSpan={2}>
              기능 {funcCount}건 · 비기능 {nonFuncCount}건
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  )
}
