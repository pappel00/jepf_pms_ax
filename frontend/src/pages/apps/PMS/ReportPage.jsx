import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './pms.css'
import { SortTh, useSort } from './sortUtil'
import { DEFAULT_PROJECT_ID } from './docStore'
import { isHighPriority, isOpenStatus, useIssues } from './issueStore'
import { ME, TODAY, blockedBy, isLeaf, plannedProgress, projectProgress, statusOf, targetProgressOf, useTasks } from './pmsStore'
import { formatKoreanWon, useProjects } from './projectStore'
import { REPORT_STATUSES, cancelEdit, reopenReport, reportKey, reportOf, saveDraft, submitReport, useReports } from './reportStore'
import { ProcessButton } from './ProcessModal'
import { reportProcess } from './processModels'
import { mondayOf, saveSnapshot, useSnapshots, valueAt } from './snapshotStore'

// ---------- 날짜 유틸 ----------
const parse = (s) => new Date(`${s}T00:00:00Z`)
const iso = (d) => d.toISOString().slice(0, 10)
const addDays = (s, n) => {
  const d = parse(s)
  d.setUTCDate(d.getUTCDate() + n)
  return iso(d)
}
const md = (s) => `${Number(s.slice(5, 7))}/${Number(s.slice(8, 10))}`

/** 기준일(TODAY)이 속한 주/월 + offset 만큼 이동한 기간. */
function periodOf(mode, offset) {
  if (mode === 'week') {
    const d = parse(TODAY)
    const monday = addDays(TODAY, -((d.getUTCDay() + 6) % 7) + offset * 7)
    return { start: monday, end: addDays(monday, 6), label: mode }
  }
  const d = parse(TODAY)
  const first = new Date(Date.UTC(d.getUTCFullYear(), d.getUTCMonth() + offset, 1))
  const last = new Date(Date.UTC(first.getUTCFullYear(), first.getUTCMonth() + 1, 0))
  return { start: iso(first), end: iso(last), label: mode }
}
const nextPeriodOf = (mode, offset) => periodOf(mode, offset + 1)
const dday = (date) => {
  const diff = Math.round((parse(date) - parse(TODAY)) / 86400000)
  return diff === 0 ? 'D-Day' : diff > 0 ? `D-${diff}` : `D+${-diff}`
}
const overlaps = (t, p) => t.start <= p.end && t.end >= p.start
const inRange = (date, p) => !!date && date >= p.start && date <= p.end

/**
 * WBS 목업 데이터가 있는 프로젝트(통합업무포탈)는 WBS/이슈 스토어를 그대로 쓰고, 그 외 참여 프로젝트는
 * 이 보고서의 다중 프로젝트 레이아웃을 보여주기 위한 보조 더미 데이터를 쓴다.
 */
const EXTRA = {
  'PRJ-2026-007': {
    tasks: [
      { id: 'X7-1', name: '계약서 법무 검토·날인', phase: '착수 준비', category: '계약', assignee: ME, start: '2026-10-12', end: '2026-10-16', progress: 70 },
      { id: 'X7-2', name: '운영 환경 접속 권한 신청', phase: '착수 준비', category: '인수인계', assignee: '김개발', start: '2026-10-14', end: '2026-10-30', progress: 40 },
      { id: 'X7-3', name: '기존 운영 문서 인수 및 검토', phase: '착수 준비', category: '인수인계', assignee: '이설계', start: '2026-10-19', end: '2026-10-23', progress: 0 },
      { id: 'X7-4', name: '유지보수 체계·SLA 수립', phase: '착수 준비', category: '계획', assignee: ME, start: '2026-10-26', end: '2026-11-06', progress: 0 },
      { id: 'X7-5', name: '발주사 담당자 킥오프 미팅', phase: '착수 준비', category: '계획', assignee: ME, start: '2026-10-07', end: '2026-10-08', progress: 100 },
    ],
    issues: [
      { id: 'ISS-P7-01', title: '계약서 법무 검토 지연', priority: '높음', status: '진행 중', assignee: ME, dueDate: '2026-10-16', resolvedDate: '', registeredDate: '2026-10-08', taskName: '계약서 법무 검토·날인' },
      { id: 'ISS-P7-02', title: '운영 계정 인계 문서 누락', priority: '보통', status: '해결', assignee: '김개발', dueDate: '2026-10-14', resolvedDate: '2026-10-12', registeredDate: '2026-10-09', taskName: '운영 환경 접속 권한 신청' },
    ],
  },
}

