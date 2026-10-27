/**
 * 고객사 / 고객사 담당자 스토어 — module-pms API(/api/v1/pms/customers, /customer-contacts) 기반.
 *
 * 화면 전체가 동기 훅(useCustomerState)으로 읽으므로 이 모듈이 서버 결과를 메모리에 캐시한다. 화면에서 쓰는 id는
 * 고객사 코드(CUS-###)·담당자 코드(CTC-###)이고(다른 목업 데이터가 이 코드를 참조), 서버 PK는 dbId에 둔다.
 * 저장·삭제는 API 호출 후 목록을 다시 불러온다(실패하면 예외를 던지므로 호출한 화면이 메시지를 보여준다).
 */
import { useEffect, useSyncExternalStore } from 'react'
import { pmsContactsApi, pmsCustomersApi } from '../../../api/entities'

export const INDUSTRIES = ['제조', '금융', '유통/물류', '공공', '의료/제약', '에너지', 'IT/서비스', '기타']
export const CUSTOMER_STATUSES = ['거래 중', '잠재', '거래 종료']

let state = { customers: [], contacts: [], loaded: false, error: '' }
const listeners = new Set()
const emit = (next) => {
  state = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}

const customerFromApi = (c) => ({
  id: c.customerCode, dbId: c.id, name: c.customerName, industry: c.industryCode ?? '', status: c.dealStatusCode,
  bizNo: c.bizNo ?? '', phone: c.phone ?? '', address: c.address ?? '', homepage: c.homepage ?? '', memo: c.memo ?? '',
})

let loading = null
/** 서버에서 고객사·담당자를 다시 읽어 캐시를 갱신한다. */
export function loadCustomers() {
  if (loading) return loading
  loading = Promise.all([pmsCustomersApi.list('?size=1000&sort=customerCode'), pmsContactsApi.list('?size=2000&sort=contactCode')])
    .then(([cRes, tRes]) => {
      const customers = (cRes.data || []).map(customerFromApi)
      const codeOfDbId = Object.fromEntries(customers.map((c) => [c.dbId, c.id]))
      const contacts = (tRes.data || []).map((t) => ({
        id: t.contactCode, dbId: t.id, customerId: codeOfDbId[t.customerId] ?? '', customerDbId: t.customerId, name: t.contactName,
        title: t.jobTitle ?? '', dept: t.deptName ?? '', phone: t.phone ?? '', email: t.email ?? '', memo: t.memo ?? '', useYn: t.useYn,
      }))
      emit({ customers, contacts, loaded: true, error: '' })
    })
    .catch((e) => emit({ ...state, loaded: true, error: e.message }))
    .finally(() => {
      loading = null
    })
  return loading
}

/** 최초 한 번 서버에서 불러온다(다른 스토어가 코드↔PK 변환에 쓸 때도 이걸 기다린다). */
export function ensureCustomersLoaded() {
  return state.loaded ? Promise.resolve() : loadCustomers()
}

export function useCustomerState() {
  useEffect(() => {
    ensureCustomersLoaded()
  }, [])
  return useSyncExternalStore(subscribe, () => state)
}

export const emptyCustomer = () => ({ id: '', name: '', industry: '', status: '거래 중', bizNo: '', phone: '', address: '', homepage: '', memo: '' })
export const emptyContact = (customerId = '') => ({ id: '', customerId, name: '', title: '', dept: '', phone: '', email: '', memo: '', useYn: 'Y' })

const customerBody = (c) => ({
  customerName: c.name, industryCode: c.industry || null, dealStatusCode: c.status, bizNo: c.bizNo || null,
  phone: c.phone || null, address: c.address || null, homepage: c.homepage || null, memo: c.memo || null,
})

export async function saveCustomer(c) {
  if (c.dbId) await pmsCustomersApi.update(c.dbId, customerBody(c))
  else await pmsCustomersApi.create(customerBody(c))
  await loadCustomers()
}

export async function deleteCustomer(code) {
  const c = state.customers.find((x) => x.id === code)
  if (!c) return
  await pmsCustomersApi.remove(c.dbId)
  await loadCustomers()
}

export async function saveContact(t) {
  const customer = state.customers.find((c) => c.id === t.customerId)
  const body = {
    customerId: customer?.dbId, contactName: t.name, jobTitle: t.title || null, deptName: t.dept || null,
    phone: t.phone || null, email: t.email || null, memo: t.memo || null, useYn: t.useYn,
  }
  if (t.dbId) await pmsContactsApi.update(t.dbId, body)
  else await pmsContactsApi.create(body)
  await loadCustomers()
}

export async function deleteContact(code) {
  const t = state.contacts.find((x) => x.id === code)
  if (!t) return
  await pmsContactsApi.remove(t.dbId)
  await loadCustomers()
}

/** 화면의 고객사 코드/담당자 코드 → 서버 PK (프로젝트 저장용). */
export const customerDbId = (code) => state.customers.find((c) => c.id === code)?.dbId
export const contactDbId = (code) => state.contacts.find((t) => t.id === code)?.dbId
export const customerCodeOfDbId = (dbId) => state.customers.find((c) => c.dbId === dbId)?.id ?? ''
export const contactCodeOfDbId = (dbId) => state.contacts.find((t) => t.dbId === dbId)?.id ?? ''

/** 프로젝트에 저장된 발주사/담당자 이름으로 마스터를 찾는다(id 없는 기존 데이터용). */
export const customerOf = (customers, p) => customers.find((c) => c.id === p.customerId) ?? customers.find((c) => c.name === p.client)
export const contactOf = (contacts, p) =>
  contacts.find((t) => t.id === p.contactId) ?? contacts.find((t) => t.name === p.contactName && t.phone === p.contactPhone)