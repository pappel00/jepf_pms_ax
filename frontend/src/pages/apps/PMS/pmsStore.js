/**
 * PMS 목업 전용 인메모리 스토어. 백엔드 API 연동 없이 화면 설계 검토(고객 gap 최소화)를
 * 위해 만든 더미 데이터와 계산 로직이다 — 새로고침하면 초기 상태로 돌아간다.
 *
 * WBS 구조는 실제 사업의 WBS 엑셀(상세일정계획(WBS)_v2.1)을 따른다:
 *   Level1 = 단계(사업관리/분석/설계/개발/테스트/이행/준공·안정화, 단계별 가중치)
 *   Level2 = 분류(그룹웨어/협업/ECM/SSO/포탈/통합검색, 단계말 검토 등)
 *   Level3 = 작업(리프, 담당자 1인)
 *
 * 규칙(요구사항 문서 docs/pms/01-요구사항-초안.md):
 *  - 3.3   리프만 진척률을 입력하고, 상위는 하위의 가중평균으로 자동 집계
 *  - 3.9.1 요구사항(=기능) 1건이 분석 → 설계 → 개발 → 테스트 4개 리프를 각 단계 아래에 생성
 *          (같은 기능은 단계마다 하나씩 포함되고, 선행 단계가 끝나야 다음 단계를 진행한다)
 *  - 3.9.2 업무당 담당자 1인, 복수 담당이 필요하면 업무 복사(가중치 균등 분배)
 */
import { useSyncExternalStore } from 'react'

export const TODAY = '2026-10-13'
export const ME = '관리자'

export const PROJECT = {
  id: 'PRJ-001',
  name: '통합업무포탈 구축사업',
  customer: '(샘플) 고객사',
  period: '2026-09-01 ~ 2027-03-31',
  stage: '설계 / 개발',
}

export const MEMBERS = [
  { name: '관리자', role: 'PM', org: '자사' },
  { name: '이설계', role: 'PL', org: '자사' },
  { name: '김개발', role: '개발', org: '자사' },
  { name: '박협력', role: '개발', org: '협력사 (가나다시스템)' },
  { name: '정QA', role: '테스트', org: '자사' },
]

/** 분석 → 설계 → 개발 → 테스트 : 기능(요구사항) 단위로 이 순서가 지켜져야 한다. */
export const CHAIN = ['분석', '설계', '개발', '테스트']
/** 기능 하나의 전체 진척을 구할 때 단계별 반영 비율(요구사항 3.9.1 템플릿). */
export const CHAIN_RATIO = { 분석: 10, 설계: 20, 개발: 50, 테스트: 20 }

// 원본 WBS의 단계별 가중치(사업관리 5 / 분석 15 / 설계 15 / 구현 30 / 테스트 10 / 이행 5 / 안정화 5).
const PHASES = [
  { code: 'PM', name: '사업관리', weight: 5 },
  { code: 'AN', name: '분석', weight: 15 },
  { code: 'DS', name: '설계', weight: 15 },
  { code: 'DV', name: '개발', weight: 30 },
  { code: 'TS', name: '테스트', weight: 10 },
  { code: 'MG', name: '이행', weight: 5 },
  { code: 'ST', name: '준공/안정화', weight: 5 },
]
const PHASE_CODE = Object.fromEntries(PHASES.map((p) => [p.name, p.code]))

/**
 * 기능(=요구사항) 목록. 원본 WBS의 그룹웨어/협업/ECM/SSO/포탈/통합검색 파트에서 대표 기능을 뽑았다.
 * dur: 단계별 소요 영업일(분석/설계/개발/테스트), owners: 단계별 담당자, size: 기능 규모(가중치),
 * slip: 해당 단계의 진척을 계획보다 늦춘 값(지연 상황 시연용).
 */
