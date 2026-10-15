import './pms.css'
import {
  blockedBy,
  ME,
  PROJECT,
  TODAY,
  WBS_DEFAULT_PROJECT_ID,
  isLeaf,
  progressOf,
  projectProgress,
  setProgress,
  statusOf,
  useTasks,
} from './pmsStore'

const QUICK_STEPS = [0, 25, 50, 75, 100]

function TaskRow({ task, all }) {
  const p = progressOf(all, task)
  const status = statusOf(all, task)
  const blocker = blockedBy(all, task)
  const barClass = status === '지연' ? 'late' : p >= 100 ? 'done' : ''

  const onQuick = (s) => {
    const res = setProgress(task.id, s)
    if (!res.ok) window.alert(`선행 업무 "${res.pred.name}"이(가) 완료되어야 진행할 수 있습니다.`)
  }

  return (
    <div className="pms-task">
      <div className="pms-task-main">
        <div className="pms-task-title">{task.name}</div>
        <div className="pms-task-sub">
          <span className="pms-tag" style={{ marginRight: 6 }}>
            {task.phase}
          </span>
          {task.category} · {task.start} ~ {task.end}
          {task.end === TODAY && <span className="pms-tag late" style={{ marginLeft: 8 }}>오늘 마감</span>}
          {blocker && <span className="pms-tag late" style={{ marginLeft: 8 }}>선행 대기: {blocker.name}</span>}
          {task.copiedFromId && <span className="pms-tag copy" style={{ marginLeft: 8 }}>복사된 업무</span>}
        </div>
      </div>
      <div className={`pms-bar ${barClass}`}>
        <span style={{ width: `${p}%` }} />
      </div>
      <div className="pms-pct">{p}%</div>
      <div className="pms-quick" title="메신저 알림의 진척률 버튼과 동일한 입력 방식">
        {QUICK_STEPS.map((s) => (
          <button key={s} className={p === s ? 'on' : ''} disabled={!!blocker && s > 0} onClick={() => onQuick(s)}>
            {s}
          </button>
        ))}
      </div>
    </div>
  )
}

function Section({ title, tasks, all, empty }) {
  return (
    <div className="pms-panel">
      <h3>
        {title} <span className="count">{tasks.length}</span>
      </h3>
      {tasks.length === 0 ? (
        <p className="muted">{empty}</p>
      ) : (
        tasks.map((t) => <TaskRow key={t.id} task={t} all={all} />)
      )}
    </div>
  )
}

export default function MyDashboardPage() {
  const all = useTasks()
  const baseProgress = projectProgress(all.filter((t) => t.projectId === WBS_DEFAULT_PROJECT_ID))
  const mine = all.filter((t) => t.assignee === ME && isLeaf(all, t))
  const open = mine.filter((t) => progressOf(all, t) < 100)

  const late = open.filter((t) => statusOf(all, t) === '지연')
  const dueToday = open.filter((t) => t.end === TODAY)
  const doing = open.filter((t) => t.start <= TODAY && t.end >= TODAY)
  const upcoming = open.filter((t) => t.start > TODAY).sort((a, b) => a.start.localeCompare(b.start))
  const todayList = [...new Map([...dueToday, ...doing].map((t) => [t.id, t])).values()]

  const sample = todayList[0] ?? late[0]

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            내 업무 대시보드 <span className="pms-mock-badge">목업 · 더미 데이터</span>
          </h2>
          <p className="page-description">
            {ME} 님에게 할당된 업무입니다. 기준일 {TODAY}. 진척률 버튼을 눌러 보세요 — 메신저 알림에서
            누르는 것과 같은 방식이며 WBS 화면의 상위 진척률에 바로 반영됩니다.
          </p>
        </div>
      </div>

      <div className="pms-summary">
        <div className="pms-stat warn">
          <div className="label">오늘 마감</div>
          <div className="value">{dueToday.length}</div>
        </div>
        <div className="pms-stat danger">
          <div className="label">지연</div>
          <div className="value">{late.length}</div>
        </div>
        <div className="pms-stat">
          <div className="label">진행 중</div>
          <div className="value">{doing.length}</div>
        </div>
        <div className="pms-stat">
          <div className="label">예정</div>
          <div className="value">{upcoming.length}</div>
        </div>
        <div className="pms-stat ok">
          <div className="label">완료</div>
          <div className="value">{mine.length - open.length}</div>
        </div>
      </div>

      <div className="pms-grid-2">
        <div>
          <Section title="오늘 할 일" tasks={todayList} all={all} empty="오늘 처리할 업무가 없습니다." />
          <Section title="지연된 업무" tasks={late} all={all} empty="지연된 업무가 없습니다." />
          <Section title="예정 업무" tasks={upcoming} all={all} empty="예정된 업무가 없습니다." />
        </div>

        <div>
          <div className="pms-panel">
            <h3>
              참여 프로젝트 <span className="count">1</span>
            </h3>
            <div className="pms-project-card">
              <strong>{PROJECT.name}</strong>
              <div className="muted">
                {PROJECT.customer} · {PROJECT.period} · 단계: {PROJECT.stage}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 8 }}>
                <div className="pms-bar" style={{ flex: 1, width: 'auto' }}>
                  <span style={{ width: `${baseProgress}%` }} />
                </div>
                <span className="pms-pct">{baseProgress}%</span>
              </div>
            </div>
          </div>

          <div className="pms-panel">
            <h3>메신저 알림 미리보기</h3>
            <p className="muted" style={{ marginTop: 0 }}>
              아침 알림에서 바로 진척률을 입력합니다. 텔레그램 / Mattermost 공통.
            </p>
            {sample ? (
              <>
                <div className="pms-chat">
                  <div className="who">PMS 봇 · Telegram</div>
                  <div className="bubble">
                    📋 오늘의 업무 ({TODAY})
                    <br />▸ {sample.name} — {PROJECT.name}
                    <br />
                    　기한 {sample.end} · 현재 {progressOf(all, sample)}%
                    <div className="btn-row">
                      {QUICK_STEPS.map((s) => (
                        <span key={s}>{s}%</span>
                      ))}
                    </div>
                  </div>
                </div>
                <div className="pms-chat">
                  <div className="who">PMS 봇 · Mattermost</div>
                  <div className="bubble">
                    **오늘의 업무** ({TODAY})
                    <br />
                    {sample.name} / 기한 {sample.end} / 현재 {progressOf(all, sample)}%
                    <div className="btn-row">
                      {QUICK_STEPS.map((s) => (
                        <span key={s}>{s}%</span>
                      ))}
                      <span style={{ background: '#6b7280' }}>직접 입력…</span>
                    </div>
                  </div>
                </div>
              </>
            ) : (
              <p className="muted">표시할 업무가 없습니다.</p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
