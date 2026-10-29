/**
 * RFP 관리 스토어 — 서버(`/api/v1/pms/rfps`)에서 읽고 저장한다.
 *
 * 구조: 프로젝트 1 : N RFP(차수/재공고) → RFP 1 : N 항목(세분화된 요구사항) → 항목 1 : N 평가(평가자별).
 * 이 화면(RFP 관리)은 문서 접수와 항목 정리, 평가자 지정까지를 다루고, 항목별 평가(기능/비기능 구분,
 * 구현 여부, 예상 M/D)는 「RFP 평가」 화면에서 한다 — 여기서는 평가 진행 현황(evalDone)만 보여준다.
 * 화면의 RFP id 는 RFP 코드(RFP-연도-###), dbId 가 서버 PK 이고 항목 id 는 서버 PK 다. 평가자는 이름으로 다룬다.
 * 원본 문서 파일은 아직 저장하지 않는다(module-file 연동 시 지원).
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsRfpsApi } from '../../../api/entities'
import { evalDoneOf, hydrateEvals, isSubmitted } from './evalStore'
import { ensurePmsUsersLoaded, userIdByName } from './pmsUsers'
import { getProjects } from './projectStore'

/** 공공/SI 사업 RFP에서 흔히 쓰는 요구사항 분류(발주사 원문 기준). 기능/비기능 구분은 PM 평가 결과로 정한다. */
export const REQ_CATEGORIES = [
  { code: 'SFR', name: '기능 요구사항' },
  { code: 'PER', name: '성능 요구사항' },
  { code: 'SER', name: '보안 요구사항' },
  { code: 'ECR', name: '시스템 장비 구성 요구사항' },
  { code: 'DAR', name: '데이터 요구사항' },
  { code: 'QUR', name: '품질 요구사항' },
  { code: 'COR', name: '제약사항' },
  { code: 'PMR', name: '프로젝트 관리 요구사항' },
  { code: 'PSR', name: '프로젝트 지원 요구사항' },
  { code: 'INF', name: '클라우드 서비스 요구사항' },
  { code: 'SIR', name: '인터페이스 요구사항' },
  { code: 'TER', name: '테스트 요구사항' },
]
export const categoryName = (code) => REQ_CATEGORIES.find((c) => c.code === code)?.name ?? code

export const ROUNDS = ['1차', '2차', '재공고', '변경 공고']

export const INTAKE_METHODS = [
  { value: 'excel', label: '엑셀 템플릿 업로드 (권장)', desc: '항목 양식에 맞춰 정리한 엑셀을 올리면 항목이 한 번에 등록됩니다.' },
  { value: 'manual', label: '수동 입력', desc: '원본 문서를 보면서 항목을 직접 하나씩 추가합니다.' },
  { value: 'auto', label: '문서 자동 분해 (hwpx)', desc: '한글(hwpx) 원본 문서의 요구사항 표를 읽어 항목 후보를 만듭니다. 저장 전에 미리보기로 확인합니다.' },
]

let rfps = []
let loaded = false
export const getRfps = () => rfps
const listeners = new Set()