const FEATURES = [
  { id: 'REQ-001', name: '전자결재', category: '그룹웨어', start: '2026-09-01', size: 20, dur: [10, 15, 25, 10], owners: ['이설계', '이설계', '김개발', '정QA'] },
  { id: 'REQ-002', name: '게시판', category: '그룹웨어', start: '2026-09-14', size: 10, dur: [10, 10, 20, 8], owners: [ME, '이설계', '박협력', '정QA'], slip: { 분석: 10 } },
  { id: 'REQ-003', name: '일정관리', category: '그룹웨어', start: '2026-09-21', size: 10, dur: [8, 12, 20, 8], owners: ['이설계', ME, '김개발', '정QA'] },
  { id: 'REQ-004', name: '조직도·주소록', category: '그룹웨어', start: '2026-09-08', size: 10, dur: [8, 10, 15, 6], owners: ['이설계', '이설계', '김개발', '정QA'] },
  { id: 'REQ-005', name: '프로젝트·태스크 관리', category: '협업', start: '2026-09-07', size: 25, dur: [10, 15, 30, 10], owners: ['이설계', '이설계', '박협력', '정QA'] },
  { id: 'REQ-006', name: '주간보고서', category: '협업', start: '2026-09-28', size: 10, dur: [6, 8, 15, 6], owners: ['이설계', '김개발', '박협력', '정QA'] },
  { id: 'REQ-007', name: '문서 중앙화·권한', category: 'ECM', start: '2026-09-14', size: 20, dur: [10, 15, 25, 10], owners: ['이설계', '이설계', '박협력', '정QA'] },
  { id: 'REQ-008', name: 'SSO 연계', category: 'SSO', start: '2026-09-01', size: 10, dur: [8, 10, 15, 8], owners: ['이설계', '김개발', '김개발', '정QA'] },
  { id: 'REQ-009', name: '포틀릿·개인화', category: '포탈', start: '2026-09-21', size: 15, dur: [8, 10, 20, 8], owners: ['이설계', '이설계', '김개발', '정QA'] },
  { id: 'REQ-010', name: '통합검색 색인', category: '통합검색', start: '2026-10-06', size: 10, dur: [6, 8, 15, 6], owners: [ME, '김개발', '박협력', '정QA'] },
]

// ---------- 날짜 유틸 (UTC 기준, 주말만 제외) ----------
const parse = (s) => new Date(`${s}T00:00:00Z`)
const iso = (d) => d.toISOString().slice(0, 10)
const isWeekend = (d) => d.getUTCDay() === 0 || d.getUTCDay() === 6

function nextWorkday(s) {
  const d = parse(s)
  while (isWeekend(d)) d.setUTCDate(d.getUTCDate() + 1)
  return iso(d)
}

/** start(포함)부터 n영업일째 되는 날. */
function addWorkdays(s, n) {
  const d = parse(nextWorkday(s))
  let count = 1
  while (count < n) {
    d.setUTCDate(d.getUTCDate() + 1)
    if (!isWeekend(d)) count++
  }
  return iso(d)
}

function dayAfter(s) {
  const d = parse(s)
  d.setUTCDate(d.getUTCDate() + 1)
  return nextWorkday(iso(d))
}

/** 기준일(오늘) 시점의 계획 진척 — 시작~종료 구간 선형(달력일 기준). */
export function plannedProgress(start, end) {
  if (TODAY < start) return 0
  if (TODAY >= end) return 100
  const total = (parse(end) - parse(start)) / 86400000 + 1
  const elapsed = (parse(TODAY) - parse(start)) / 86400000 + 1
  return Math.round((elapsed / total) * 100)
}

const round5 = (n) => Math.round(n / 5) * 5

