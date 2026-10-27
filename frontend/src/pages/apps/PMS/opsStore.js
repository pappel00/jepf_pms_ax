/**
 * 운영 관리(운영 업무 · 운영 이슈 · 운영 영업) 목업 스토어 — 더미 데이터, API 연동 없음(새로고침하면 초기화).
 *
 * 운영 중인 프로젝트(운영단계전환을 마친 프로젝트)별로 운영 업무, 장애·문의 등 운영 이슈, 운영 중 발생하는 영업 기회를 관리한다.
 * 운영 이슈의 SLA 시한은 프로젝트의 운영 정보(SLA 응답·해결 목표 시간)에서 계산한다.
 */
import { useSyncExternalStore } from 'react'
import { useProjects } from './projectStore'

export const NOW = '2026-10-13 10:00' // 목업 기준 현재 시각(pmsStore.TODAY와 같은 날)

/* ---------- 공통 ---------- */
export const useOpsProjects = () => useProjects().filter((p) => p.status === '운영 중' && p.operation)

export const opsMembers = (p) => [...new Set([p?.operation?.manager?.name, ...(p?.operation?.team ?? []).map((m) => m.name)].filter(Boolean))]

const make = (initial) => {
  let state = initial
  const listeners = new Set()
  return {
    get: () => state,
    set: (next) => {
      state = next
      listeners.forEach((l) => l())
    },
    use: () =>
      useSyncExternalStore(
        (l) => {
          listeners.add(l)
          return () => listeners.delete(l)
        },
        () => state,
      ),
  }
}
const nextId = (prefix, list) => `${prefix}-${String(list.reduce((m, x) => Math.max(m, Number(x.id.split('-')[1])), 0) + 1).padStart(3, '0')}`

/* ---------- 운영 업무 ---------- */
export const TASK_CATEGORIES = ['정기 점검', '패치·배포', '장애 조치', '문의 대응', '요청 작업', '보고', '기타']
export const TASK_STATUSES = ['대기', '진행 중', '완료', '보류']
export const REPEATS = ['없음', '매주', '매월', '분기']
export const PRIORITIES = ['긴급', '높음', '보통', '낮음']

const T = (id, projectId, title, category, assignee, due, status, priority, repeat, memo = '', incidentId = '') => ({ id, projectId, title, category, assignee, due, status, priority, repeat, memo, incidentId })
const tasks = make([
  T('OT-001', 'PRJ-2026-011', '10월 정기 점검 (서버·DB·백업)', '정기 점검', '김개발', '2026-10-16', '진행 중', '보통', '매월', '점검 결과서 작성 후 고객 담당자 공유'),
  T('OT-002', 'PRJ-2026-011', '보안 패치 적용 (4분기)', '패치·배포', '이설계', '2026-10-24', '대기', '높음', '분기', '개발 서버 → 운영 서버 순서, 야간 작업'),
  T('OT-003', 'PRJ-2026-011', '결재 지연 장애 조치 및 원인 보고', '장애 조치', '김개발', '2026-10-12', '완료', '긴급', '없음', '', 'OI-001'),
  T('OT-004', 'PRJ-2026-011', '9월 운영 보고서 작성·제출', '보고', '관리자', '2026-10-08', '대기', '보통', '매월', '기한 경과'),
])
export const useOpsTasks = tasks.use
export const emptyTask = (projectId, assignee) => ({ id: '', projectId, title: '', category: '요청 작업', assignee, due: '', status: '대기', priority: '보통', repeat: '없음', repeatEnd: '', prevTaskId: '', autoCreated: false, memo: '', incidentId: '' })

const addDaysIso = (s, n) => new Date(new Date(`${s}T00:00:00Z`).getTime() + n * 86400000).toISOString().slice(0, 10)
/** 매월/분기는 같은 일자로 N개월 뒤(말일 초과 시 말일). */
function addMonthsIso(s, n) {
  const [y, m, d] = s.split('-').map(Number)
  const last = new Date(Date.UTC(y, m - 1 + n + 1, 0)).getUTCDate()
  return new Date(Date.UTC(y, m - 1 + n, Math.min(d, last))).toISOString().slice(0, 10)
}
/** 다음 주기 기한 — 매주 +7일, 매월 +1개월, 분기 +3개월. 반복이 아니면 null. */
export function nextDueOf(due, repeat) {
  if (!due) return null
  if (repeat === '매주') return addDaysIso(due, 7)
  if (repeat === '매월') return addMonthsIso(due, 1)
  if (repeat === '분기') return addMonthsIso(due, 3)
  return null
}

