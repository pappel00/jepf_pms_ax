/**
 * RFP 평가 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 평가자(PM)가 RFP 항목마다 입력한다: 기능/비기능 구분, (기능이면) 구현 여부, 구현 시 예상 M/D, 의견.
 *  - 블라인드: 평가자는 자기 평가만 볼 수 있다(RFP 평가 화면). 다른 평가자의 값은 평가관리 화면에서
 *    **제출된 평가**만 평가자별로 나란히 보여준다 — 제출 전 초안은 보이지 않는다.
 *  - M/D 취합 규칙은 두지 않는다(평균/최댓값 등 자동 계산 없음). 평가자별 값과 소계·총합만 보여준다.
 */
import { useSyncExternalStore } from 'react'

export const KINDS = ['기능', '비기능']
export const IMPLS = ['구현', '일부 구현', '미구현', '협의 필요']
/** 항목 난이도 — 기능 항목은 필수, 비기능은 선택. M/D에 자동 반영하지 않고 참고 정보로만 쓴다. */
export const DIFFICULTIES = ['하', '중', '상']
const difficultyFromMd = (md) => (md >= 10 ? '상' : md >= 5 ? '중' : '하')

const key = (rfpId, itemId, evaluator) => `${rfpId}|${itemId}|${evaluator}`
const subKey = (rfpId, evaluator) => `${rfpId}|${evaluator}`

const roundHalf = (n) => Math.round(n * 2) / 2
const itemId = (rfpId, idx) => `${rfpId}-I${String(idx).padStart(3, '0')}`

// rfpStore의 시드 항목과 같은 순서: [분류, 기준 M/D(기능 항목만)]
const ECM = [['SFR', 8], ['SFR', 5], ['SFR', 10], ['SFR', 12], ['SFR', 6], ['SFR', 4], ['PER'], ['PER'], ['SER'], ['SER'], ['DAR', 25], ['COR'], ['PMR'], ['PSR']]
const MOB = [['SFR', 10], ['SFR', 6], ['SFR', 8], ['SFR', 5], ['PER'], ['SER'], ['QUR'], ['COR'], ['PMR']]
const FACTOR = { 이설계: 1, 관리자: 1.2, 김개발: 0.9 }

function buildSeed() {
  const evals = {}
  const subs = {}

  const put = (rfpId, table, evaluator, upTo, submitted) => {
    for (let idx = 1; idx <= upTo; idx++) {
      const [, base] = table[idx - 1]
      let ev
      if (base === undefined) {
        ev = { kind: '비기능', impl: null, md: null, comment: '' }
      } else {
        ev = { kind: '기능', impl: '구현', md: roundHalf(base * (FACTOR[evaluator] ?? 1)), comment: '' }
      }
      // 평가자 간 의견이 갈리는 지점을 일부러 만든다(블라인드 평가 시연용).
      if (rfpId === 'RFP-2026-001' && idx === 6 && evaluator === '이설계') ev = { kind: '기능', impl: '일부 구현', md: 4, comment: '기존 DRM 모듈 재사용 가능' }
      if (rfpId === 'RFP-2026-001' && idx === 6 && evaluator === '관리자') ev = { kind: '기능', impl: '협의 필요', md: 3, comment: '발주사 DRM 제품 확인 후 확정' }
      if (rfpId === 'RFP-2026-001' && idx === 10 && evaluator === '관리자') ev = { kind: '기능', impl: '구현', md: 3, comment: '암호 모듈 연동 개발 필요' }
      if (rfpId === 'RFP-2026-002' && idx === 3 && evaluator === '김개발') ev = { kind: '기능', impl: '일부 구현', md: 4.5, comment: '생체 인증은 단말 의존' }
      ev = { difficulty: ev.kind === '기능' ? difficultyFromMd(ev.md) : null, ...ev }
      evals[key(rfpId, itemId(rfpId, idx), evaluator)] = ev
    }
    subs[subKey(rfpId, evaluator)] = submitted
  }

  put('RFP-2026-001', ECM, '이설계', 14, true)
  put('RFP-2026-001', ECM, '관리자', 8, false)
  put('RFP-2026-002', MOB, '이설계', 9, true)
  put('RFP-2026-002', MOB, '김개발', 9, true)
  put('RFP-2026-002', MOB, '관리자', 4, false)
  return { evals, subs, confirmed: {} }
}

let state = buildSeed()
const listeners = new Set()