function emit(next) {
  rfps = next
  listeners.forEach((l) => l())
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

/** 서버 응답 → 화면 RFP. 평가자는 이름 배열, 항목은 화면 항목 형태. */
const fromApi = (r) => ({
  id: r.rfpCode,
  dbId: r.id,
  projectId: r.projectCode,
  title: r.title,
  round: r.roundCode,
  receivedDate: r.receivedDate ?? '',
  dueDate: r.dueDate ?? '',
  fileName: '',
  fileSize: '',
  evaluators: r.evaluators.map((e) => e.userName),
  items: r.items.map((i) => ({
    id: i.id,
    code: i.itemCode,
    category: i.categoryCode,
    area: i.area ?? '',
    name: i.itemName,
    content: i.content ?? '',
    page: i.sourcePage ?? null,
    mandatory: i.mandatory,
    evalDone: i.evalDone,
  })),
})

let loading = null
/** RFP·항목·평가자와 평가·확정 값을 한꺼번에 읽어 두 스토어(rfp, eval)를 갱신한다. */
export function loadRfps() {
  if (loading) return loading
  loading = ensurePmsUsersLoaded()
    .then(() => Promise.all([pmsRfpsApi.list(), pmsRfpsApi.evals()]))
    .then(([rfpRes, evalRes]) => {
      const raw = rfpRes.data || []
      const evals = {}
      const subs = {}
      const confirmed = {}
      const rfpOfItem = {}
      raw.forEach((r) => {
        r.evaluators.forEach((e) => {
          subs[`${r.rfpCode}|${e.userName}`] = e.submitted
        })
        r.items.forEach((i) => {
          rfpOfItem[i.id] = r.rfpCode
          if (i.confirmedMd !== null && i.confirmedMd !== undefined) {
            confirmed[`${r.rfpCode}|${i.id}`] = { evaluator: i.confirmedEvaluatorName ?? '', md: Number(i.confirmedMd), edited: i.confirmedEdited }
          }
        })
      })
      ;(evalRes.data || []).forEach((e) => {
        const code = rfpOfItem[e.itemId]
        if (!code) return
        evals[`${code}|${e.itemId}|${e.evaluatorName}`] = {
          kind: e.kindCode ?? '',
          impl: e.implCode ?? null,
          difficulty: e.difficultyCode ?? null,
          md: e.md === null || e.md === undefined ? null : Number(e.md),
          comment: e.comment ?? '',
        }
      })
      hydrateEvals({ evals, subs, confirmed })
      loaded = true
      emit(raw.map(fromApi))
    })
    .catch((e) => {
      console.error('[rfpStore] RFP 조회 실패:', e.message)
      loaded = true
      emit(rfps)
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useRfps() {
  useEffect(() => {
    if (!loaded) loadRfps()
  }, [])
  return useSyncExternalStore(subscribe, () => rfps)
}

/**
 * 항목/평가자/평가 입력 상태에서 RFP 상태를 계산한다(저장하지 않음).
 * 평가 완료 = 모든 평가자가 모든 항목을 입력하고 제출까지 마친 상태.
 */
export function rfpStatusOf(rfp) {
  if (rfp.items.length === 0) return '접수'
  if (rfp.evaluators.length === 0) return '항목 정리 중'
  const n = rfp.evaluators.length
  const allDone = rfp.items.every((i) => evalDoneOf(rfp, i) >= n) && rfp.evaluators.every((e) => isSubmitted(rfp.id, e))
  return allDone ? '평가 완료' : '평가 중'
}

/** 평가 진행률(%) = 입력 완료 수 합 ÷ (항목 × 평가자). */
export function evalProgressOf(rfp) {
  const n = rfp.evaluators.length
  if (!n || rfp.items.length === 0) return 0
  const done = rfp.items.reduce((s, i) => s + Math.min(evalDoneOf(rfp, i), n), 0)
  return Math.round((done / (rfp.items.length * n)) * 100)
}

/** 새 RFP 폼에 보여줄 임시 코드 — 실제 코드는 서버가 채번한다. */
export function nextRfpId(all) {
  const year = new Date().getFullYear()
  const max = all
    .map((r) => r.id.match(/^RFP-(\d{4})-(\d+)$/))
    .filter((m) => m && Number(m[1]) === year)
    .reduce((s, m) => Math.max(s, Number(m[2])), 0)
  return `RFP-${year}-${String(max + 1).padStart(3, '0')}`
}

export function nextItemCode(rfp, category) {
  const max = rfp.items
    .filter((i) => i.category === category)
    .reduce((s, i) => Math.max(s, Number(i.code.split('-')[1]) || 0), 0)
  return `${category}-${String(max + 1).padStart(3, '0')}`
}

const rfpByCode = (id) => rfps.find((r) => r.id === id)

/** 저장 — 신규면 등록, 아니면 수정한다. 저장된 RFP(서버 코드 포함)를 돌려준다. 실패하면 예외를 던진다. */
export async function saveRfp(rfp) {
  const body = { title: rfp.title, roundCode: rfp.round, receivedDate: rfp.receivedDate || null, dueDate: rfp.dueDate || null }
  const existing = rfpByCode(rfp.id)
  let code = rfp.id
  if (existing) {
    await pmsRfpsApi.update(existing.dbId, body)
  } else {
    const project = getProjects().find((p) => p.id === rfp.projectId)
    if (!project) throw new Error('프로젝트를 찾을 수 없습니다.')
    const res = await pmsRfpsApi.create(project.dbId, body)
    code = res.data.rfpCode
  }
  await loadRfps()
  return rfpByCode(code)
}

export async function deleteRfp(id) {
  try {
    await pmsRfpsApi.remove(rfpByCode(id).dbId)
  } catch (e) {
    window.alert(`삭제하지 못했습니다: ${e.message}`)
  }
  await loadRfps()
}

export async function setEvaluators(rfpId, names) {
  try {
    await pmsRfpsApi.setEvaluators(rfpByCode(rfpId).dbId, names.map(userIdByName).filter(Boolean))
  } catch (e) {
    window.alert(`평가자를 지정하지 못했습니다: ${e.message}`)
  }
  await loadRfps()
}

const itemBody = (item) => ({
  itemCode: item.itemCode ?? null,
  categoryCode: item.category,
  area: item.area || null,
  itemName: item.name,
  content: item.content || null,
  sourcePage: item.page ?? null,
  mandatory: !!item.mandatory,
})

export async function saveItem(rfpId, item) {
  const rfp = rfpByCode(rfpId)
  try {
    if (rfp.items.some((i) => i.id === item.id)) await pmsRfpsApi.updateItem(item.id, itemBody(item))
    else await pmsRfpsApi.addItem(rfp.dbId, itemBody(item))
  } catch (e) {
    window.alert(`항목을 저장하지 못했습니다: ${e.message}`)
  }
  await loadRfps()
}

export async function deleteItem(rfpId, itemId) {
  try {
    await pmsRfpsApi.deleteItem(itemId)
  } catch (e) {
    window.alert(`항목을 삭제하지 못했습니다: ${e.message}`)
  }
  await loadRfps()
}

/** RFP 문서(hwpx)를 서버에서 분석해 항목 후보를 받는다(저장하지 않음). */
export async function parseRfpDocument(file, splitLines) {
  const res = await pmsRfpsApi.parse(file, splitLines)
  return res.data
}

/** 자동 분해로 읽은 항목 후보를 RFP 에 일괄 등록한다(원문 고유번호 유지). 등록한 건수를 돌려준다. */
export async function importParsedItems(rfpId, items) {
  const rfp = rfpByCode(rfpId)
  const body = items.map((i) => itemBody({ itemCode: i.itemCode, category: i.categoryCode, area: i.area, name: i.itemName, content: i.content, page: null, mandatory: i.mandatory }))
  let done = 0
  try {
    for (let i = 0; i < body.length; i += 500) {
      await pmsRfpsApi.addItems(rfp.dbId, body.slice(i, i + 500))
      done = Math.min(i + 500, body.length)
    }
  } catch (e) {
    await loadRfps()
    // 어느 항목에서 왜 실패했는지 알려 준다(서버 검증 오류는 첫 번째 필드를 덧붙인다).
    const first = e.errors?.[0]
    throw new Error(`항목 ${body.length}건 중 ${done}건까지 등록하고 중단했습니다: ${e.message}${first ? ` (${first.field}: ${first.reason})` : ''}`)
  }
  await loadRfps()
  return body.length
}

/** 목업: 엑셀 템플릿을 올렸다고 가정하고 샘플 항목을 만들어 붙인다. */
export async function importSampleItems(rfpId) {
  const samples = [
    ['SFR', '샘플 영역', '샘플 기능 항목 1', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 5, true],
    ['SFR', '샘플 영역', '샘플 기능 항목 2', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 5, true],
    ['SFR', '샘플 영역', '샘플 기능 항목 3', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 6, false],
    ['PER', '응답 성능', '샘플 성능 항목', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 8, true],
    ['SER', '접근 통제', '샘플 보안 항목', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 9, true],
    ['PMR', '보고', '샘플 관리 항목', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 11, true],
  ].map(([category, area, name, content, page, mandatory]) => ({ category, area, name, content, page, mandatory }))
  try {
    await pmsRfpsApi.addItems(rfpByCode(rfpId).dbId, samples.map(itemBody))
  } catch (e) {
    window.alert(`항목을 추가하지 못했습니다: ${e.message}`)
  }
  await loadRfps()
}