// ---------- 초기 데이터 생성 ----------
function buildInitialTasks() {
  const list = []
  const cats = new Set()

  PHASES.forEach((p) =>
    list.push({ id: `PH-${p.code}`, parentId: null, name: p.name, phase: p.name, weight: p.weight }),
  )

  const addLeaf = (phaseName, category, task) => {
    const code = PHASE_CODE[phaseName]
    const catId = `C-${code}-${category}`
    if (!cats.has(catId)) {
      cats.add(catId)
      list.push({ id: catId, parentId: `PH-${code}`, name: category, phase: phaseName, category, weight: 0 })
    }
    list.push({ parentId: catId, phase: phaseName, category, ...task })
  }

  // 1) 기능(요구사항)별 분석 → 설계 → 개발 → 테스트 — 단계마다 한 건씩, 앞 단계가 끝난 다음날부터 시작.
  FEATURES.forEach((f) => {
    let start = nextWorkday(f.start)
    let prev = null
    CHAIN.forEach((ph, i) => {
      const end = addWorkdays(start, f.dur[i])
      const id = `${f.id}-${PHASE_CODE[ph]}`
      let progress = Math.max(0, round5(plannedProgress(start, end)) - (f.slip?.[ph] ?? 0))
      if (prev && prev.progress < 100) progress = 0 // 선행 단계가 끝나지 않았으면 착수 불가
      addLeaf(ph, f.category, {
        id,
        name: `${f.name} ${ph}`,
        reqId: f.id,
        feature: f.name,
        assignee: f.owners[i],
        start,
        end,
        weight: f.size,
        progress,
        predId: prev?.id ?? null,
      })
      prev = { id, progress }
      start = dayAfter(end)
    })
  })

  // 2) 단계말 검토 — 그 단계의 마지막 기능 업무가 끝난 뒤 3영업일.
  CHAIN.forEach((ph) => {
    const ends = list.filter((t) => t.phase === ph && t.reqId).map((t) => t.end)
    const start = dayAfter(ends.sort().slice(-1)[0])
    const end = addWorkdays(start, 3)
    addLeaf(ph, '단계말 검토', {
      id: `RV-${PHASE_CODE[ph]}`,
      name: `${ph} 단계말 검토`,
      reqId: null,
      assignee: ME,
      start,
      end,
      weight: 10,
      progress: round5(plannedProgress(start, end)),
    })
  })

  // 3) 기능과 무관한 단계 업무(원본 WBS의 사업관리/이행/준공·안정화 항목).
  const plain = (phase, category, id, name, assignee, start, end, progress) =>
    addLeaf(phase, category, {
      id,
      name,
      reqId: null,
      assignee,
      start,
      end,
      weight: 10,
      progress: progress ?? round5(plannedProgress(start, end)),
    })

  plain('사업관리', '착수', 'PM-1', '사업수행계획서 및 WBS 작성', ME, '2026-09-01', '2026-09-04')
  plain('사업관리', '보고관리', 'PM-2', '착수보고', ME, '2026-09-08', '2026-09-08')
  plain('사업관리', '보고관리', 'PM-3', '주간보고 작성 (10월 2주차)', ME, '2026-10-13', '2026-10-13', 0)
  plain('사업관리', '보고관리', 'PM-4', '월간보고 작성 (10월)', ME, '2026-10-26', '2026-10-30')
  plain('사업관리', '보고관리', 'PM-5', '중간보고', ME, '2026-12-10', '2026-12-10')
  plain('사업관리', '보고관리', 'PM-6', '완료보고', ME, '2027-03-24', '2027-03-24')

  plain('이행', '교육', 'MG-1', '교육계획 수립', '이설계', '2027-01-04', '2027-01-15')
  plain('이행', '교육', 'MG-2', '사용자 교육', '이설계', '2027-02-01', '2027-02-12')
  plain('이행', '시스템 전환', 'MG-3', '시스템 이행계획 수립', ME, '2027-01-18', '2027-01-29')
  plain('이행', '시스템 전환', 'MG-4', '운영 환경 전환', '김개발', '2027-02-15', '2027-02-19')

  plain('준공/안정화', '시범 운영', 'ST-1', '시범운영 오픈', ME, '2027-02-22', '2027-02-22')
  plain('준공/안정화', '전사 확대', 'ST-2', '그랜드 오픈', ME, '2027-03-05', '2027-03-05')
  plain('준공/안정화', '안정화', 'ST-3', '안정화 지원', '김개발', '2027-03-08', '2027-03-31')

  // 4) 분류 노드의 가중치 = 하위 리프 가중치 합(같은 단계 안에서 분류 간 비중이 되도록).
  list
    .filter((t) => t.id.startsWith('C-'))
    .forEach((c) => {
      c.weight = list.filter((t) => t.parentId === c.id).reduce((s, t) => s + (t.weight || 0), 0)
    })

  return list.map((t) => ({ projectId: WBS_DEFAULT_PROJECT_ID, ...t }))
}

/** 기본(목업 기준) WBS 프로젝트. 다른 프로젝트의 WBS는 단계전환으로 생성된다. */
export const WBS_DEFAULT_PROJECT_ID = 'PRJ-2026-001'

let tasks = buildInitialTasks()
let nextCopyNo = 1
const listeners = new Set()

