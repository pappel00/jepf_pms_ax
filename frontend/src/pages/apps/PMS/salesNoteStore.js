/**
 * 영업 노트(영업 활동 기록) 스토어 — 서버(`/api/v1/pms/sales-notes`)에서 읽고 저장한다.
 *
 * 영업 담당자가 사전영업·제안 과정에서 한 활동(방문·미팅·통화 등)과 고객 반응, 다음 할 일을 남기는 기록.
 * 담당자가 바뀌어도 프로젝트/고객사 단위로 히스토리를 이어서 볼 수 있게 하는 것이 목적이다.
 * 화면의 노트 id 는 노트 코드(SN-###), 프로젝트·고객사·담당자는 각 코드로 다룬다. 작성자는 로그인 사용자가 된다.
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsSalesNotesApi } from '../../../api/entities'
import { contactCodeOfDbId, contactDbId, customerCodeOfDbId, customerDbId, ensureCustomersLoaded } from './customerStore'
import { ensurePmsUsersLoaded } from './pmsUsers'
import { getProjects } from './projectStore'

export const NOTE_TYPES = ['방문', '미팅', '전화', '이메일', '식사·접대', '제안·발표', '자료 전달', '정보 수집', '인수인계', '기타']

let notes = []
let loaded = false
const listeners = new Set()
const emit = (next) => {
  notes = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}

const fromApi = (n) => ({
  id: n.noteCode,
  dbId: n.id,
  date: n.noteDate,
  type: n.noteTypeCode,
  author: n.authorName ?? '',
  projectId: n.projectCode ?? '',
  customerId: customerCodeOfDbId(n.customerId),
  contactId: contactCodeOfDbId(n.contactId),
  title: n.title,
  content: n.content,
  nextAction: n.nextAction ?? '',
  nextDate: n.nextDate ?? '',
  done: n.done,
})

let loading = null
export function loadSalesNotes() {
  if (loading) return loading
  loading = Promise.all([ensureCustomersLoaded(), ensurePmsUsersLoaded()])
    .then(() => pmsSalesNotesApi.list())
    .then((res) => {
      loaded = true
      emit((res.data || []).map(fromApi))
    })
    .catch((e) => {
      console.error('[salesNoteStore] 영업 노트 조회 실패:', e.message)
      loaded = true
      emit(notes)
    })
    .finally(() => {
      loading = null
    })
  return loading
}

export function useSalesNotes() {
  useEffect(() => {
    if (!loaded) loadSalesNotes()
  }, [])
  return useSyncExternalStore(subscribe, () => notes)
}

export const emptyNote = (author, today) => ({
  id: '', date: today, type: '미팅', author, projectId: '', customerId: '', contactId: '', title: '', content: '', nextAction: '', nextDate: '', done: false,
})

const toApi = (n) => ({
  noteDate: n.date,
  noteTypeCode: n.type,
  projectId: getProjects().find((p) => p.id === n.projectId)?.dbId ?? null,
  customerId: customerDbId(n.customerId) ?? null,
  contactId: contactDbId(n.contactId) ?? null,
  title: n.title,
  content: n.content,
  nextAction: n.nextAction || null,
  nextDate: n.nextDate || null,
  done: !!n.done,
})

const guard = async (fn, message) => {
  try {
    await fn()
  } catch (e) {
    window.alert(`${message}: ${e.message}`)
  }
  await loadSalesNotes()
}

export async function saveNote(n) {
  const existing = notes.find((x) => x.id === n.id)
  await guard(() => (existing ? pmsSalesNotesApi.update(existing.dbId, toApi(n)) : pmsSalesNotesApi.create(toApi(n))), '영업 노트를 저장하지 못했습니다')
}
export async function deleteNote(id) {
  await guard(() => pmsSalesNotesApi.remove(notes.find((n) => n.id === id).dbId), '삭제하지 못했습니다')
}
export async function toggleDone(id) {
  await guard(() => pmsSalesNotesApi.toggleDone(notes.find((n) => n.id === id).dbId), '상태를 바꾸지 못했습니다')
}
