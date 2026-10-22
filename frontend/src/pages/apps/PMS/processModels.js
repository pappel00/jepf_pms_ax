/**
 * 프로세스 현황 모달(ProcessModal)에 넘길 model을 도메인별로 만든다 — 각 스토어의 데이터(상태·이력·담당자)를 읽기만 한다.
 */
import { PROPOSAL_STATUSES } from './proposalStore'

const APPROVER = '센터장·admin (승인자)'

/** 요청 → 승인 계열(단계전환 요청, 투입 계획 변경 요청)의 공통 모델. */
function approvalModel({ title, subtitle, req, approver, applyLabel }) {
  const pending = req.status === '승인 대기'
  const approved = req.status === '승인'
  const rejected = req.status === '반려'
  const canceled = req.status === '취소'
  const steps = [
    { label: '요청', state: 'done', who: req.requester, date: req.requestedAt },
    {
      label: '승인 심사',
      state: pending ? 'current' : approved ? 'done' : rejected ? 'rejected' : 'skipped',
      who: pending ? approver : req.decidedBy || '-',
      date: req.decidedAt || '',
      note: rejected ? '반려' : canceled ? '요청 취소' : '',
    },
    { label: applyLabel, state: approved ? 'done' : rejected || canceled ? 'skipped' : 'pending', who: approved ? '시스템' : '', date: approved ? req.decidedAt : '' },
  ]
  const history = [{ date: req.requestedAt, who: req.requester, action: '요청', text: req.reason ? `요청 등록 — 사유: ${req.reason}` : '요청 등록' }]
  if (!pending) {
    history.push({
      date: req.decidedAt,
      who: req.decidedBy || '-',
      action: req.status,
      text: [req.comment, approved ? `${applyLabel} 완료` : ''].filter(Boolean).join(' · ') || req.status,
    })
  }
  return {
    title,
    subtitle,
    steps,
    history,
    current: pending ? { label: '승인 심사', who: approver, hint: `요청자 ${req.requester} (${req.requestedAt}) — 승인 또는 반려가 필요합니다.` } : null,
    endNote: approved ? `승인되어 ${applyLabel}까지 끝났습니다.` : rejected ? '반려되어 종료되었습니다.' : '요청이 취소되었습니다.',
  }
}

export const transitionProcess = (req) =>
  approvalModel({
    title: `단계전환 처리 현황 — ${req.projectName}`,
    subtitle: `${req.id} · ${req.summary}`,
    req,
    approver: APPROVER,
    applyLabel: '수행 중 전환',
  })

export const changeProcess = (req) =>
  approvalModel({
    title: `투입 계획 변경 처리 현황 — ${req.projectName}`,
    subtitle: `${req.id} · ${(req.summary ?? []).join(' / ')}`,
    req,
    approver: '인력 관리자',
    applyLabel: '투입 계획 반영',
  })

/** 주간/월간 보고서: 작성 → 제출 → 수정 → 재제출. owner = 보고 작성 담당자(프로젝트 관리자). */
export function reportProcess(report, { projectName, label, owner }) {
  const hist = report.history
  const firstOf = (action) => hist.find((h) => h.action === action)
  const lastOf = (action) => [...hist].reverse().find((h) => h.action === action)
  const submits = hist.filter((h) => h.action === '제출')
  const status = report.status
  const writing = status === '작성 중' && report.version === 0
  const editing = status === '수정 중'
  const steps = [
    { label: '작성', state: writing ? 'current' : 'done', who: firstOf('작성')?.author ?? owner, date: firstOf('작성')?.date ?? '' },
    { label: '제출', state: report.version >= 1 ? 'done' : 'pending', who: submits[0]?.author ?? '', date: submits[0]?.date ?? '' },
    { label: '수정', state: editing ? 'current' : lastOf('수정') ? 'done' : 'pending', who: lastOf('수정')?.author ?? '', date: lastOf('수정')?.date ?? '', note: lastOf('수정') ? '선택' : '필요 시' },
    { label: '재제출', state: report.version >= 2 ? 'done' : 'pending', who: submits[1] ? submits[submits.length - 1].author : '', date: submits[1] ? submits[submits.length - 1].date : '' },
  ]
  const who = owner
  return {
    title: `보고서 처리 현황 — ${projectName}`,
    subtitle: `${label} · 현재 상태 ${status}${report.version ? ` · 제출본 v${report.version}` : ''}`,
    steps,
    history: hist.map((h) => ({ date: h.date, who: h.author, action: h.action, text: h.text })),
    current: writing
      ? { label: '작성', who, hint: '특이사항을 입력하고 제출하세요.' }
      : editing
        ? { label: '수정', who, hint: '수정 후 다시 제출(재제출)해야 반영됩니다.' }
        : null,
    endNote: '제출이 완료되었습니다. 수정이 필요하면 작성자가 수정 사유를 입력하고 수정을 시작합니다.',
  }
}

