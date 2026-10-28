/**
 * 직무·등급별 M/M 단가 + 인건비 환산 스토어 — 단가표와 인력 등급은 서버에서 읽고 저장한다(단가표는 원가 열람 권한 필요).
 *
 * 단가표(직무 × 등급, 적용 기간) 와 인력별 등급을 관리하고, 투입 계획(월별 계획 M/M)에 단가를 곱해
 * 프로젝트 예상 인건비(= 예상 원가의 인건비 부분)를 계산한다. 요구사항 3.21 참고.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsCostApi, pmsUserProfilesApi } from '../../../api/entities'
import { daysInMonth, monthlyPlan } from './assignmentStore'
import { ensurePmsUsersLoaded, getUsers, loadPmsUsers } from './pmsUsers'

export const GRADES = ['특급', '고급', '중급', '초급']
export const RATE_JOBS = ['PM', 'PL', 'PMO', '분석', '설계', '분석/설계', '개발', 'DBA', 'UI/UX', 'QA', 'TA', '운영', '장애대응', '기술지원', '기타']
export const DIFFICULTIES = ['하', '중', '상']

let state = {
  rates: [],
  // 인력별 등급(인력 프로필 pms_user_profile.job_grade_code) — 미지정이면 단가를 적용할 수 없다
  grades: {},
  // 난이도 가중치: 켜면 RFP 평가 확정 M/D 기반 산정(미연동)에 난이도별 배수를 곱한다. 적용 여부는 협의 사항(저장하지 않음).
  difficulty: { enabled: false, 하: 0.8, 중: 1, 상: 1.3 },
}
let loaded = false
const listeners = new Set()
const emit = () => listeners.forEach((l) => l())
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}

const gradesFromUsers = () => Object.fromEntries(getUsers().filter((u) => u.jobGradeCode).map((u) => [u.userName, u.jobGradeCode]))

/** 단가표(원가 열람 권한 필요)와 인력 등급을 서버에서 읽는다. 권한이 없으면 단가표는 비어 있다. */
export function loadRates() {
  return ensurePmsUsersLoaded()
    .then(() => pmsCostApi.rates().catch(() => ({ data: [] })))
    .then((res) => {
      state = {
        ...state,
        rates: (res.data || []).map((r) => ({ id: r.id, job: r.jobCode, grade: r.gradeCode, unit: Number(r.unitPrice), from: r.fromDate, to: r.toDate ?? '' })),
        grades: gradesFromUsers(),
      }
      loaded = true
      emit()
    })
}

export function useRateState() {
  useEffect(() => {
    if (!loaded) loadRates()
  }, [])
  return useSyncExternalStore(subscribe, () => state)
}

const fail = (message) => (e) => {
  window.alert(`${message}: ${e.message}`)
  return false
}

/** 저장(신규/수정) — 성공하면 true. 같은 직무·등급의 적용 기간 겹침 등은 서버가 거부한다. */
export async function saveRate(rate) {
  const body = { jobCode: rate.job, gradeCode: rate.grade, unitPrice: Number(rate.unit), fromDate: rate.from, toDate: rate.to || null }
  const ok = await (rate.id ? pmsCostApi.updateRate(rate.id, body) : pmsCostApi.createRate(body)).then(() => true, fail('단가를 저장하지 못했습니다'))
  await loadRates()
  return ok
}
export async function deleteRate(id) {
  await pmsCostApi.deleteRate(id).catch(fail('삭제하지 못했습니다'))
  await loadRates()
}
/** 인력 등급 변경 — 인력 프로필(job_grade_code)에 저장한다. */
export async function setMemberGrade(name, grade) {
  const u = getUsers().find((x) => x.userName === name)
  if (!u) return
  await pmsUserProfilesApi
    .upsert(u.userId, {
      orgTypeCode: u.orgTypeCode,
      partnerCompany: u.partnerCompany ?? null,
      notifyChannelCode: u.notifyChannelCode ?? null,
      telegramChatId: u.telegramChatId ?? null,
      mattermostUserId: u.mattermostUserId ?? null,
      jobGradeCode: grade || null,
    })
    .catch(fail('등급을 저장하지 못했습니다'))
  await loadPmsUsers()
  await loadRates()
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
