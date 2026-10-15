/**
 * 고객사 / 고객사 담당자 관리 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 프로젝트마다 발주사·담당자를 텍스트로 입력하던 것을 자산(마스터)으로 관리한다.
 * 프로젝트 관리는 여기서 선택하며, 선택 시 이름·연락처를 프로젝트에 복사해 둔다(고객사 정보가 바뀌어도 과거 프로젝트 기록은 유지).
 */
import { useSyncExternalStore } from 'react'

export const INDUSTRIES = ['제조', '금융', '유통/물류', '공공', '의료/제약', '에너지', 'IT/서비스', '기타']
export const CUSTOMER_STATUSES = ['거래 중', '잠재', '거래 종료']

const C = (id, name, industry, status, phone, address, bizNo = '') => ({ id, name, industry, status, phone, address, bizNo, homepage: '', memo: '' })
const T = (id, customerId, name, title, dept, phone, email) => ({ id, customerId, name, title, dept, phone, email, memo: '', useYn: 'Y' })

let state = {
  customers: [
    C('CUS-001', '(샘플) 고객사', 'IT/서비스', '거래 중', '02-100-0000', '서울 강남구 테헤란로 1', '123-45-67890'),
    C('CUS-002', '한빛전자', '제조', '거래 중', '031-888-0000', '경기 수원시 영통구'),
    C('CUS-003', '대한물산', '유통/물류', '거래 중', '02-555-0000', '서울 중구 세종대로'),
    C('CUS-004', '새솔금융', '금융', '거래 중', '02-777-0000', '서울 영등포구 여의대로'),
    C('CUS-005', '푸른물류', '유통/물류', '잠재', '031-777-0000', '경기 평택시'),
    C('CUS-006', '미래제약', '의료/제약', '잠재', '043-200-0000', '충북 청주시'),
    C('CUS-007', '동서에너지', '에너지', '거래 중', '052-300-0000', '울산 남구'),
  ],
  contacts: [
    T('CTC-001', 'CUS-001', '한고객', '부장', 'IT기획팀', '010-1234-5678', 'client@example.com'),
    T('CTC-002', 'CUS-003', '박담당', '과장', '정보시스템팀', '02-555-0101', ''),
    T('CTC-003', 'CUS-004', '이팀장', '팀장', '디지털전략팀', '010-9876-5432', 'lee@saesol.example'),
    T('CTC-004', 'CUS-005', '정구매', '대리', '구매팀', '031-777-2020', ''),
    T('CTC-005', 'CUS-003', '송운영', '대리', '운영팀', '02-555-0202', ''),
    T('CTC-006', 'CUS-002', '유협업', '과장', '업무혁신팀', '031-888-0101', ''),
    T('CTC-007', 'CUS-007', '오책임', '책임', '스마트팩토리추진팀', '052-300-1100', ''),
  ],
}
const listeners = new Set()
const emit = (next) => {
  state = next
  listeners.forEach((l) => l())
}
const subscribe = (l) => {
  listeners.add(l)
  return () => listeners.delete(l)
}
export const useCustomerState = () => useSyncExternalStore(subscribe, () => state)

const nextId = (prefix, list) => `${prefix}-${String(list.reduce((m, x) => Math.max(m, Number(x.id.split('-')[1])), 0) + 1).padStart(3, '0')}`

export const emptyCustomer = () => ({ id: '', name: '', industry: '', status: '거래 중', bizNo: '', phone: '', address: '', homepage: '', memo: '' })
export const emptyContact = (customerId = '') => ({ id: '', customerId, name: '', title: '', dept: '', phone: '', email: '', memo: '', useYn: 'Y' })

export function saveCustomer(c) {
  const customers = c.id ? state.customers.map((x) => (x.id === c.id ? c : x)) : [...state.customers, { ...c, id: nextId('CUS', state.customers) }]
  emit({ ...state, customers })
}
export const deleteCustomer = (id) => emit({ customers: state.customers.filter((c) => c.id !== id), contacts: state.contacts.filter((t) => t.customerId !== id) })

export function saveContact(t) {
  const contacts = t.id ? state.contacts.map((x) => (x.id === t.id ? t : x)) : [...state.contacts, { ...t, id: nextId('CTC', state.contacts) }]
  emit({ ...state, contacts })
}
export const deleteContact = (id) => emit({ ...state, contacts: state.contacts.filter((t) => t.id !== id) })

/** 프로젝트에 저장된 발주사/담당자 이름으로 마스터를 찾는다(시드 데이터·id 없는 기존 프로젝트용). */
export const customerOf = (customers, p) => customers.find((c) => c.id === p.customerId) ?? customers.find((c) => c.name === p.client)
export const contactOf = (contacts, p) =>
  contacts.find((t) => t.id === p.contactId) ?? contacts.find((t) => t.name === p.contactName && t.phone === p.contactPhone)