function emit(next) {
  state = next
  listeners.forEach((l) => l())
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function useEvals() {
  return useSyncExternalStore(subscribe, () => state)
}

export const getEval = (rfpId, itemIdValue, evaluator) => state.evals[key(rfpId, itemIdValue, evaluator)] ?? null
export const isSubmitted = (rfpId, evaluator) => !!state.subs[subKey(rfpId, evaluator)]

/** 평가 입력이 끝났는지: 구분 필수, 기능이면 구현 여부·난이도 필수, 구현/일부 구현이면 M/D(>0) 필수. */
export function isComplete(ev) {
  if (!ev || !ev.kind) return false
  if (ev.kind === '비기능') return true
  if (!ev.impl || !ev.difficulty) return false
  if (ev.impl === '구현' || ev.impl === '일부 구현') return typeof ev.md === 'number' && ev.md > 0
  return true // 미구현(M/D 0) / 협의 필요(M/D 선택)
}

/** 집계에 쓰는 M/D — 기능 항목만 센다(비기능·미구현은 0). */
export const mdOf = (ev) => (ev && ev.kind === '기능' && ev.impl !== '미구현' ? ev.md || 0 : 0)

/** 항목을 평가 완료한 평가자 수 (RFP 관리의 "n/m 완료" 표시에 사용). */
export function evalDoneOf(rfp, item) {
  return rfp.evaluators.filter((n) => isComplete(getEval(rfp.id, item.id, n))).length
}

/**
 * 항목에 대한 **제출된** 평가를 요약한다(단계전환 화면에서 RFP 항목을 고를 때 참고용).
 * 구분은 평가자 다수결(동수면 기능), M/D는 기능으로 평가한 값의 평균(0.5 단위), 난이도는 가장 높은 값.
 */
export function itemEvalSummary(rfp, item) {
  const submitted = rfp.evaluators.filter((n) => isSubmitted(rfp.id, n))
  const evs = submitted.map((n) => getEval(rfp.id, item.id, n)).filter((e) => e && e.kind)
  if (evs.length === 0) return { count: 0, total: rfp.evaluators.length, kind: null, md: null, difficulty: null }
  const func = evs.filter((e) => e.kind === '기능').length
  const mds = evs.filter((e) => e.kind === '기능' && e.impl !== '미구현' && e.md > 0).map((e) => e.md)
  const rank = { 하: 1, 중: 2, 상: 3 }
  return {
    count: evs.length,
    total: rfp.evaluators.length,
    kind: func * 2 >= evs.length ? '기능' : '비기능',
    md: mds.length ? roundHalf(mds.reduce((s, x) => s + x, 0) / mds.length) : null,
    difficulty: evs.map((e) => e.difficulty).filter(Boolean).sort((a, b) => rank[b] - rank[a])[0] ?? null,
  }
}

export function saveEval(rfpId, itemIdValue, evaluator, patch) {
  if (isSubmitted(rfpId, evaluator)) return
  const k = key(rfpId, itemIdValue, evaluator)
  const prev = state.evals[k] ?? { kind: '', impl: null, difficulty: null, md: null, comment: '' }
  let next = { ...prev, ...patch }
  if (next.kind === '비기능') next = { ...next, impl: null, md: null }
  if (next.kind === '기능' && next.impl === '미구현') next = { ...next, md: 0 }
  emit({ ...state, evals: { ...state.evals, [k]: next } })
}

/** 평가 제출 — 모든 항목이 완료여야 한다. 못 하면 미완료 항목 수를 돌려준다. */
export function submitEvals(rfp, evaluator) {
  const missing = rfp.items.filter((i) => !isComplete(getEval(rfp.id, i.id, evaluator))).length
  if (missing > 0) return { ok: false, missing }
  emit({ ...state, subs: { ...state.subs, [subKey(rfp.id, evaluator)]: true } })
  return { ok: true }
}

// ---------- 확정 M/D (원가 산정용) ----------
// 평가자별 값은 자동 취합하지 않는다. 평가 관리자가 항목마다 「확정 평가자」를 고르면 그 평가자의 M/D가 확정 값에 들어가고,
// 이후 숫자를 직접 고칠 수 있다. 평가자가 1명이면 그 평가자의 제출 값이 기본 확정 값이다(수정 가능).
const confKey = (rfpId, itemIdValue) => `${rfpId}|${itemIdValue}`

/** 항목의 확정 값 { evaluator, md, edited } 또는 null. 평가자 1명이면 제출된 값으로 자동 제안. */
export function confirmedOf(rfp, item) {
  const saved = state.confirmed[confKey(rfp.id, item.id)]
  if (saved) return saved
  if (rfp.evaluators.length === 1 && isSubmitted(rfp.id, rfp.evaluators[0])) {
    const n = rfp.evaluators[0]
    return { evaluator: n, md: mdOf(getEval(rfp.id, item.id, n)), edited: false, auto: true }
  }
  return null
}

/** 확정 평가자를 고른다 — 그 평가자(제출 완료)의 M/D가 확정 값이 된다. evaluator가 빈 값이면 확정 해제. */
export function confirmEvaluator(rfpId, itemIdValue, evaluator) {
  const k = confKey(rfpId, itemIdValue)
  const next = { ...state.confirmed }
  if (!evaluator) delete next[k]
  else next[k] = { evaluator, md: mdOf(getEval(rfpId, itemIdValue, evaluator)), edited: false }
  emit({ ...state, confirmed: next })
}

/** 확정 M/D를 직접 수정 — 확정 평가자는 유지하고 「수정됨」으로 표시한다. */
export function setConfirmedMd(rfp, item, md) {
  const k = confKey(rfp.id, item.id)
  const cur = confirmedOf(rfp, item)
  const value = Math.max(0, Number(md) || 0)
  emit({ ...state, confirmed: { ...state.confirmed, [k]: { evaluator: cur?.evaluator ?? '', md: value, edited: true } } })
}

/** 모든 항목의 확정 평가자를 한 명으로 일괄 지정(입력 보조). */
export function confirmAllWith(rfp, evaluator) {
  const next = { ...state.confirmed }
  rfp.items.forEach((i) => {
    next[confKey(rfp.id, i.id)] = { evaluator, md: mdOf(getEval(rfp.id, i.id, evaluator)), edited: false }
  })
  emit({ ...state, confirmed: next })
}

/** 재평가 요청 — 제출을 풀어 평가자가 다시 수정할 수 있게 한다(평가관리 화면). */
export function reopenEvals(rfpId, evaluator) {
  emit({ ...state, subs: { ...state.subs, [subKey(rfpId, evaluator)]: false } })
}
