/**
 * 문서 테일러링 / 단계말 산출물 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 *  - 문서 테일러링: 표준 산출물 목록(단계별 문서 종류)에서 프로젝트에 적용할 문서를 정한다.
 *    필수 산출물을 제외(미적용)하려면 사유가 필요하다.
 *  - 단계말 산출물: WBS의 「단계말 검토」에서 해당 단계의 적용 문서를 등록·수정·다운로드한다(버전 관리).
 */
import { useSyncExternalStore } from 'react'

/** WBS 단계말 검토가 있는 단계(분석 → 설계 → 개발 → 테스트). */
export const DOC_PHASES = ['분석', '설계', '개발', '테스트']
export const DEFAULT_PROJECT_ID = 'PRJ-2026-001'
const PHASE_CODE = { 분석: 'AN', 설계: 'DS', 개발: 'DV', 테스트: 'TS' }

/** 표준 산출물 목록: [문서 종류, 표준 필수 여부]. */
const STANDARD = {
  분석: [
    ['요구사항 정의서', true], ['요구사항 추적표', true], ['현행 시스템 분석서', false],
    ['인터페이스 현황 분석서', false], ['데이터 이관 분석서', false], ['업무 프로세스 정의서', false],
  ],
  설계: [
    ['아키텍처 설계서', true], ['화면 설계서', true], ['인터페이스 정의서', true], ['테이블 정의서(ERD)', true],
    ['단위테스트 시나리오', true], ['프로그램 설계서', false], ['보안 설계서', false],
  ],
  개발: [
    ['소스코드 목록', true], ['단위테스트 결과서', true], ['개발자 가이드', false],
    ['코딩 표준 점검 결과서', false], ['빌드·배포 가이드', false],
  ],
  테스트: [
    ['통합테스트 시나리오', true], ['통합테스트 결과서', true], ['결함 관리대장', true], ['성능테스트 결과서', false],
    ['보안 취약점 점검 결과서', false], ['사용자 인수 테스트 결과서', false],
  ],
}

const defaultOwner = { 분석: '이설계', 설계: '이설계', 개발: '김개발', 테스트: '정QA' }

function buildDefault(projectId) {
  const items = []
  DOC_PHASES.forEach((phase) =>
    STANDARD[phase].forEach(([name, required], i) =>
      items.push({
        id: `${projectId}-${PHASE_CODE[phase]}-${String(i + 1).padStart(2, '0')}`,
        phase, name, required, applied: '적용', reason: '', owner: defaultOwner[phase], custom: false, note: '',
      }),
    ),
  )
  return items
}

const defaultCache = {}
function defaultTailoring(projectId) {
  if (!defaultCache[projectId]) defaultCache[projectId] = buildDefault(projectId)
  return defaultCache[projectId]
}

