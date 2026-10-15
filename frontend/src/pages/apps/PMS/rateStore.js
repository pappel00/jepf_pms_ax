/**
 * 직무·등급별 M/M 단가 + 인건비 환산 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 단가표(직무 × 등급, 적용 기간) 와 인력별 등급을 관리하고, 투입 계획(월별 계획 M/M)에 단가를 곱해
 * 프로젝트 예상 인건비(= 예상 원가의 인건비 부분)를 계산한다. 요구사항 3.21 참고.
 */
import { useSyncExternalStore } from 'react'
import { daysInMonth, monthlyPlan } from './assignmentStore'

export const GRADES = ['특급', '고급', '중급', '초급']
export const RATE_JOBS = ['PM', 'PL', 'PMO', '분석', '설계', '분석/설계', '개발', 'DBA', 'UI/UX', 'QA', 'TA', '운영', '장애대응', '기술지원', '기타']
export const DIFFICULTIES = ['하', '중', '상']

// 등급 기본 단가(원/M/M) 와 직무별 배수 — 시드 단가표 생성용
const GRADE_BASE = { 특급: 9000000, 고급: 7500000, 중급: 6000000, 초급: 4500000 }
const JOB_FACTOR = { PM: 1.15, PL: 1.1, PMO: 1.1, TA: 1.05, DBA: 1.05 }

let seq = 0
const newId = () => `RT-${String(++seq).padStart(3, '0')}`

function buildSeed() {
  const rows = []
  RATE_JOBS.forEach((job) => {
    GRADES.forEach((grade) => {
      const unit = Math.round((GRADE_BASE[grade] * (JOB_FACTOR[job] ?? 1)) / 10000) * 10000
      rows.push({ id: newId(), job, grade, unit, from: '2026-01-01', to: '' })
    })
  })
  // 적용 기간 예시: 작년 단가(종료됨)
  GRADES.forEach((grade) => rows.push({ id: newId(), job: '개발', grade, unit: Math.round((GRADE_BASE[grade] * 0.95) / 10000) * 10000, from: '2025-01-01', to: '2025-12-31' }))
  return rows
}

let state = {
  rates: buildSeed(),
  // 인력별 등급(인력 마스터 확장 항목) — 미지정이면 단가를 적용할 수 없다
  grades: { 관리자: '특급', 이설계: '특급', 김개발: '고급', 박협력: '중급', 정QA: '중급' },
  // 난이도 가중치: 켜면 RFP 평가 확정 M/D 기반 산정(미연동)에 난이도별 배수를 곱한다. 적용 여부는 협의 사항.
  difficulty: { enabled: false, 하: 0.8, 중: 1, 상: 1.3 },
}
const listeners = new Set()
const emit = () => listeners.forEach((l) => l())
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}
export const useRateState = () => useSyncExternalStore(subscribe, () => state)

export function saveRate(rate) {
  const rates = rate.id ? state.rates.map((r) => (r.id === rate.id ? { ...r, ...rate } : r)) : [...state.rates, { ...rate, id: newId() }]
  state = { ...state, rates }
  emit()
}
export function deleteRate(id) {
  state = { ...state, rates: state.rates.filter((r) => r.id !== id) }
  emit()
}
export function setMemberGrade(name, grade) {
  const grades = { ...state.grades }
  if (grade) grades[name] = grade
  else delete grades[name]
  state = { ...state, grades }
  emit()
}
export function setDifficulty(patch) {
  state = { ...state, difficulty: { ...state.difficulty, ...patch } }
  emit()
}

/** 같은 직무·등급의 적용 기간이 겹치는 단가 행(저장 전 검증용). */
export function overlaps(rates, rate) {
  const end = (r) => r.to || '9999-12-31'
  return rates.filter((r) => r.id !== rate.id && r.job === rate.job && r.grade === rate.grade && r.from <= end(rate) && rate.from <= end(r))
}

/** 해당 월(15일 기준 아님: 월의 1일 기준)에 적용되는 단가. 없으면 null. */
export function rateFor(rates, job, grade, month) {
  if (!grade) return null
  const day = `${month}-01`
  const hit = rates.find((r) => r.job === job && r.grade === grade && r.from <= `${month}-${String(daysInMonth(month))}` && (!r.to || r.to >= day))
  return hit ? hit.unit : null
}

/**
 * 투입 한 건의 예상 인건비 — 월별 계획 M/M × 그 달 적용 단가.
 * 반환: { mm, cost, missing } (missing = 단가를 못 찾은 월 수, 해당 월 인건비는 0으로 계산)
 */
export function costOf(a, rates, grades) {
  const plan = monthlyPlan(a)
  const grade = grades[a.name]
  let mm = 0
  let cost = 0
  let missing = 0
  Object.entries(plan).forEach(([month, v]) => {
    mm += v
    const unit = rateFor(rates, a.role, grade, month)
    if (unit == null) missing += 1
    else cost += v * unit
  })
  return { mm, cost: Math.round(cost), missing, grade }
}
