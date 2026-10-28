/**
 * 제안서 관리 스토어 — 서버(`/api/v1/pms/proposals`)에서 읽고 저장한다.
 *
 * 구조: 프로젝트 1 : N 제안서(RFP별) → 제안서 1 : N 버전(이력) / RFP 항목 대응표 / 제출 체크리스트.
 * RFP 항목 대응표는 RFP 관리의 항목(rfpStore)과 제안서 목차(절)를 연결해 "RFP를 빠짐없이 다뤘는지"를 점검한다.
 * 화면의 제안서 id 는 제안서 코드(PROP-연도-###), dbId 가 서버 PK 다. 대응표 키는 RFP 항목 id(서버 PK), RFP 는 코드, 담당자는 이름.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsProposalsApi } from '../../../api/entities'
import { ensurePmsUsersLoaded, userIdByName } from './pmsUsers'
import { getRfps } from './rfpStore'

export const PROPOSAL_STATUSES = ['작성 중', '내부 검토', '제출 완료', '결과 확정']
/** 항목 대응 방식. */
export const RESPONSES = ['수용', '일부 수용', '대안 제시', '미수용']

export const DEFAULT_CHECKLIST = ['제안서 본문', '제안서 요약본', '가격(견적) 제안서', '사업자등록증·인증서 사본', '위임장·서약서', '발표 자료(PT)']

let proposals = []
let loaded = false
const listeners = new Set()

function emit(next) {
  proposals = next
  listeners.forEach((l) => l())
}

function subscribe(listener) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

const fromApi = (p) => ({
  id: p.proposalCode,
  dbId: p.id,
  projectId: p.projectCode,
  rfpId: p.rfpCode,
  title: p.title,
  status: p.statusCode,
  owner: p.ownerName ?? '',
  dueDate: p.dueDate ?? '',
  submittedDate: p.submittedDate ?? '',
  price: p.price ?? null,
  costEstimate: p.costEstimate ?? null,
  versions: p.versions.map((v) => ({ ver: v.ver, date: v.versionDate ?? '', author: v.authorName ?? '', summary: v.summary ?? '', fileName: v.fileName ?? '', fileSize: v.fileSize ?? '' })),
  mapping: Object.fromEntries(
    p.mappings.map((m) => [m.rfpItemId, { section: m.section ?? '', response: m.responseCode ?? '', summary: m.summary ?? '', owner: m.ownerName ?? '' }]),
  ),
  checklist: p.checklist.map((c) => ({ id: c.id, name: c.itemName, done: c.done })),
})

let loading = null
export function loadProposals() {
  if (loading) return loading
  loading = ensurePmsUsersLoaded()
    .then(() => pmsProposalsApi.list())
    .then((res) => {
      loaded = true
      emit((res.data || []).map(fromApi))
    })
    .catch((e) => {
      console.error('[proposalStore] 제안서 조회 실패:', e.message)
      loaded = true
      emit(proposals)
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useProposals() {
  useEffect(() => {
    if (!loaded) loadProposals()
  }, [])
  return useSyncExternalStore(subscribe, () => proposals)
}

/** 새 제안서 폼에 보여줄 임시 코드 — 실제 코드는 서버가 채번한다. */
export function nextProposalId(all) {
  const year = new Date().getFullYear()
  const max = all
    .map((p) => p.id.match(/^PROP-(\d{4})-(\d+)$/))
    .filter((m) => m && Number(m[1]) === year)
    .reduce((s, m) => Math.max(s, Number(m[2])), 0)
  return `PROP-${year}-${String(max + 1).padStart(3, '0')}`
}

/** 다음 버전 번호: 마지막이 v1.0 이전이면 0.1씩, 이후면 소수점 한 자리씩 올린다. */
export function nextVersion(versions) {
  const last = versions[versions.length - 1]?.ver ?? 'v0.0'
  const n = Math.round(parseFloat(last.slice(1)) * 10) + 1
  return `v${Math.floor(n / 10)}.${n % 10}`
}

const byCode = (id) => proposals.find((p) => p.id === id)
const guard = async (fn, message) => {
  try {
    await fn()
  } catch (e) {
    window.alert(`${message}: ${e.message}`)
  }
  await loadProposals()
}

/** 저장 — 신규면 등록, 아니면 수정한다. 저장된 제안서(서버 코드 포함)를 돌려준다. 실패하면 예외를 던진다. */
export async function saveProposal(p) {
  const rfp = getRfps().find((r) => r.id === p.rfpId)
  if (!rfp) throw new Error('RFP를 찾을 수 없습니다.')
  const ownerUserId = userIdByName(p.owner)
  if (!ownerUserId) throw new Error('작성 책임자를 찾을 수 없습니다.')
  const body = {
    rfpId: rfp.dbId,
    title: p.title,
    statusCode: p.status,
    ownerUserId,
    dueDate: p.dueDate || null,
    submittedDate: p.submittedDate || null,
    price: p.price ?? null,
    costEstimate: p.costEstimate ?? null,
  }
  const existing = byCode(p.id)
  let code = p.id
  if (existing) {
    await pmsProposalsApi.update(existing.dbId, body)
  } else {
    const res = await pmsProposalsApi.create({ ...body, checklist: p.checklist.map((c) => c.name) })
    code = res.data.proposalCode
  }
  await loadProposals()
  return byCode(code)
}

export async function deleteProposal(id) {
  await guard(() => pmsProposalsApi.remove(byCode(id).dbId), '삭제하지 못했습니다')
}

export async function addVersion(id, version) {
  await guard(
    () => pmsProposalsApi.addVersion(byCode(id).dbId, { ver: version.ver, versionDate: version.date, summary: version.summary, fileName: version.fileName, fileSize: version.fileSize }),
    '버전을 등록하지 못했습니다',
  )
}

/** value 가 null 이면 대응을 지운다. 값이 있으면 있는 필드만 바꾼다(section/response/summary/owner). */
export async function setMapping(id, itemIdValue, value) {
  const p = byCode(id)
  await guard(() => {
    if (value === null) return pmsProposalsApi.deleteMapping(p.dbId, itemIdValue)
    const body = {}
    if ('section' in value) body.section = value.section
    if ('response' in value) body.responseCode = value.response
    if ('summary' in value) body.summary = value.summary
    if ('owner' in value) body.ownerUserId = value.owner ? userIdByName(value.owner) ?? '' : ''
    return pmsProposalsApi.saveMapping(p.dbId, itemIdValue, body)
  }, '항목 대응을 저장하지 못했습니다')
}

export async function toggleCheck(id, checkId) {
  await guard(() => pmsProposalsApi.toggleCheck(byCode(id).dbId, checkId), '체크리스트를 바꾸지 못했습니다')
}

export async function addCheck(id, name) {
  await guard(() => pmsProposalsApi.addCheck(byCode(id).dbId, name), '체크리스트를 추가하지 못했습니다')
}

export async function removeCheck(id, checkId) {
  await guard(() => pmsProposalsApi.removeCheck(byCode(id).dbId, checkId), '체크리스트를 삭제하지 못했습니다')
}

