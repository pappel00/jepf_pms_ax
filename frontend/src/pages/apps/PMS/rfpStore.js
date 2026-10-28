/**
 * RFP 관리 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 구조: 프로젝트 1 : N RFP(차수/재공고) → RFP 1 : N 항목(세분화된 요구사항) → 항목 1 : N 평가(평가자별).
 * 이 화면(RFP 관리)은 문서 접수와 항목 정리, 평가자 지정까지를 다루고, 항목별 평가(기능/비기능 구분,
 * 구현 여부, 예상 M/D)는 후속 「RFP 평가」 화면에서 한다 — 여기서는 평가 진행 현황(evalDone)만 보여준다.
 */
import { useSyncExternalStore } from 'react'
import { evalDoneOf, isSubmitted } from './evalStore'

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
]
export const categoryName = (code) => REQ_CATEGORIES.find((c) => c.code === code)?.name ?? code

export const ROUNDS = ['1차', '2차', '재공고', '변경 공고']

export const INTAKE_METHODS = [
  { value: 'excel', label: '엑셀 템플릿 업로드 (권장)', desc: '항목 양식에 맞춰 정리한 엑셀을 올리면 항목이 한 번에 등록됩니다.' },
  { value: 'manual', label: '수동 입력', desc: '원본 문서를 보면서 항목을 직접 하나씩 추가합니다.' },
  { value: 'auto', label: '문서 자동 분해 (추후 지원)', desc: '원본 문서에서 요구사항 표를 자동 추출합니다. 정확도 검증 후 도입 예정.', disabled: true },
]

const it = (category, area, name, content, page, mandatory = true, evalDone = 0) => ({ category, area, name, content, page, mandatory, evalDone })

const ECM_ITEMS = [
  it('SFR', '문서 관리', '문서 등록·수정·삭제', '사용자는 문서를 등록·수정·삭제할 수 있어야 하며 모든 변경은 이력으로 남아야 한다.', 12, true, 2),
  it('SFR', '문서 관리', '버전 관리', '문서의 버전을 자동 채번하고 이전 버전으로 복원할 수 있어야 한다.', 12, true, 2),
  it('SFR', '권한 관리', '조직·직급별 접근 권한', '폴더·문서 단위로 조직, 직급, 개인별 접근 권한을 설정할 수 있어야 한다.', 13, true, 2),
  it('SFR', '검색', '통합 검색 연계', '기존 통합검색 시스템과 연계하여 ECM 문서를 색인·검색할 수 있어야 한다.', 14, true, 1),
  it('SFR', '연계', '그룹웨어 문서 연계', '전자결재 첨부문서를 ECM으로 자동 이관할 수 있어야 한다.', 15, true, 1),
  it('SFR', '연계', 'DRM 연계', '문서 반출 시 DRM 암·복호화 모듈과 연계해야 한다.', 15, false, 0),
  it('PER', '응답 성능', '동시 접속 1,000명 처리', '동시 사용자 1,000명 기준으로 주요 화면 응답시간 3초 이내를 보장해야 한다.', 21, true, 1),
  it('PER', '응답 성능', '대용량 파일 업로드', '500MB 이하 파일을 안정적으로 업로드할 수 있어야 한다.', 21, false, 0),
  it('SER', '접근 통제', '접근 로그 및 감사', '문서 열람·다운로드 이력을 1년 이상 보관하고 조회할 수 있어야 한다.', 24, true, 2),
  it('SER', '암호화', '저장 데이터 암호화', '저장되는 문서는 국정원 검증 암호 모듈로 암호화해야 한다.', 24, true, 1),
  it('DAR', '이관', '기존 문서 마이그레이션', '기존 문서 약 120만 건을 이관하고 정합성을 검증해야 한다.', 28, true, 0),
  it('COR', '제약', '국산 소프트웨어 우선 적용', '핵심 솔루션은 GS 인증 국산 제품을 우선 적용한다.', 31, true, 2),
  it('PMR', '보고', '주간·월간 보고', '수행사는 주간 및 월간 진행 보고서를 제출해야 한다.', 35, true, 0),
  it('PSR', '교육·하자', '교육 및 하자보수', '사용자·관리자 교육을 실시하고 검수 후 1년간 무상 하자보수를 제공해야 한다.', 37, true, 0),
]

