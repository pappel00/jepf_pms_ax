/**
 * RFP 평가 스토어 — 서버(`/api/v1/pms/rfp-evals`, RFP 응답의 평가자·확정 값)에서 읽고 저장한다.
 *
 * 평가자(PM)가 RFP 항목마다 입력한다: 기능/비기능 구분, (기능이면) 구현 여부, 구현 시 예상 M/D, 의견.
 *  - 블라인드: 서버가 본인 평가와 **제출된** 평가만 내려준다. 다른 평가자의 초안은 화면에 오지 않는다.
 *  - M/D 취합 규칙은 두지 않는다(평균/최댓값 등 자동 계산 없음). 평가자별 값과 소계·총합만 보여준다.
 *  - 평가 입력·제출은 로그인한 사용자 본인 것만 저장된다(평가자로 지정된 사용자).
 * 화면의 RFP id 는 RFP 코드, 항목 id 는 서버 PK 이고 평가자는 이름으로 다룬다(서버는 사용자 ID).
 */
import { useSyncExternalStore } from 'react'
import { pmsRfpsApi } from '../../../api/entities'
import { userIdByName } from './pmsUsers'
import { getRfps, loadRfps } from './rfpStore'

export const KINDS = ['기능', '비기능']
export const IMPLS = ['구현', '일부 구현', '미구현', '협의 필요']
/** 항목 난이도 — 기능 항목은 필수, 비기능은 선택. M/D에 자동 반영하지 않고 참고 정보로만 쓴다. */
export const DIFFICULTIES = ['하', '중', '상']

const roundHalf = (n) => Math.round(n * 2) / 2
const key = (rfpId, itemId, evaluator) => `${rfpId}|${itemId}|${evaluator}`
const subKey = (rfpId, evaluator) => `${rfpId}|${evaluator}`
const confKey = (rfpId, itemId) => `${rfpId}|${itemId}`

let state = { evals: {}, subs: {}, confirmed: {} }
const listeners = new Set()
function emit(next) {
  state = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}

/** rfpStore 가 서버에서 읽은 값으로 통째로 교체한다. */
export function hydrateEvals(next) {
  emit(next)
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

/** 항목을 평가 완료한 평가자 수 (RFP 관리의 "n/m 완료" 표시) — 다른 평가자의 초안은 안 보이므로 서버가 센 값을 쓴다. */
export const evalDoneOf = (rfp, item) => item.evalDone ?? 0

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

// ---------- 평가 입력 (입력 중에는 화면에 바로 반영하고, 잠깐 멈추면 서버에 저장) ----------
const pending = new Map() // 항목 id → { timer, body }
const toBody = (ev) => ({
  kindCode: ev.kind || null,
  implCode: ev.impl || null,
  difficultyCode: ev.difficulty || null,
  md: typeof ev.md === 'number' ? ev.md : null,
  comment: ev.comment || null,
})

async function sendEval(itemIdValue) {
  const p = pending.get(itemIdValue)
  if (!p) return
  clearTimeout(p.timer)
  pending.delete(itemIdValue)
  try {
    await pmsRfpsApi.saveMyEval(itemIdValue, p.body)
  } catch (e) {
    window.alert(`평가를 저장하지 못했습니다: ${e.message}`)
    await loadRfps()
  }
}

const flushEvals = () => Promise.all([...pending.keys()].map(sendEval))

export function saveEval(rfpId, itemIdValue, evaluator, patch) {
  if (isSubmitted(rfpId, evaluator)) return
  const k = key(rfpId, itemIdValue, evaluator)
  const prev = state.evals[k] ?? { kind: '', impl: null, difficulty: null, md: null, comment: '' }
  let next = { ...prev, ...patch }
  if (next.kind === '비기능') next = { ...next, impl: null, md: null }
  if (next.kind === '기능' && next.impl === '미구현') next = { ...next, md: 0 }
  emit({ ...state, evals: { ...state.evals, [k]: next } })
  const old = pending.get(itemIdValue)
  if (old) clearTimeout(old.timer)
  pending.set(itemIdValue, { body: toBody(next), timer: setTimeout(() => sendEval(itemIdValue), 500) })
}

/** 평가 제출 — 모든 항목이 완료여야 한다. 못 하면 { ok:false, missing } 또는 { ok:false, error }. */
export async function submitEvals(rfp, evaluator) {
  const missing = rfp.items.filter((i) => !isComplete(getEval(rfp.id, i.id, evaluator))).length
  if (missing > 0) return { ok: false, missing }
  await flushEvals()
  try {
    await pmsRfpsApi.submit(rfp.dbId)
  } catch (e) {
    return { ok: false, error: e.message }
  }
  await loadRfps()
  return { ok: true }
}

// ---------- 확정 M/D (원가 산정용) ----------
// 평가자별 값은 자동 취합하지 않는다. 평가 관리자가 항목마다 「확정 평가자」를 고르면 그 평가자의 M/D가 확정 값에 들어가고,
// 이후 숫자를 직접 고칠 수 있다. 평가자가 1명이면 그 평가자의 제출 값이 기본 확정 값이다(수정 가능).

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

const rfpOf = (rfpId) => getRfps().find((r) => r.id === rfpId)
const fail = async (e) => {
  window.alert(`처리하지 못했습니다: ${e.message}`)
  await loadRfps()
}

/** 확정 평가자를 고른다 — 그 평가자(제출 완료)의 M/D가 확정 값이 된다. evaluator가 빈 값이면 확정 해제. */
export async function confirmEvaluator(rfpId, itemIdValue, evaluator) {
  try {
    await pmsRfpsApi.confirm(itemIdValue, { evaluatorUserId: evaluator ? userIdByName(evaluator) : null, md: null })
    await loadRfps()
  } catch (e) {
    await fail(e)
  }
}

/** 확정 M/D를 직접 수정 — 확정 평가자는 유지하고 「수정됨」으로 표시한다. */
const mdTimers = new Map()
export function setConfirmedMd(rfp, item, md) {
  const k = confKey(rfp.id, item.id)
  const cur = confirmedOf(rfp, item)
  const value = Math.max(0, Number(md) || 0)
  emit({ ...state, confirmed: { ...state.confirmed, [k]: { evaluator: cur?.evaluator ?? '', md: value, edited: true } } })
  clearTimeout(mdTimers.get(k))
  mdTimers.set(
    k,
    setTimeout(() => {
      pmsRfpsApi.confirm(item.id, { evaluatorUserId: cur?.evaluator ? userIdByName(cur.evaluator) : null, md: value }).catch(fail)
    }, 500),
  )
}

/** 모든 항목의 확정 평가자를 한 명으로 일괄 지정(입력 보조). */
export async function confirmAllWith(rfp, evaluator) {
  try {
    await pmsRfpsApi.confirmAll(rfp.dbId, userIdByName(evaluator))
    await loadRfps()
  } catch (e) {
    await fail(e)
  }
}

/** 재평가 요청 — 제출을 풀어 평가자가 다시 수정할 수 있게 한다(평가관리 화면). */
export async function reopenEvals(rfpId, evaluator) {
  try {
    await pmsRfpsApi.reopen(rfpOf(rfpId).dbId, userIdByName(evaluator))
    await loadRfps()
  } catch (e) {
    await fail(e)
  }
}