const SEED_MEMO = { [`${DEFAULT_PROJECT_ID}|week`]: '• 발주사 요구사항 회신 지연으로 게시판 분석 약 5일 지연 예상 (ISS-001) — 10/16 회신 확정 요청 중.\n• 전자결재 연동 방식(배치/어댑터) 결정이 필요하여 개발 착수 판단 요청 (ISS-002).' }

/** 프로젝트 하나의 보고 데이터를 모은다(WBS 프로젝트 / 보조 더미 프로젝트 공통 형태). */
function collect(project, everyTask, allIssues) {
  const allTasks = everyTask.filter((t) => t.projectId === project.id)
  if (allTasks.length > 0) {
    const leaves = allTasks.filter((t) => isLeaf(allTasks, t))
    const roots = allTasks.filter((t) => !t.parentId)
    const rootW = roots.reduce((s, t) => s + t.weight, 0)
    const own = new Set(allTasks.map((t) => t.id))
    return {
      tasks: leaves.map((t) => ({ ...t, blocked: !!blockedBy(allTasks, t), late: statusOf(allTasks, t) === '지연' })),
      // 기본 프로젝트는 업무에 연결되지 않은(프로젝트 공통) 이슈도 포함한다.
      issues: allIssues
        .filter((i) => i.projectId === project.id)
        .map((i) => ({ ...i, taskName: i.taskIds.map((id) => allTasks.find((t) => t.id === id)?.name).filter(Boolean).join(', ') })),
      overall: projectProgress(allTasks),
      target: Math.round((roots.reduce((s, t) => s + targetProgressOf(allTasks, t) * t.weight, 0) / rootW) * 10) / 10,
      phases: roots.map((r) => ({
        name: r.name,
        progress: Math.round((allTasks.filter((t) => t.phase === r.name && isLeaf(allTasks, t)).reduce((s, t) => s + (t.progress || 0) * t.weight, 0) /
          (allTasks.filter((t) => t.phase === r.name && isLeaf(allTasks, t)).reduce((s, t) => s + t.weight, 0) || 1)) * 10) / 10,
        target: targetProgressOf(allTasks, r),
      })),
    }
  }
  const x = EXTRA[project.id]
  if (!x) return { tasks: [], issues: [], overall: 0, target: 0, phases: [] }
  const tasks = x.tasks.map((t) => ({ ...t, blocked: false, late: t.end < TODAY && t.progress < 100 }))
  const avg = (f) => Math.round((tasks.reduce((s, t) => s + f(t), 0) / (tasks.length || 1)) * 10) / 10
  return { tasks, issues: x.issues, overall: avg((t) => t.progress), target: avg((t) => plannedProgress(t.start, t.end)), phases: [] }
}

function Bar({ value, late }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      <div className={`pms-bar ${late ? 'late' : value >= 100 ? 'done' : ''}`} style={{ width: 90 }}>
        <span style={{ width: `${value}%` }} />
      </div>
      <span className="pms-pct">{value}%</span>
    </div>
  )
}

const PRIORITY_CLASS = { 긴급: 'late', 높음: 'manual', 보통: '', 낮음: '' }
const STATUS_CLASS = { 오픈: 'late', '진행 중': 'doing', 해결: 'done', 종결: '' }