const MOBILE_ITEMS = [
  it('SFR', '모바일 앱', '통합 로그인(SSO)', '기존 SSO와 연계하여 1회 로그인으로 전 서비스를 이용할 수 있어야 한다.', 9, true, 3),
  it('SFR', '모바일 앱', '푸시 알림', '업무 알림을 푸시로 발송하고 수신 이력을 관리해야 한다.', 9, true, 3),
  it('SFR', '모바일 앱', '간편 인증', '생체 인증 및 FIDO 기반 간편 인증을 지원해야 한다.', 10, true, 2),
  it('SFR', '관리자', '앱 배포·버전 관리', '관리자 화면에서 앱 버전과 강제 업데이트 정책을 관리할 수 있어야 한다.', 11, false, 2),
  it('PER', '응답 성능', '앱 구동 시간', '앱 구동 후 메인 화면 노출까지 2초 이내여야 한다.', 14, true, 1),
  it('SER', '앱 보안', '위·변조 방지', '앱 위변조, 루팅 탐지, 소스 난독화를 적용해야 한다.', 16, true, 3),
  it('QUR', '품질', '단말·OS 호환성', 'Android 10 이상, iOS 15 이상 주요 단말에서 정상 동작해야 한다.', 18, true, 1),
  it('COR', '제약', '망 분리 환경 대응', '내부망/외부망 분리 환경에서 동작하도록 구성해야 한다.', 19, true, 0),
  it('PMR', '일정', '단계별 산출물 제출', '착수·중간·완료 단계별 산출물을 제출해야 한다.', 22, true, 0),
]

function withIds(rfpId, list) {
  const counters = {}
  return list.map((x, i) => {
    counters[x.category] = (counters[x.category] || 0) + 1
    return { ...x, id: `${rfpId}-I${String(i + 1).padStart(3, '0')}`, code: `${x.category}-${String(counters[x.category]).padStart(3, '0')}` }
  })
}

const seed = [
  {
    id: 'RFP-2026-001', projectId: 'PRJ-2026-003', title: '전사 문서중앙화(ECM) 고도화 제안요청서', round: '1차',
    receivedDate: '2026-10-01', dueDate: '2026-10-21', fileName: '대한물산_ECM고도화_RFP_v1.0.pdf', fileSize: '4.2MB',
    evaluators: ['이설계', '관리자'], items: withIds('RFP-2026-001', ECM_ITEMS),
  },
  {
    id: 'RFP-2026-002', projectId: 'PRJ-2026-004', title: '모바일 앱 통합 플랫폼 구축 제안요청서', round: '1차',
    receivedDate: '2026-09-02', dueDate: '2026-09-25', fileName: '새솔금융_모바일플랫폼_RFP.hwp', fileSize: '3.1MB',
    evaluators: ['이설계', '김개발', '관리자'], items: withIds('RFP-2026-002', MOBILE_ITEMS),
  },
  {
    id: 'RFP-2026-003', projectId: 'PRJ-2026-002', title: '차세대 인사시스템 구축 제안요청서', round: '1차',
    receivedDate: '2026-10-12', dueDate: '2026-10-30', fileName: '한빛전자_차세대인사_RFP.pdf', fileSize: '5.8MB',
    evaluators: [], items: [],
  },
]

let rfps = seed
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

export function useRfps() {
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

const patch = (id, fn) => emit(rfps.map((r) => (r.id === id ? fn(r) : r)))

export function saveRfp(rfp) {
  const exists = rfps.some((r) => r.id === rfp.id)
  emit(exists ? rfps.map((r) => (r.id === rfp.id ? rfp : r)) : [...rfps, rfp])
}

export function deleteRfp(id) {
  emit(rfps.filter((r) => r.id !== id))
}

export function setEvaluators(rfpId, names) {
  patch(rfpId, (r) => ({ ...r, evaluators: names }))
}

export function saveItem(rfpId, item) {
  patch(rfpId, (r) => {
    const exists = r.items.some((i) => i.id === item.id)
    return { ...r, items: exists ? r.items.map((i) => (i.id === item.id ? item : i)) : [...r.items, item] }
  })
}

export function deleteItem(rfpId, itemId) {
  patch(rfpId, (r) => ({ ...r, items: r.items.filter((i) => i.id !== itemId) }))
}

/** 목업: 엑셀 템플릿을 올렸다고 가정하고 샘플 항목을 만들어 붙인다. */
export function importSampleItems(rfpId) {
  patch(rfpId, (r) => {
    const samples = [
      ['SFR', '샘플 영역', '샘플 기능 항목 1', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 5, true],
      ['SFR', '샘플 영역', '샘플 기능 항목 2', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 5, true],
      ['SFR', '샘플 영역', '샘플 기능 항목 3', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 6, false],
      ['PER', '응답 성능', '샘플 성능 항목', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 8, true],
      ['SER', '접근 통제', '샘플 보안 항목', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 9, true],
      ['PMR', '보고', '샘플 관리 항목', '엑셀 템플릿에서 읽어온 요구사항 내용입니다.', 11, true],
    ]
    const items = [...r.items]
    samples.forEach(([category, area, name, content, page, mandatory], i) => {
      const draft = { ...r, items }
      items.push({
        id: `${r.id}-I${String(items.length + 1).padStart(3, '0')}-${i}`,
        category, area, name, content, page, mandatory, evalDone: 0,
        code: nextItemCode(draft, category),
      })
    })
    return { ...r, items }
  })
}