function buildSeed() {
  const pid = DEFAULT_PROJECT_ID
  const tailoring = buildDefault(pid).map((t) => {
    if (t.name === '데이터 이관 분석서') return { ...t, applied: '미적용', reason: '신규 구축 사업으로 이관 대상 데이터 없음' }
    if (t.name === '코딩 표준 점검 결과서') return { ...t, applied: '미적용', reason: '정적 분석 도구 리포트(단위테스트 결과서에 첨부)로 대체' }
    return t
  })
  tailoring.push({
    id: `${pid}-TS-90`, phase: '테스트', name: '모바일 단말 호환성 시험서', required: false, applied: '적용',
    reason: '', owner: '정QA', custom: true, note: '발주사 요청으로 추가된 프로젝트 고유 산출물',
  })

  const docs = {}
  const v = (ver, date, author, fileName, fileSize, note = '') => ({ ver, date, author, fileName, fileSize, note })
  tailoring
    .filter((t) => t.phase === '분석' && t.applied === '적용')
    .forEach((t, i) => {
      docs[t.id] = { versions: [v('v1.0', '2026-10-06', t.owner, `${t.name}_v1.0.docx`, `${(1.2 + i * 0.3).toFixed(1)}MB`, '분석 단계말 제출본')], approved: i < 3 }
    })
  const design = (name) => tailoring.find((t) => t.phase === '설계' && t.name === name)
  docs[design('아키텍처 설계서').id] = { versions: [v('v1.0', '2026-10-08', '이설계', '아키텍처 설계서_v1.0.pptx', '4.6MB', '초안')], approved: false }
  docs[design('화면 설계서').id] = {
    versions: [
      v('v1.0', '2026-10-05', '이설계', '화면 설계서_v1.0.pptx', '8.2MB', '1차'),
      v('v1.1', '2026-10-12', '이설계', '화면 설계서_v1.1.pptx', '8.9MB', '전자결재·게시판 화면 보완'),
    ],
    approved: false,
  }
  docs[design('인터페이스 정의서').id] = { versions: [v('v0.2', '2026-10-09', '김개발', '인터페이스 정의서_v0.2.xlsx', '0.8MB', 'SSO·ECM 연계 항목 작성 중')], approved: false }
  // 수행 종료된 프로젝트(PRJ-2026-010): 필수 산출물이 모두 등록·검토 완료된 상태(운영단계전환 인수 산출물 시연용)
  const pid2 = 'PRJ-2026-010'
  const tailoring2 = buildDefault(pid2)
  const docs2 = {}
  tailoring2
    .filter((t) => t.required)
    .forEach((t, i) => {
      docs2[t.id] = { versions: [v('v1.0', `2026-08-${String(10 + (i % 15)).padStart(2, '0')}`, t.owner, `${t.name}_v1.0.docx`, '1.1MB', '최종본')], approved: true }
    })
  return { tailoring: { [pid]: tailoring, [pid2]: tailoring2 }, docs: { [pid]: docs, [pid2]: docs2 } }
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

export function useDocs() {
  return useSyncExternalStore(subscribe, () => state)
}

export const tailoringOf = (s, projectId) => s.tailoring[projectId] ?? defaultTailoring(projectId)
const EMPTY_DOC = { versions: [], approved: false }
export const docOf = (s, projectId, itemId) => s.docs[projectId]?.[itemId] ?? EMPTY_DOC

export function docStatusOf(d) {
  if (d.versions.length === 0) return '미등록'
  return d.approved ? '검토 완료' : '등록'
}

/** 단계말에 제출해야 하는 문서 현황(적용 문서 기준). */
export function stageSummary(s, projectId, phase) {
  const targets = tailoringOf(s, projectId).filter((t) => t.phase === phase && t.applied === '적용')
  const registered = targets.filter((t) => docOf(s, projectId, t.id).versions.length > 0).length
  const approved = targets.filter((t) => docOf(s, projectId, t.id).approved).length
  return { total: targets.length, registered, approved }
}

const setTailoring = (projectId, items) => emit({ ...state, tailoring: { ...state.tailoring, [projectId]: items } })
const setDoc = (projectId, itemId, d) =>
  emit({ ...state, docs: { ...state.docs, [projectId]: { ...(state.docs[projectId] ?? {}), [itemId]: d } } })

/** RFP 요구사항 분류가 선택되면 자동으로 "적용"해 주는 권장 산출물. */
const CATEGORY_TRIGGERS = {
  SER: ['보안 설계서', '보안 취약점 점검 결과서'],
  PER: ['성능테스트 결과서'],
  DAR: ['데이터 이관 분석서'],
  QUR: ['코딩 표준 점검 결과서', '사용자 인수 테스트 결과서'],
  SFR: ['프로그램 설계서'],
}

/**
 * 단계전환용 문서 테일러링 기초데이터(순수 함수).
 * 선택한 단계의 표준 산출물 중 필수 문서는 적용하고, 권장 문서는 선택한 RFP 항목 분류에 해당할 때만 적용한다.
 * 나머지 권장 문서는 사유("RFP 선택 항목에 해당 분류 없음")와 함께 미적용으로 시작한다(이후 변경 가능).
 */
export function buildTailoringFor(projectId, { phases, owners, categories }) {
  const triggered = new Set([...categories].flatMap((c) => CATEGORY_TRIGGERS[c] ?? []))
  return buildDefault(projectId)
    .filter((t) => phases.includes(t.phase))
    .map((t) => {
      const owner = owners[t.phase] ?? t.owner
      if (t.required) return { ...t, owner }
      if (triggered.has(t.name)) return { ...t, owner, note: 'RFP 선택 항목 분류에 따라 자동 적용' }
      return { ...t, owner, applied: '미적용', reason: 'RFP 선택 항목에 해당 분류가 없어 자동 제외 (필요 시 적용으로 변경)' }
    })
}

/** 단계전환 실행 — 문서 테일러링 기초데이터를 저장한다. */
export function initTailoring(projectId, opts) {
  setTailoring(projectId, buildTailoringFor(projectId, opts))
}

/** 단계전환 롤백 — 해당 프로젝트의 문서 테일러링·산출물 데이터를 모두 지운다. */
export function clearProjectDocs(projectId) {
  const { [projectId]: _t, ...tailoring } = state.tailoring
  const { [projectId]: _d, ...docs } = state.docs
  emit({ ...state, tailoring, docs })
}

export function patchItem(projectId, id, patch) {
  setTailoring(projectId, tailoringOf(state, projectId).map((t) => (t.id === id ? { ...t, ...patch } : t)))
}

export function addCustomDoc(projectId, { phase, name, required, owner }) {
  const items = tailoringOf(state, projectId)
  const n = items.filter((t) => t.phase === phase && t.custom).length
  setTailoring(projectId, [
    ...items,
    { id: `${projectId}-${PHASE_CODE[phase]}-${90 + n}-${Date.now() % 1000}`, phase, name, required, applied: '적용', reason: '', owner, custom: true, note: '' },
  ])
}

export function removeItem(projectId, id) {
  setTailoring(projectId, tailoringOf(state, projectId).filter((t) => t.id !== id))
}

export function resetTailoring(projectId) {
  setTailoring(projectId, buildDefault(projectId))
}

/** 새 버전 등록(첫 등록 포함). 버전은 v1.0, v1.1, … 으로 자동 채번. */
export function uploadVersion(projectId, id, { fileName, fileSize, note, author, date }) {
  const d = docOf(state, projectId, id)
  const ver = d.versions.length === 0 ? 'v1.0' : `v1.${d.versions.length}`
  setDoc(projectId, id, { approved: false, versions: [...d.versions, { ver, date, author, fileName, fileSize, note }] })
}

/** 최신 버전 수정(파일 교체·비고 변경). */
export function updateLatest(projectId, id, patch) {
  const d = docOf(state, projectId, id)
  if (d.versions.length === 0) return
  const versions = d.versions.map((x, i) => (i === d.versions.length - 1 ? { ...x, ...patch } : x))
  setDoc(projectId, id, { ...d, versions, approved: false })
}

export function deleteLatest(projectId, id) {
  const d = docOf(state, projectId, id)
  setDoc(projectId, id, { approved: false, versions: d.versions.slice(0, -1) })
}

export function setApproved(projectId, id, approved) {
  const d = docOf(state, projectId, id)
  if (d.versions.length === 0) return
  setDoc(projectId, id, { ...d, approved })
}
