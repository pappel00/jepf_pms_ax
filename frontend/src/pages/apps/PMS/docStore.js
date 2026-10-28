/**
 * 문서 테일러링 / 단계말 산출물 스토어 — 서버(`/api/v1/pms/doc-tailoring`)에서 읽고 저장한다.
 *
 *  - 문서 테일러링: 표준 산출물 마스터에서 프로젝트에 적용할 문서를 정한다. 필수 산출물을 제외(미적용)하려면 사유가 필요하다.
 *  - 단계말 산출물: WBS의 「단계말 검토」에서 해당 단계의 적용 문서를 등록·수정·다운로드한다(버전 관리, v1.0 · v1.1 …).
 * 화면의 프로젝트 id 는 프로젝트 코드, 문서 항목 id 는 서버 PK 다. 담당자는 이름으로 다룬다.
 * 산출물 파일 자체는 아직 저장하지 않는다(파일명·크기 표시값만 — module-file 연동 시 지원).
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsDocsApi } from '../../../api/entities'
import { ensurePmsUsersLoaded, userIdByName } from './pmsUsers'
import { getProjects } from './projectStore'

/** WBS 단계말 검토가 있는 단계(분석 → 설계 → 개발 → 테스트). */
export const DOC_PHASES = ['분석', '설계', '개발', '테스트']
export const DEFAULT_PROJECT_ID = 'PRJ-2026-001'

let state = { tailoring: {}, docs: {} }
let loaded = false
const listeners = new Set()