/** 진척 추이(스냅샷 + 현재) — 실적(파랑 실선)과 목표(회색 점선). */
function TrendChart({ points }) {
  const W = 520
  const H = 120
  const pad = 24
  if (points.length < 2) return <p className="muted">스냅샷이 2개 이상 쌓이면 추이가 표시됩니다.</p>
  const x = (i) => pad + (i * (W - pad * 2)) / (points.length - 1)
  const y = (v) => H - pad - (v / 100) * (H - pad * 2)
  const line = (key) => points.map((p, i) => `${x(i)},${y(p[key])}`).join(' ')
  return (
    <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', maxWidth: W, height: 'auto' }} role="img" aria-label="진척 추이">
      {[0, 50, 100].map((v) => (
        <g key={v}>
          <line x1={pad} x2={W - pad} y1={y(v)} y2={y(v)} stroke="#e3e7ee" />
          <text x={2} y={y(v) + 4} fontSize="10" fill="#7a8599">{v}%</text>
        </g>
      ))}
      <polyline points={line('target')} fill="none" stroke="#9aa5b8" strokeDasharray="4 3" strokeWidth="2" />
      <polyline points={line('overall')} fill="none" stroke="#2f6fdb" strokeWidth="2.5" />
      {points.map((p, i) => (
        <g key={p.date}>
          <circle cx={x(i)} cy={y(p.overall)} r="3.5" fill="#2f6fdb" />
          <text x={x(i)} y={H - 6} fontSize="10" textAnchor="middle" fill="#7a8599">{md(p.date)}</text>
        </g>
      ))}
    </svg>
  )
}