/**
 * 반복 업무가 완료되면 다음 주기 업무를 자동 생성한다(상태 대기, 기한 = 이전 기한 + 주기, 이전 업무 연결).
 * 반복 종료일·운영 종료일을 넘기거나 같은 기한의 업무가 이미 있으면 만들지 않는다.
 * 반환: { created } 또는 { skipped: 사유 } 또는 null(반복 아님/완료 아님).
 */
function spawnNext(task, list, opsEnd) {
  if (task.status !== '완료' || !task.repeat || task.repeat === '없음') return null
  const due = nextDueOf(task.due, task.repeat)
  if (!due) return null
  if (list.some((x) => x.prevTaskId === task.id)) return { skipped: '이미 다음 업무가 생성되었습니다.' }
  if (task.repeatEnd && due > task.repeatEnd) return { skipped: `반복 종료일(${task.repeatEnd})을 넘어 생성하지 않았습니다.` }
  if (opsEnd && due > opsEnd) return { skipped: `운영 종료일(${opsEnd})을 넘어 생성하지 않았습니다.` }
  if (list.some((x) => x.projectId === task.projectId && x.title === task.title && x.due === due)) return { skipped: `같은 기한(${due})의 동일 업무가 이미 있습니다.` }
  const created = { ...task, id: nextId('OT', list), due, status: '대기', prevTaskId: task.id, autoCreated: true, incidentId: '' }
  return { created }
}

/** 저장 — 완료로 저장하면 반복 업무의 다음 업무를 자동 생성한다. 결과(spawn)를 돌려준다. */
export function saveTask(t, opsEnd) {
  const list = tasks.get()
  const old = list.find((x) => x.id === t.id)
  const saved = t.id ? t : { ...t, id: nextId('OT', list) }
  let next = t.id ? list.map((x) => (x.id === t.id ? t : x)) : [...list, saved]
  const spawn = !old || old.status !== '완료' ? spawnNext(saved, next, opsEnd) : null
  if (spawn?.created) next = [...next, { ...spawn.created, id: nextId('OT', next) }]
  tasks.set(next)
  return spawn
}
export const deleteTask = (id) => tasks.set(tasks.get().filter((t) => t.id !== id))
export function setTaskStatus(id, status, opsEnd) {
  const list = tasks.get()
  const cur = list.find((t) => t.id === id)
  if (!cur) return null
  const updated = { ...cur, status }
  let next = list.map((t) => (t.id === id ? updated : t))
  const spawn = cur.status !== '완료' ? spawnNext(updated, next, opsEnd) : null
  if (spawn?.created) next = [...next, { ...spawn.created, id: nextId('OT', next) }]
  tasks.set(next)
  return spawn
}

/* ---------- 운영 이슈(장애·문의·개선 요청) ---------- */
export const INCIDENT_TYPES = ['장애', '문의', '개선 요청', '기타']
export const INCIDENT_STATUSES = ['접수', '분석', '조치 중', '해결', '종결']
export const isOpenIncident = (s) => s !== '해결' && s !== '종결'

const I = (id, projectId, type, title, priority, status, reporter, assignee, receivedAt, respondedAt, resolvedAt, content, action = '', opportunityId = '') => ({
  id, projectId, type, title, priority, status, reporter, assignee, receivedAt, respondedAt, resolvedAt, content, action, opportunityId,
})
const incidents = make([
  I('OI-001', 'PRJ-2026-011', '장애', '전자결재 결재선 조회 지연', '높음', '종결', '한운영', '김개발', '2026-10-09 14:20', '2026-10-09 14:50', '2026-10-10 11:00', '결재선 조회 시 10초 이상 지연', 'DB 인덱스 추가, 슬로우 쿼리 개선'),
  I('OI-002', 'PRJ-2026-011', '문의', '모바일 결재 알림 설정 방법 문의', '낮음', '해결', '한운영', '이설계', '2026-10-12 09:30', '2026-10-12 10:10', '2026-10-12 16:00', '알림 수신 설정 위치 문의', '매뉴얼 안내'),
  I('OI-003', 'PRJ-2026-011', '장애', '통합검색 결과 누락', '긴급', '조치 중', '한운영', '김개발', '2026-10-13 08:10', '2026-10-13 08:40', '', '신규 등록 문서가 검색되지 않음', '색인 재생성 중'),
  I('OI-004', 'PRJ-2026-011', '개선 요청', '결재 문서 일괄 다운로드 기능 요청', '보통', '접수', '한운영', '', '2026-10-13 09:15', '', '', '월말 결산 시 문서를 한 번에 내려받고 싶다는 요청 (유상 개선 대상 후보)'),
  I('OI-005', 'PRJ-2026-011', '문의', '신규 입사자 계정 발급 절차 문의', '보통', '분석', '송고객', '이설계', '2026-10-13 06:30', '', '', '권한 그룹 지정 방법 문의'),
])
export const useOpsIncidents = incidents.use
export const emptyIncident = (projectId, reporter) => ({ id: '', projectId, type: '문의', title: '', priority: '보통', status: '접수', reporter, assignee: '', receivedAt: NOW, respondedAt: '', resolvedAt: '', content: '', action: '', opportunityId: '' })
export function saveIncident(i) {
  const list = incidents.get()
  const old = list.find((x) => x.id === i.id)
  let next = { ...i }
  // 처리 상태에 맞춰 응답/해결 시각을 자동 기록(목업: 현재 시각)
  if (!next.respondedAt && next.status !== '접수') next.respondedAt = NOW
  if (!next.resolvedAt && !isOpenIncident(next.status)) next.resolvedAt = NOW
  if (old && isOpenIncident(next.status)) next.resolvedAt = ''
  incidents.set(next.id ? list.map((x) => (x.id === next.id ? next : x)) : [{ ...next, id: nextId('OI', list) }, ...list])
}
export const deleteIncident = (id) => incidents.set(incidents.get().filter((i) => i.id !== id))
export const linkOpportunity = (id, opportunityId) => incidents.set(incidents.get().map((i) => (i.id === id ? { ...i, opportunityId } : i)))