function emit(next) {
  tasks = next
  listeners.forEach((l) => l())
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function useTasks() {
  return useSyncExternalStore(subscribe, () => tasks)
}

export function childrenOf(all, id) {
  return all.filter((t) => t.parentId === id)
}

export function isLeaf(all, task) {
  return childrenOf(all, task.id).length === 0
}

/** 리프는 입력값 그대로, 상위는 하위 가중평균(소수점 첫째 자리까지). */
export function progressOf(all, task) {
  const kids = childrenOf(all, task.id)
  if (kids.length === 0) return task.progress ?? 0
  const totalWeight = kids.reduce((s, k) => s + (k.weight || 0), 0)
  if (totalWeight === 0) return 0
  const sum = kids.reduce((s, k) => s + progressOf(all, k) * (k.weight || 0), 0)
  return Math.round((sum / totalWeight) * 10) / 10
}

/** 프로젝트 전체 진척률 — 최상위(단계)들의 가중평균. */
export function projectProgress(all) {
  const roots = all.filter((t) => !t.parentId)
  const total = roots.reduce((s, t) => s + (t.weight || 0), 0)
  if (!total) return 0
  return Math.round((roots.reduce((s, t) => s + progressOf(all, t) * t.weight, 0) / total) * 10) / 10
}

export function rangeOf(all, task) {
  const kids = childrenOf(all, task.id)
  if (kids.length === 0) return { start: task.start, end: task.end }
  const ranges = kids.map((k) => rangeOf(all, k)).filter((r) => r.start)
  return {
    start: ranges.map((r) => r.start).sort()[0],
    end: ranges.map((r) => r.end).sort().slice(-1)[0],
  }
}

/** 기준일 시점에 기대되는 목표 공정율. */
export function targetProgressOf(all, task) {
  const kids = childrenOf(all, task.id)
  if (kids.length > 0) {
    const totalWeight = kids.reduce((s, k) => s + (k.weight || 0), 0)
    const sum = kids.reduce((s, k) => s + targetProgressOf(all, k) * (k.weight || 0), 0)
    return totalWeight ? Math.round((sum / totalWeight) * 10) / 10 : 0
  }
  return plannedProgress(task.start, task.end)
}

export function statusOf(all, task) {
  const p = progressOf(all, task)
  const { start, end } = rangeOf(all, task)
  if (p >= 100) return '완료'
  if (end && end < TODAY) return '지연'
  if (start && start > TODAY) return '미착수'
  return '진행중'
}

/** 선행 업무가 아직 끝나지 않았으면 그 업무를 돌려준다(없으면 null). */
export function blockedBy(all, task) {
  if (!task.predId) return null
  const pred = all.find((t) => t.id === task.predId)
  return pred && (pred.progress ?? 0) < 100 ? pred : null
}

/** 트리 순서대로 평탄화하면서 WBS 번호(1, 1.1, 1.1.1 …)를 붙인다. */
export function flattenTree(all, expanded) {
  const rows = []
  const walk = (parentId, prefix, depth) => {
    childrenOf(all, parentId).forEach((t, i) => {
      const no = prefix ? `${prefix}.${i + 1}` : `${i + 1}`
      rows.push({ task: t, no, depth })
      if (expanded.has(t.id)) walk(t.id, no, depth + 1)
    })
  }
  walk(null, '', 0)
  return rows
}

/**
 * 기능 × 단계 매트릭스 — 같은 기능(요구사항)이 분석/설계/개발/테스트에 어떻게 걸쳐 있는지 본다.
 * 기능 전체 진척은 단계 비율(CHAIN_RATIO)로 가중평균한다.
 */
export function featureMatrix(all) {
  const feats = [...new Map(all.filter((t) => t.reqId).map((t) => [t.reqId, t])).values()]
  return feats.map((f) => {
    const cells = {}
    let weighted = 0
    let ratioSum = 0
    CHAIN.forEach((ph) => {
      const ts = all.filter((t) => t.reqId === f.reqId && t.phase === ph && isLeaf(all, t))
      if (ts.length === 0) {
        cells[ph] = null
        return
      }
      const w = ts.reduce((s, t) => s + (t.weight || 0), 0) || 1
      const progress = Math.round((ts.reduce((s, t) => s + (t.progress || 0) * (t.weight || 0), 0) / w) * 10) / 10
      const start = ts.map((t) => t.start).sort()[0]
      const end = ts.map((t) => t.end).sort().slice(-1)[0]
      const status = progress >= 100 ? '완료' : end < TODAY ? '지연' : start > TODAY ? '미착수' : '진행중'
      cells[ph] = {
        progress,
        start,
        end,
        status,
        assignees: [...new Set(ts.map((t) => t.assignee))],
        blocked: ts.some((t) => blockedBy(all, t)),
      }
      weighted += progress * CHAIN_RATIO[ph]
      ratioSum += CHAIN_RATIO[ph]
    })
    return {
      reqId: f.reqId,
      feature: f.feature,
      category: f.category,
      cells,
      overall: ratioSum ? Math.round((weighted / ratioSum) * 10) / 10 : 0,
    }
  })
}

/**
 * 진척률 입력. 선행 단계(분석 → 설계 → 개발 → 테스트)가 끝나지 않은 업무는 0%보다 큰 값을
 * 입력할 수 없다 — 막히면 선행 업무를 돌려주고 변경하지 않는다.
 */
export function setProgress(id, value) {
  const task = tasks.find((t) => t.id === id)
  const v = Math.max(0, Math.min(100, Number(value) || 0))
  if (task && v > 0) {
    const pred = blockedBy(tasks, task)
    if (pred) return { ok: false, pred }
  }
  emit(tasks.map((t) => (t.id === id ? { ...t, progress: v } : t)))
  return { ok: true }
}

/**
 * 단계전환용 WBS 기초데이터 생성(순수 함수 — 스토어는 바꾸지 않는다).
 *  - 사업관리 + 선택한 단계(분석/설계/개발/테스트)를 Level1, RFP 분류를 Level2, 선택한 RFP 항목(기능)을 Level3로 만든다.
 *  - 항목마다 선택한 단계 순서대로 업무를 하나씩 만들고(선행 순서 finish-to-start), 소요일은 항목 M/D를 단계 비율로 나눠 정한다.
 *  - 진척률은 모두 0%에서 시작한다. 단계말 검토는 단계 마지막 업무 뒤에 둔다.
 * features: [{ code, name, categoryName, md, weight }]
 * owners: { admin, mode, byPhase: { 분석: [{ name, weight }…], 설계: [...], 개발: [...], 테스트: [...] } }
 *  - admin: 프로젝트 관리자 — 사업관리·단계말 검토 업무 담당
 *  - byPhase: 단계별 업무 담당 후보 = 그 단계에 투입된 인력 전체(역할 우선순위 순, 개발자 우선), weight = 투입 M/M(없으면 1)
 *  - mode: 'auto' 후보 전원에게 투입 M/M 비율로 분배 / 'lead' 첫 번째 후보 1명 / 'none' 미배정
 *  후보가 없으면 미배정('')으로 생성한다(후보에 프로젝트 관리자를 넣어 두면 관리자에게 배정된다).
 */
export function buildProjectWbs({ projectId, startDate, phases, features, owners }) {
  const pid = projectId
  const selected = CHAIN.filter((p) => phases.includes(p))
  const roots = [{ name: '사업관리', code: 'PM', weight: 5 }, ...PHASES.filter((p) => selected.includes(p.name))]
  const list = roots.map((p) => ({ id: `${pid}-PH-${p.code}`, projectId: pid, parentId: null, name: p.name, phase: p.name, weight: p.weight }))
  const cats = new Set()
  const addLeaf = (phaseName, code, category, task) => {
    const catId = `${pid}-C-${code}-${category}`
    if (!cats.has(catId)) {
      cats.add(catId)
      list.push({ id: catId, projectId: pid, parentId: `${pid}-PH-${code}`, name: category, phase: phaseName, category, weight: 0 })
    }
    list.push({ projectId: pid, parentId: catId, phase: phaseName, category, ...task })
  }

  const admin = owners.admin ?? ''
  const start0 = nextWorkday(startDate)
  const pmEnd = addWorkdays(start0, 5)
  addLeaf('사업관리', 'PM', '착수', { id: `${pid}-PM-1`, name: '사업수행계획서 및 WBS 작성', reqId: null, assignee: admin, start: start0, end: pmEnd, weight: 10, progress: 0 })
  const kickoff = dayAfter(pmEnd)
  addLeaf('사업관리', 'PM', '보고관리', { id: `${pid}-PM-2`, name: '착수보고', reqId: null, assignee: admin, start: kickoff, end: kickoff, weight: 10, progress: 0 })

  const categories = [...new Set(features.map((f) => f.categoryName))]
  const given = {} // 단계별 인력별 배정 건수(투입 M/M 비율 분배용)
  features.forEach((f) => {
    let start = addWorkdays(start0, 1 + categories.indexOf(f.categoryName) * 3) // 분류별로 시작을 조금씩 어긋나게
    let prevId = null
    selected.forEach((ph) => {
      const days = Math.max(2, Math.ceil((f.md * CHAIN_RATIO[ph]) / 100))
      const end = addWorkdays(start, days)
      const id = `${pid}-${f.code}-${PHASE_CODE[ph]}`
      const pool = owners.byPhase?.[ph] ?? []
      let assignee = ''
      if (pool.length > 0 && owners.mode !== 'none') {
        if (owners.mode === 'lead') {
          assignee = pool[0].name
        } else {
          // (배정 건수 + 1) ÷ 투입 M/M 이 가장 작은 사람에게 배정 → M/M 비율대로 나눠 갖고, 동률이면 앞 순서(개발자 우선)
          const g = (given[ph] ??= {})
          let best = pool[0]
          pool.forEach((p) => {
            if (((g[p.name] ?? 0) + 1) / p.weight < ((g[best.name] ?? 0) + 1) / best.weight) best = p
          })
          g[best.name] = (g[best.name] ?? 0) + 1
          assignee = best.name
        }
      }
      addLeaf(ph, PHASE_CODE[ph], f.categoryName, {
        id, name: `${f.name} ${ph}`, reqId: f.code, feature: f.name,
        assignee, start, end, weight: f.weight, progress: 0, predId: prevId,
      })
      prevId = id
      start = dayAfter(end)
    })
  })

  let prevReviewEnd = pmEnd
  selected.forEach((ph) => {
    const ends = list.filter((t) => t.phase === ph && t.reqId).map((t) => t.end)
    const start = dayAfter(ends.length ? ends.sort().slice(-1)[0] : prevReviewEnd)
    const end = addWorkdays(start, 3)
    prevReviewEnd = end
    addLeaf(ph, PHASE_CODE[ph], '단계말 검토', { id: `${pid}-RV-${PHASE_CODE[ph]}`, name: `${ph} 단계말 검토`, reqId: null, assignee: admin, start, end, weight: 10, progress: 0 })
  })

  list.filter((t) => t.id.startsWith(`${pid}-C-`)).forEach((c) => {
    c.weight = list.filter((t) => t.parentId === c.id).reduce((s, t) => s + (t.weight || 0), 0)
  })
  return list
}

/** 단계전환 실행 — 생성한 WBS를 스토어에 추가한다(이미 WBS가 있는 프로젝트면 무시). */
export function addProjectTasks(newTasks) {
  const pid = newTasks[0]?.projectId
  if (!pid || tasks.some((t) => t.projectId === pid)) return false
  emit([...tasks, ...newTasks])
  return true
}

/** 단계전환 롤백 — 해당 프로젝트의 WBS 업무를 모두 지운다. */
export function removeProjectTasks(projectId) {
  emit(tasks.filter((t) => t.projectId !== projectId))
}

/**
 * 업무 복사 — 담당자만 바꿔서 같은 업무를 한 명 더 만든다(진척률 0으로 초기화).
 * 가중치는 복사 그룹(원본 + 복사본들)의 합계를 균등 분배한다(요구사항 3.9.2).
 */
export function copyTask(id, assignee) {
  const src = tasks.find((t) => t.id === id)
  if (!src) return
  const rootId = src.copiedFromId ?? src.id
  const group = tasks.filter((t) => t.id === rootId || t.copiedFromId === rootId)
  const total = group.reduce((s, t) => s + (t.weight || 0), 0)
  const each = Math.round((total / (group.length + 1)) * 10) / 10
  const copy = {
    ...src,
    id: `${src.id}-C${nextCopyNo++}`,
    assignee,
    progress: 0,
    copiedFromId: rootId,
    weight: each,
  }
  const groupIds = new Set(group.map((t) => t.id))
  const updated = tasks.map((t) => (groupIds.has(t.id) ? { ...t, weight: each } : t))
  // 복사본은 원본(과 기존 복사본들) 바로 뒤에 놓아서 WBS에서 나란히 보이게 한다.
  const lastIdx = Math.max(...group.map((t) => updated.findIndex((u) => u.id === t.id)))
  emit([...updated.slice(0, lastIdx + 1), copy, ...updated.slice(lastIdx + 1)])
}