function emit(next) {
  state = next
  listeners.forEach((l) => l())
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

const day = (s) => (s ?? '').slice(0, 10)

/** 서버 응답(전체 프로젝트의 테일러링 + 버전) → 화면 상태. */
function fromApi(list) {
  const tailoring = {}
  const docs = {}
  list.forEach((t) => {
    const pid = t.projectCode
    ;(tailoring[pid] ??= []).push({
      id: t.id,
      phase: t.phaseCode,
      name: t.docName,
      required: t.required,
      applied: t.appliedCode,
      reason: t.notAppliedReason ?? '',
      owner: t.ownerName ?? '',
      custom: t.custom,
      note: t.note ?? '',
    })
    ;(docs[pid] ??= {})[t.id] = {
      approved: t.reviewed,
      versions: t.versions.map((v) => ({ ver: v.ver, date: day(v.versionDate), author: v.authorName ?? '', fileName: v.fileName ?? '', fileSize: v.fileSize ?? '', note: v.note ?? '' })),
    }
  })
  return { tailoring, docs }
}

let loading = null
export function loadDocs() {
  if (loading) return loading
  loading = ensurePmsUsersLoaded()
    .then(() => pmsDocsApi.list())
    .then((res) => {
      loaded = true
      emit(fromApi(res.data || []))
    })
    .catch((e) => {
      console.error('[docStore] 산출물 조회 실패:', e.message)
      loaded = true
      emit(state)
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useDocs() {
  useEffect(() => {
    if (!loaded) loadDocs()
  }, [])
  return useSyncExternalStore(subscribe, () => state)
}

export const tailoringOf = (s, projectId) => s.tailoring[projectId] ?? []
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

const dbIdOf = (code) => getProjects().find((p) => p.id === code)?.dbId
const guard = async (fn, message) => {
  try {
    await fn()
  } catch (e) {
    window.alert(`${message}: ${e.message}`)
  }
  await loadDocs()
}

// ---------- 단계전환 미리보기 (화면 계산 — 실제 생성은 서버가 표준 산출물 마스터로 한다) ----------

/** 표준 산출물 목록(미리보기용): [문서 종류, 표준 필수 여부]. 서버의 표준 산출물 마스터와 같은 값이다. */
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
const PHASE_CODE = { 분석: 'AN', 설계: 'DS', 개발: 'DV', 테스트: 'TS' }
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

/** RFP 요구사항 분류가 선택되면 자동으로 "적용"해 주는 권장 산출물. */
const CATEGORY_TRIGGERS = {
  SER: ['보안 설계서', '보안 취약점 점검 결과서'],
  PER: ['성능테스트 결과서'],
  DAR: ['데이터 이관 분석서'],
  QUR: ['코딩 표준 점검 결과서', '사용자 인수 테스트 결과서'],
  SFR: ['프로그램 설계서'],
}

/**
 * 단계전환 화면의 미리보기용 문서 테일러링(순수 함수).
 * 선택한 단계의 표준 산출물 중 필수 문서는 적용하고, 권장 문서는 선택한 RFP 항목 분류에 해당할 때만 적용한다.
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

/** 단계전환 승인 직후 — 서버가 표준 산출물 마스터로 문서 테일러링을 만든다. opts = { phases, owners(단계 → 이름), categories(Set) }. */
export async function initTailoring(project, { phases, owners, categories }) {
  await pmsDocsApi.init(project.dbId, {
    phases,
    owners: Object.fromEntries(Object.entries(owners).map(([ph, name]) => [ph, userIdByName(name)]).filter(([, id]) => id)),
    categories: [...categories],
  })
  await loadDocs()
}

/** 단계전환 롤백 — 서버가 테일러링·산출물을 지웠으므로 다시 읽는다. */
export async function clearProjectDocs() {
  await loadDocs()
}

// ---------- 테일러링 편집 ----------
const pendingPatch = new Map() // 항목 id → { timer, body }

async function sendPatch(id) {
  const p = pendingPatch.get(id)
  if (!p) return
  clearTimeout(p.timer)
  pendingPatch.delete(id)
  try {
    await pmsDocsApi.patch(id, p.body)
  } catch (e) {
    window.alert(`저장하지 못했습니다: ${e.message}`)
    await loadDocs()
  }
}

/** patch: { applied, reason, owner(이름), note } 중 바꿀 것만. 입력 중에는 화면에 바로 반영하고 잠깐 멈추면 저장한다. */
export function patchItem(projectId, id, patch) {
  const items = tailoringOf(state, projectId).map((t) => (t.id === id ? { ...t, ...patch } : t))
  emit({ ...state, tailoring: { ...state.tailoring, [projectId]: items } })
  const body = { ...(pendingPatch.get(id)?.body ?? {}) }
  if ('applied' in patch) body.appliedCode = patch.applied
  if ('reason' in patch) body.notAppliedReason = patch.reason
  if ('owner' in patch) body.ownerUserId = userIdByName(patch.owner) ?? ''
  if ('note' in patch) body.note = patch.note
  clearTimeout(pendingPatch.get(id)?.timer)
  pendingPatch.set(id, { body, timer: setTimeout(() => sendPatch(id), 500) })
}

export async function addCustomDoc(projectId, { phase, name, required, owner }) {
  await guard(() => pmsDocsApi.addCustom(dbIdOf(projectId), { phaseCode: phase, docName: name, required: !!required, ownerUserId: userIdByName(owner) ?? null }), '문서 종류를 추가하지 못했습니다')
}

export async function removeItem(projectId, id) {
  await guard(() => pmsDocsApi.remove(id), '삭제하지 못했습니다')
}

export async function resetTailoring(projectId) {
  await guard(() => pmsDocsApi.reset(dbIdOf(projectId)), '초기화하지 못했습니다')
}

// ---------- 산출물 버전 ----------

/** 새 버전 등록(첫 등록 포함). 버전은 v1.0, v1.1, … 으로 서버가 자동 채번. */
export async function uploadVersion(projectId, id, { fileName, fileSize, note }) {
  await guard(() => pmsDocsApi.addVersion(id, { fileName, fileSize, note }), '산출물을 등록하지 못했습니다')
}

/** 최신 버전 수정(파일 교체·비고 변경). */
export async function updateLatest(projectId, id, patch) {
  await guard(() => pmsDocsApi.updateLatest(id, { fileName: patch.fileName, fileSize: patch.fileSize, note: patch.note }), '수정하지 못했습니다')
}

export async function deleteLatest(projectId, id) {
  await guard(() => pmsDocsApi.deleteLatest(id), '삭제하지 못했습니다')
}

export async function setApproved(projectId, id, approved) {
  await guard(() => pmsDocsApi.setReviewed(id, approved), '검토 상태를 바꾸지 못했습니다')
}