const ISSUE_FLOW = ['오픈', '진행 중', '해결', '종결']
const issueAction = (text) => (text.startsWith('상태 변경') ? '상태 변경' : text.startsWith('이슈 등록') ? '등록' : text.startsWith('업무 연결') ? '업무 연결' : '메모')

/** 이슈: 오픈 → 진행 중 → 해결 → 종결. 오픈·진행 중은 담당자, 해결은 보고자(종결 확인)에게 있다. */
export function issueProcess(issue) {
  const idx = ISSUE_FLOW.indexOf(issue.status)
  const dateOf = (status) => {
    if (status === '오픈') return issue.registeredDate
    const h = [...issue.history].reverse().find((x) => x.text.includes(`→ ${status}`))
    return h?.date ?? (status === '해결' ? issue.resolvedDate : '')
  }
  const holder = { 오픈: issue.assignee, '진행 중': issue.assignee, 해결: issue.reporter, 종결: '' }
  const steps = ISSUE_FLOW.map((s, i) => ({
    label: s,
    state: i < idx ? 'done' : i === idx ? (s === '종결' ? 'done' : 'current') : 'pending',
    who: i <= idx ? (s === '오픈' ? issue.reporter : s === '종결' ? issue.reporter : issue.assignee) : holder[s] || '',
    date: i <= idx ? dateOf(s) : '',
  }))
  const hints = { 오픈: '담당자가 착수(진행 중으로 변경)해야 합니다.', '진행 중': '담당자가 조치 후 해결로 변경해야 합니다.', 해결: '보고자가 조치 결과를 확인하고 종결해야 합니다.' }
  return {
    title: `이슈 처리 현황 — ${issue.id}`,
    subtitle: `${issue.title} · 우선순위 ${issue.priority} · 조치 기한 ${issue.dueDate || '-'}`,
    steps,
    history: issue.history.map((h) => ({ date: h.date, who: h.author, action: issueAction(h.text), text: h.text })),
    current: idx < 3 ? { label: issue.status, who: `${holder[issue.status]}${issue.status === '해결' ? ' (보고자)' : ' (담당자)'}`, hint: hints[issue.status] } : null,
    endNote: '종결된 이슈입니다.',
  }
}

/** 제안서: 작성 중 → 내부 검토 → 제출 완료 → 결과 확정. 버전 등록 이력을 지난 처리로 보여준다. */
export function proposalProcess(p) {
  const idx = PROPOSAL_STATUSES.indexOf(p.status)
  const holder = { '작성 중': p.owner, '내부 검토': '내부 검토자(미지정)', '제출 완료': '발주사 (결과 대기)', '결과 확정': '' }
  const steps = PROPOSAL_STATUSES.map((s, i) => ({
    label: s,
    state: i < idx ? 'done' : i === idx ? (s === '결과 확정' ? 'done' : 'current') : 'pending',
    who: i < idx ? p.owner : i === idx ? holder[s] : '',
    date: s === '제출 완료' && p.submittedDate ? p.submittedDate : '',
  }))
  const history = p.versions.map((v) => ({ date: v.date, who: v.author, action: '버전 등록', text: `${v.ver} — ${v.summary}` }))
  if (p.submittedDate) history.push({ date: p.submittedDate, who: p.owner, action: '제출', text: `발주사에 제출 (제출 마감 ${p.dueDate})` })
  history.sort((a, b) => a.date.localeCompare(b.date))
  const hints = { '작성 중': `제출 마감 ${p.dueDate}까지 작성을 마쳐야 합니다.`, '내부 검토': '검토 의견을 반영한 뒤 제출해야 합니다.', '제출 완료': '발주사의 평가·결과 통보를 기다리고 있습니다.' }
  return {
    title: `제안서 처리 현황 — ${p.id}`,
    subtitle: p.title,
    steps,
    history,
    current: idx < 3 ? { label: p.status, who: holder[p.status], hint: hints[p.status] } : null,
    endNote: '결과가 확정된 제안서입니다.',
  }
}