const ms = (s) => new Date(s.replace(' ', 'T') + ':00').getTime()
const addHours = (s, h) => ms(s) + h * 3600000
/**
 * SLA 판정 — 응답 시한(접수 + 응답 목표 시간), 해결 시한(접수 + 해결 목표 시간).
 * 반환: { respondDue, resolveDue, respond: '충족'|'위반'|'임박'|'정상', resolve: ... } (장애·문의만 SLA 대상)
 */
export function slaOf(incident, sla) {
  if (!sla || incident.type === '개선 요청' || incident.type === '기타') return null
  const now = ms(NOW)
  const judge = (due, doneAt) => {
    if (doneAt) return ms(doneAt) <= due ? '충족' : '위반'
    if (now > due) return '위반'
    return due - now <= 3600000 * 2 ? '임박' : '정상'
  }
  const respondDue = addHours(incident.receivedAt, sla.respond)
  const resolveDue = addHours(incident.receivedAt, sla.resolve)
  return {
    respondDue, resolveDue,
    respond: judge(respondDue, incident.respondedAt),
    resolve: judge(resolveDue, incident.resolvedAt),
  }
}
export const fmtDue = (t) => new Date(t + 9 * 3600000).toISOString().slice(5, 16).replace('T', ' ')

/* ---------- 운영 영업(재계약·추가 개발·유상 개선 등) ---------- */
export const OPP_KINDS = ['재계약·갱신', '추가 개발', '유상 개선', '증설·확장', '교육·컨설팅', '신규 제안']
export const OPP_STAGES = ['발굴', '제안', '협상', '수주', '실패/보류']
export const OPP_STAGE_PROB = { 발굴: 10, 제안: 30, 협상: 70, 수주: 100, '실패/보류': 0 }

const O = (id, projectId, kind, title, stage, amount, probability, owner, expectedDate, nextAction, nextDate, memo = '', sourceIncidentId = '') => ({
  id, projectId, kind, title, stage, amount, probability, owner, expectedDate, nextAction, nextDate, memo, sourceIncidentId, contactId: '', sourceNextId: '',
})
const opps = make([
  O('OS-001', 'PRJ-2026-011', '재계약·갱신', '2027년 유지보수 재계약', '발굴', 180000000, 10, '관리자', '2026-12-15', '재계약 조건 사전 협의 일정 잡기', '2026-10-20', '운영 종료일 임박 — 갱신 제안서 준비'),
  O('OS-002', 'PRJ-2026-011', '추가 개발', '모바일 결재 앱 고도화', '제안', 95000000, 30, '관리자', '2026-11-30', '제안서 초안 송부', '2026-10-22', '고객사 정보화 담당 부서장 관심'),
])
export const useOpsOpps = opps.use
export const emptyOpp = (projectId, owner) => ({ id: '', projectId, kind: '유상 개선', title: '', stage: '발굴', amount: null, probability: 10, owner, expectedDate: '', nextAction: '', nextDate: '', memo: '', sourceIncidentId: '', contactId: '', sourceNextId: '' })
export function saveOpp(o) {
  const list = opps.get()
  if (o.id) {
    opps.set(list.map((x) => (x.id === o.id ? o : x)))
    return o.id
  }
  const id = nextId('OS', list)
  opps.set([{ ...o, id }, ...list])
  return id
}
export const deleteOpp = (id) => opps.set(opps.get().filter((o) => o.id !== id))