function ProjectReport({ project, data, mode, period, next, mineOnly, memoKey }) {
  const navigate = useNavigate()
  const snaps = useSnapshots()[project.id] ?? []
  const live = { overall: data.overall, target: data.target }
  // 기간 증감 = 기간 끝 시점 진척 − 기간 시작 직전 진척 (기준일 이후 기간은 계산하지 않는다)
  const future = period.start > TODAY
  const endVal = future ? null : valueAt(snaps, period.end, TODAY, live)
  const startVal = future ? null : valueAt(snaps, addDays(period.start, -1), TODAY, live)
  const gain = endVal && startVal ? Math.round((endVal.overall - startVal.overall) * 10) / 10 : null
  const planGain = endVal && startVal ? Math.round((endVal.target - startVal.target) * 10) / 10 : null
  const trend = [...snaps, ...(snaps.some((s) => s.date === mondayOf(TODAY)) ? [] : [{ date: TODAY, ...live }])]
  const savedThisWeek = snaps.some((s) => s.date === mondayOf(TODAY))
  const reports = useReports()
  const rkey = reportKey(project.id, mode, period.start)
  const seedMemo = SEED_MEMO[memoKey] ?? ''
  const report = reportOf(reports, rkey, seedMemo)
  const memo = report.memo
  const locked = report.status === '제출 완료'
  const [showHistory, setShowHistory] = useState(false)
  const unit = mode === 'week' ? '금주' : '당월'
  const nextUnit = mode === 'week' ? '다음 주' : '다음 달'
  const who = (t) => !mineOnly || t.assignee === ME

  const tasks = data.tasks.filter(who)
  const doing = tasks
    .filter((t) => t.progress < 100 && t.start <= period.end)
    .sort((a, b) => Number(b.late) - Number(a.late) || a.end.localeCompare(b.end))
  const { sort, onSort, sorted: doingSorted } = useSort(doing, {
    name: (t) => t.name,
    assignee: (t) => t.assignee,
    end: (t) => t.end,
    progress: (t) => t.progress,
  })
  const doneThis = tasks.filter((t) => t.progress >= 100 && inRange(t.end, period))
  const upcoming = tasks
    .filter((t) => t.progress < 100 && overlaps(t, next))
    .sort((a, b) => a.end.localeCompare(b.end))
  const lateCount = data.tasks.filter((t) => t.late && t.progress < 100).length

  const issues = data.issues.filter((i) => !mineOnly || i.assignee === ME)
  const solved = issues.filter((i) => !isOpenStatus(i.status) && inRange(i.resolvedDate, period))
  const ongoing = issues.filter((i) => isOpenStatus(i.status))
  const newIssues = data.issues.filter((i) => inRange(i.registeredDate, period)).length
  const diff = Math.round((data.overall - data.target) * 10) / 10

  const copyText = () => {
    const lines = [
      `[${mode === 'week' ? '주간' : '월간'} 보고] ${project.name} (${md(period.start)}~${md(period.end)})`,
      `전체 진척 ${data.overall}% (목표 ${data.target}%, ${diff >= 0 ? '+' : ''}${diff}%p)${gain !== null ? ` / ${unit} ${gain >= 0 ? '+' : ''}${gain}%p` : ''}`,
      '',
      `■ ${unit} 진행 업무`,
      ...doing.map((t) => `- ${t.name} (${t.assignee}) ${t.progress}% / 기한 ${md(t.end)}${t.late ? ' [지연]' : ''}`),
      '',
      `■ ${nextUnit} 업무`,
      ...upcoming.map((t) => `- ${t.name} (${t.assignee}) 기한 ${md(t.end)}`),
      '',
      `■ ${unit} 해결 이슈`,
      ...solved.map((i) => `- ${i.id} ${i.title}`),
      '',
      `■ 진행 중 이슈`,
      ...ongoing.map((i) => `- ${i.id} [${i.priority}] ${i.title} (${i.assignee}, 기한 ${i.dueDate ? md(i.dueDate) : '-'})`),
      ...(memo.trim() ? ['', '■ 특이사항', memo.trim()] : []),
    ]
    navigator.clipboard
      ?.writeText(lines.join('\n'))
      .then(() => window.alert('보고 내용을 복사했습니다. 메일·메신저에 붙여넣을 수 있습니다.'))
      .catch(() => window.alert(lines.join('\n')))
  }

  return (
    <div className="pms-panel" style={{ marginBottom: 22 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <h3 style={{ fontSize: 16, marginBottom: 4 }}>
            {project.name} <span className="pms-tag doing">{project.status}</span>
          </h3>
          <div className="muted">
            {project.id} · {project.client} · {project.period ?? `${project.start} ~ ${project.end}`} · 발주금액 {formatKoreanWon(project.amount)}
          </div>
        </div>
        <button className="secondary small" onClick={copyText}>
          📋 보고 내용 복사
        </button>
      </div>

      <div className="pms-summary" style={{ marginTop: 14 }}>
        <div className="pms-stat">
          <div className="label">전체 진척 (실적)</div>
          <div className="value">{data.overall}%</div>
          <div className="muted">목표 {data.target}%</div>
        </div>
        <div className={`pms-stat ${diff < 0 ? 'danger' : 'ok'}`}>
          <div className="label">계획 대비</div>
          <div className="value">
            {diff >= 0 ? '+' : ''}
            {diff}%p
          </div>
        </div>
        <div className={`pms-stat ${gain === null ? '' : gain > 0 ? 'ok' : 'warn'}`}>
          <div className="label">{unit} 진척 증감</div>
          <div className="value">{gain === null ? '-' : `${gain >= 0 ? '+' : ''}${gain}%p`}</div>
          <div className="muted">{gain === null ? (future ? '예정 기간' : '스냅샷 없음') : `계획 ${planGain >= 0 ? '+' : ''}${planGain}%p`}</div>
        </div>
        <div className="pms-stat ok">
          <div className="label">{unit} 완료 업무</div>
          <div className="value">{doneThis.length}</div>
        </div>
        <div className={`pms-stat ${lateCount ? 'danger' : ''}`}>
          <div className="label">지연 업무</div>
          <div className="value">{lateCount}</div>
        </div>
        <div className="pms-stat warn">
          <div className="label">미해결 이슈</div>
          <div className="value">{data.issues.filter((i) => isOpenStatus(i.status)).length}</div>
          <div className="muted">긴급·높음 {data.issues.filter((i) => isOpenStatus(i.status) && isHighPriority(i.priority)).length}건</div>
        </div>
        <div className="pms-stat">
          <div className="label">{unit} 신규 이슈</div>
          <div className="value">{newIssues}</div>
        </div>
      </div>

      <div className="pms-hint" style={{ boxShadow: 'none', background: '#fafbfd' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
          <b>진척 추이 <span className="muted" style={{ fontWeight: 400 }}>(주간 스냅샷 · 파랑 실적 / 점선 목표)</span></b>
          <button
            className="secondary small"
            onClick={() => {
              if (savedThisWeek && !window.confirm('이번 주 스냅샷이 이미 있습니다. 현재 값으로 덮어쓸까요?')) return
              saveSnapshot(project.id, TODAY, data.overall, data.target)
            }}
          >
            📸 {savedThisWeek ? '이번 주 스냅샷 다시 저장' : '현재 진척 스냅샷 저장'}
          </button>
        </div>
        <TrendChart points={trend} />
        <div className="pms-field-hint">매주 월요일 기준으로 저장합니다(자동 저장은 백엔드 구현 시 스케줄러로 대체). 마지막 점은 저장 전 현재 값입니다.</div>
      </div>

      {mode === 'month' && data.phases.length > 0 && (
        <div className="pms-hint" style={{ boxShadow: 'none', background: '#fafbfd' }}>
          <b>단계별 진척 (실적 / 목표)</b>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: 8, marginTop: 8 }}>
            {data.phases.map((p) => (
              <div key={p.name} style={{ fontSize: 12 }}>
                {p.name} <span className="muted">(목표 {p.target}%)</span>
                <Bar value={p.progress} late={p.progress + 5 < p.target} />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 좌: 현재 진행 중인 업무 / 우: 다음 주(달) 업무 */}
      <div className="pms-grid-2" style={{ marginTop: 14, gridTemplateColumns: 'minmax(0, 1.25fr) minmax(0, 1fr)' }}>
        <div>
          <h3 style={{ fontSize: 14 }}>
            현재 진행 중인 업무 <span className="count">{doing.length}</span>
          </h3>
          <table className="data-table pms-wbs">
            <thead>
              <tr>
                <SortTh k="name" sort={sort} onSort={onSort}>업무</SortTh>
                <SortTh k="assignee" sort={sort} onSort={onSort}>담당</SortTh>
                <SortTh k="end" sort={sort} onSort={onSort}>기한</SortTh>
                <SortTh k="progress" sort={sort} onSort={onSort}>진척</SortTh>
              </tr>
            </thead>
            <tbody>
              {doing.length === 0 && (
                <tr>
                  <td className="empty-cell" colSpan={4}>
                    진행 중인 업무가 없습니다.
                  </td>
                </tr>
              )}
              {doingSorted.map((t) => (
                <tr key={t.id}>
                  <td className="name-cell">
                    <b>{t.name}</b>{' '}
                    {t.late && <span className="pms-tag late">지연</span>}
                    {t.blocked && <span className="pms-tag late">선행 대기</span>}
                    <div className="muted">
                      {t.phase} · {t.category}
                    </div>
                  </td>
                  <td>{t.assignee || <span className="muted">미배정</span>}</td>
                  <td>
                    {md(t.end)} <span className={`pms-tag ${t.end < TODAY ? 'late' : ''}`}>{dday(t.end)}</span>
                  </td>
                  <td>
                    <Bar value={t.progress} late={t.late} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div>
          <h3 style={{ fontSize: 14 }}>
            {nextUnit} 업무 <span className="muted">({md(next.start)} ~ {md(next.end)})</span> <span className="count">{upcoming.length}</span>
          </h3>
          <table className="data-table pms-wbs">
            <thead>
              <tr>
                <th>업무</th>
                <th>담당</th>
                <th>기한</th>
              </tr>
            </thead>
            <tbody>
              {upcoming.length === 0 && (
                <tr>
                  <td className="empty-cell" colSpan={3}>
                    {nextUnit}에 예정된 업무가 없습니다.
                  </td>
                </tr>
              )}
              {upcoming.map((t) => (
                <tr key={t.id}>
                  <td className="name-cell">
                    <b>{t.name}</b>{' '}
                    <span className={`pms-tag ${t.start >= next.start ? 'doing' : ''}`}>{t.start >= next.start ? '신규 착수' : '계속'}</span>
                    <div className="muted">
                      {t.phase} · {t.category}
                    </div>
                  </td>
                  <td>{t.assignee || <span className="muted">미배정</span>}</td>
                  <td>
                    <b>{md(t.end)}</b> <span className="pms-tag">{dday(t.end)}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {doneThis.length > 0 && (
        <div className="pms-hint" style={{ boxShadow: 'none', background: '#f1faf3', marginTop: 10 }}>
          <b>{unit} 완료 업무 ({doneThis.length}건)</b> — {doneThis.map((t) => `${t.name}(${t.assignee})`).join(', ')}
        </div>
      )}

      {/* 하단: 해결한 이슈 / 진행 중인 이슈 */}
      <div className="pms-grid-2" style={{ marginTop: 14, gridTemplateColumns: '1fr 1.4fr' }}>
        <div>
          <h3 style={{ fontSize: 14 }}>
            {unit} 해결한 이슈 <span className="count">{solved.length}</span>
          </h3>
          <table className="data-table pms-wbs">
            <thead>
              <tr>
                <th>이슈</th>
                <th>담당</th>
                <th>해결일</th>
              </tr>
            </thead>
            <tbody>
              {solved.length === 0 && (
                <tr>
                  <td className="empty-cell" colSpan={3}>
                    {unit} 해결한 이슈가 없습니다.
                  </td>
                </tr>
              )}
              {solved.map((i) => (
                <tr key={i.id}>
                  <td className="name-cell">
                    <b>{i.id}</b> {i.title}
                    {i.taskName && <div className="muted">업무: {i.taskName}</div>}
                    {i.resolution && <div className="muted">조치: {i.resolution}</div>}
                  </td>
                  <td>{i.assignee}</td>
                  <td>{md(i.resolvedDate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <div>
          <h3 style={{ fontSize: 14 }}>
            현재 진행 중인 이슈 <span className="count">{ongoing.length}</span>
          </h3>
          <table className="data-table pms-wbs">
            <thead>
              <tr>
                <th>이슈</th>
                <th>우선순위</th>
                <th>상태</th>
                <th>담당</th>
                <th>조치 기한</th>
              </tr>
            </thead>
            <tbody>
              {ongoing.length === 0 && (
                <tr>
                  <td className="empty-cell" colSpan={5}>
                    진행 중인 이슈가 없습니다.
                  </td>
                </tr>
              )}
              {ongoing.map((i) => (
                <tr key={i.id}>
                  <td className="name-cell">
                    <b>{i.id}</b> {i.title}
                    {i.taskName && <div className="muted">업무: {i.taskName}</div>}
                  </td>
                  <td>
                    <span className={`pms-tag ${PRIORITY_CLASS[i.priority]}`}>{i.priority}</span>
                  </td>
                  <td>
                    <span className={`pms-tag ${STATUS_CLASS[i.status]}`}>{i.status}</span>
                  </td>
                  <td>{i.assignee}</td>
                  <td>
                    {i.dueDate ? (
                      <>
                        {md(i.dueDate)} <span className={`pms-tag ${i.dueDate < TODAY ? 'late' : ''}`}>{dday(i.dueDate)}</span>
                      </>
                    ) : (
                      '-'
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {project.id === DEFAULT_PROJECT_ID && (
            <button className="link-btn" onClick={() => navigate('/workspace/PMS/issues')}>
              이슈 관리에서 전체 보기 ›
            </button>
          )}
        </div>
      </div>

      <div className="field" style={{ marginTop: 14 }}>
        <label>
          특이사항 · 요청사항<span className="opt-mark"> (옵션, 보고서에 포함)</span>
        </label>
        <textarea
          rows={3}
          value={memo}
          disabled={locked}
          onChange={(e) => saveDraft(rkey, seedMemo, e.target.value, ME, TODAY)}
          placeholder="발주사 협조 요청, 리스크, 의사결정 필요 사항 등"
        />
      </div>

      <div className="pms-hint" style={{ boxShadow: 'none', background: '#fafbfd', marginTop: 10 }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <b>보고서 상태</b>
          <span className={`pms-tag ${report.status === '제출 완료' ? 'done' : report.status === '수정 중' ? 'manual' : 'doing'}`}>{report.status}</span>
          {report.version > 0 && <span className="muted">제출본 v{report.version} · {report.submitted?.date} {report.submitted?.author}</span>}
          <div style={{ marginLeft: 'auto', display: 'flex', gap: 6 }}>
            {!locked && (
              <button
                className="primary small"
                onClick={() => {
                  const re = report.version > 0
                  if (!window.confirm(`${re ? '재제출' : '제출'}할까요? 현재 진척(${data.overall}%)과 특이사항이 제출본으로 저장됩니다.`)) return
                  submitReport(rkey, seedMemo, { overall: data.overall, target: data.target }, ME, TODAY)
                }}
              >
                {report.version > 0 ? '재제출' : '제출'}
              </button>
            )}
            {!locked && report.status === '수정 중' && (
              <button className="secondary small" onClick={() => cancelEdit(rkey, seedMemo, ME, TODAY)}>
                수정 취소
              </button>
            )}
            {locked && (
              <button
                className="secondary small"
                onClick={() => {
                  const reason = window.prompt('수정 사유를 입력하세요 (필수)')
                  if (reason && reason.trim()) reopenReport(rkey, seedMemo, reason.trim(), ME, TODAY)
                }}
              >
                수정
              </button>
            )}
            <ProcessButton
              getModel={() =>
                reportProcess(report, {
                  projectName: project.name,
                  label: `${mode === 'week' ? '주간' : '월간'} ${md(period.start)}~${md(period.end)}`,
                  owner: project.pmName || ME,
                })
              }
            />
            <button className="link-btn" onClick={() => setShowHistory((v) => !v)}>
              이력 {report.history.length}건 {showHistory ? '▲' : '▼'}
            </button>
          </div>
        </div>
        <div className="pms-field-hint">작성 중 → 제출 → (수정 사유 입력) 수정 중 → 재제출 순서로 진행합니다. 제출 후에는 수정 시작 전까지 특이사항을 바꿀 수 없습니다.</div>
        {showHistory && (
          <div style={{ marginTop: 6 }}>
            {report.history.length === 0 && <div className="muted">이력이 없습니다.</div>}
            {[...report.history].reverse().map((h, i) => (
              <div key={i} style={{ display: 'flex', gap: 12, fontSize: 12, padding: '3px 0' }}>
                <span style={{ width: 80, color: '#888' }}>{h.date}</span>
                <span style={{ width: 54, fontWeight: 600 }}>{h.author}</span>
                <span className="pms-tag">{h.action}</span>
                <span>{h.text}</span>
              </div>
            ))}
          </div>
        )}
      </div>    </div>
  )
}

/**
 * 주간/월간 보고 — 로그인 사용자가 참여한 프로젝트별로 업무·이슈 데이터를 모아 보고서 형태로 보여준다.
 *  좌: 현재 진행 중인 업무(진척률) / 우: 다음 주(달) 업무(기한) / 하단: 해당 기간에 해결한 이슈 + 진행 중인 이슈.
 * 업무별 진척률은 조회 시점(현재) 기준이고, 전체 진척의 기간별 증감·추이는 주간 스냅샷(snapshotStore)으로 계산한다.
 */
export default function ReportPage({ mode }) {
  const allTasks = useTasks()
  const allIssues = useIssues()
  const projects = useProjects()
  const [offset, setOffset] = useState(0)
  const [mineOnly, setMineOnly] = useState(false)
  const [projectFilter, setProjectFilter] = useState('')

  // 참여 프로젝트: 수주/수행 중이면서 내가 영업 담당·수행 PM이거나 내 업무가 있는 프로젝트
  const hasMyTask = (p) =>
    allTasks.some((t) => t.projectId === p.id && t.assignee === ME) || (EXTRA[p.id]?.tasks ?? []).some((t) => t.assignee === ME)
  const hasData = (p) => allTasks.some((t) => t.projectId === p.id) || !!EXTRA[p.id] // 단계전환 전(수주)에는 업무 데이터가 없다
  const mine = projects.filter(
    (p) => ['수주', '수행 중'].includes(p.status) && hasData(p) && (p.salesName === ME || p.pmName === ME || hasMyTask(p)),
  )
  const shown = mine.filter((p) => !projectFilter || p.id === projectFilter)

  const period = periodOf(mode, offset)
  const next = nextPeriodOf(mode, offset)
  const title = mode === 'week' ? '주간 보고' : '월간 보고'
  const nowLabel = mode === 'week' ? '이번 주' : '이번 달'
  const rangeLabel =
    mode === 'week'
      ? `${period.start} ~ ${period.end}`
      : `${period.start.slice(0, 4)}년 ${Number(period.start.slice(5, 7))}월`

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            {title}
          </h2>
          <p className="page-description">
            {ME} 님이 참여한 프로젝트별 보고서입니다. 업무·이슈 데이터를 자동으로 모아 보여주며, 좌측은 현재 진행 중인 업무와 진척률, 우측은{' '}
            {mode === 'week' ? '다음 주' : '다음 달'} 예정 업무와 기한, 하단은 {mode === 'week' ? '금주' : '당월'} 해결한 이슈와 진행 중인 이슈입니다.
          </p>
        </div>
      </div>

      <div className="pms-toolbar">
        <button className="secondary small" onClick={() => setOffset(offset - 1)}>
          ◀ 이전
        </button>
        <b style={{ minWidth: 190, textAlign: 'center' }}>
          {rangeLabel} {offset === 0 && <span className="pms-tag doing">{nowLabel}</span>}
        </b>
        <button className="secondary small" onClick={() => setOffset(offset + 1)}>
          다음 ▶
        </button>
        {offset !== 0 && (
          <button className="link-btn" onClick={() => setOffset(0)}>
            {nowLabel}로
          </button>
        )}
        <label style={{ fontSize: 13, display: 'flex', gap: 6, alignItems: 'center', marginLeft: 12 }}>
          <input type="checkbox" checked={mineOnly} onChange={(e) => setMineOnly(e.target.checked)} />내 업무·이슈만
        </label>
        <div style={{ marginLeft: 'auto', display: 'flex', gap: 6 }}>
          <button className="secondary small" onClick={() => window.alert('목업: 엑셀 내보내기')}>
            엑셀 ⬇
          </button>
          <button className="secondary small" onClick={() => window.alert('목업: PDF 내보내기 / 인쇄')}>
            PDF ⬇
          </button>
        </div>
      </div>

      <div className="pms-chips">
        <button className={!projectFilter ? 'on' : ''} onClick={() => setProjectFilter('')}>
          참여 프로젝트 전체 <span>{mine.length}</span>
        </button>
        {mine.map((p) => (
          <button key={p.id} className={projectFilter === p.id ? 'on' : ''} onClick={() => setProjectFilter(p.id)}>
            {p.name}
          </button>
        ))}
      </div>

      {offset !== 0 && <div className="pms-blind">ℹ️ 업무별 진척률은 조회 시점(현재) 기준입니다. 전체 진척의 기간별 증감은 주간 스냅샷으로 계산합니다(스냅샷이 없는 기간은 표시되지 않음).</div>}

      {shown.length === 0 && <p className="muted">참여 중인 프로젝트가 없습니다.</p>}
      {shown.map((p) => (
        <ProjectReport
          key={`${p.id}-${mode}`}
          project={p}
          data={collect(p, allTasks, allIssues)}
          mode={mode}
          period={period}
          next={next}
          mineOnly={mineOnly}
          memoKey={`${p.id}|${mode}`}
        />
      ))}
    </div>
  )
}
