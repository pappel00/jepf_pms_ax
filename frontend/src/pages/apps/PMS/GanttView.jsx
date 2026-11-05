import { useEffect, useMemo, useRef, useState } from 'react'
import './pms.css'
import { isLeaf, progressOf, rangeOf, statusOf } from './pmsStore'

/**
 * WBS 간트 — 왼쪽은 WBS 항목(전체 / 작업명만 / 숨김), 오른쪽은 일·주·월 단위 타임라인.
 * 행(rows)과 펼침 상태는 WBS 화면의 것을 그대로 받아 쓰므로 트리 보기와 같은 항목이 같은 순서로 보인다(오늘 업무 보기 포함).
 * 업무 막대 안의 채움은 실적 진척률, 상위 단계·분류는 하위 기간을 합친 요약 막대, 선행 업무는 화살표로 잇는다.
 */

const DAY = 86400000
const ROW_H = 30
const HEAD_H = 48
const pad = (n) => String(n).padStart(2, '0')
const toDay = (s) => {
  const [y, m, d] = s.split('-').map(Number)
  return Math.round(Date.UTC(y, m - 1, d) / DAY)
}
const dateOf = (n) => new Date(n * DAY)
const md = (n) => `${pad(dateOf(n).getUTCMonth() + 1)}/${pad(dateOf(n).getUTCDate())}`
const localToday = () => {
  const d = new Date()
  return toDay(`${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`)
}

const ZOOMS = { day: { label: '일', px: 26 }, week: { label: '주', px: 9 }, month: { label: '월', px: 3 } }
const STATUS_COLOR = { 완료: '#2f9e5b', 지연: '#d64545', 진행중: '#4468f0', 미착수: '#9aa5b8' }
// 왼쪽 항목 표시 모드별 열 너비.
const LEFT = {
  full: [
    ['no', 'WBS', 56],
    ['name', '작업', 230],
    ['assignee', '담당', 64],
    ['start', '시작', 56],
    ['end', '종료', 56],
    ['pct', '실적', 58],
  ],
  name: [
    ['no', 'WBS', 56],
    ['name', '작업', 230],
  ],
  none: [],
}

export default function GanttView({ all, rows, expanded, toggle, canEdit, onEdit }) {
  const [zoom, setZoom] = useState('week')
  const [leftMode, setLeftMode] = useState('full')
  const scroller = useRef(null)
  const px = ZOOMS[zoom].px
  const cols = LEFT[leftMode]
  const leftW = cols.reduce((s, c) => s + c[2], 0)
  const today = localToday()

  // 전체 업무의 기간으로 타임라인 범위를 잡는다(보이는 행만이 아니라 전체 — 펼치고 접어도 눈금이 흔들리지 않게).
  const range = useMemo(() => {
    const dated = all.filter((t) => isLeaf(all, t) && t.start && t.end)
    if (dated.length === 0) return null
    let s = Math.min(...dated.map((t) => toDay(t.start)), today)
    const e = Math.max(...dated.map((t) => toDay(t.end)), today)
    s -= 3
    s -= (dateOf(s).getUTCDay() + 6) % 7 // 월요일로 맞춘다
    return { start: s, end: e + 7 }
  }, [all, today])

  const scrollToToday = () => {
    if (!range || !scroller.current) return
    const x = (today - range.start) * px
    scroller.current.scrollLeft = Math.max(0, x - (scroller.current.clientWidth - leftW) / 2)
  }
  useEffect(() => {
    scrollToToday()
    // 눈금 단위·항목 표시를 바꾸면 오늘 위치로 다시 맞춘다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [zoom, leftMode, !!range])

  if (!range) return <p className="muted">기간이 입력된 업무가 없어 간트를 그릴 수 없습니다.</p>

  const total = range.end - range.start + 1
  const width = total * px

  // 상단 눈금: 월 구간 + (일/주 단위) 하위 눈금
  const months = []
  for (let d = range.start; d <= range.end; ) {
    const dt = dateOf(d)
    const next = Date.UTC(dt.getUTCFullYear(), dt.getUTCMonth() + 1, 1) / DAY
    const stop = Math.min(next - 1, range.end)
    months.push({ label: `${dt.getUTCFullYear()}.${pad(dt.getUTCMonth() + 1)}`, w: (stop - d + 1) * px })
    d = stop + 1
  }
  const ticks = []
  if (zoom === 'day') for (let d = range.start; d <= range.end; d++) ticks.push({ d, w: px, label: dateOf(d).getUTCDate(), weekend: [0, 6].includes(dateOf(d).getUTCDay()) })
  if (zoom === 'week') for (let d = range.start; d <= range.end; d += 7) ticks.push({ d, w: 7 * px, label: md(d) })
  const unit = zoom === 'day' ? px : zoom === 'week' ? 7 * px : 0
  const gridBg = unit ? { backgroundImage: 'linear-gradient(to right, #e6e9f0 1px, transparent 1px)', backgroundSize: `${unit}px 100%` } : {}

  // 행별 막대 위치(화살표·막대 공통)
  const geo = rows.map(({ task }) => {
    const leaf = isLeaf(all, task)
    const r = leaf ? { start: task.start, end: task.end } : rangeOf(all, task)
    if (!r.start || !r.end) return null
    const s = toDay(r.start)
    const e = toDay(r.end)
    return { leaf, x: (s - range.start) * px, w: Math.max((e - s + 1) * px, 4) }
  })
  const rowOf = new Map(rows.map(({ task }, i) => [task.id, i]))
  const arrows = []
  rows.forEach(({ task }, i) => {
    if (!task.predId || !geo[i]) return
    const pi = rowOf.get(task.predId)
    if (pi === undefined || !geo[pi]) return
    const x1 = geo[pi].x + geo[pi].w
    const y1 = pi * ROW_H + ROW_H / 2
    const x2 = geo[i].x
    const y2 = i * ROW_H + ROW_H / 2
    const pts = x2 >= x1 + 12
      ? [[x1, y1], [x1 + 6, y1], [x1 + 6, y2], [x2, y2]]
      : [[x1, y1], [x1 + 6, y1], [x1 + 6, (y1 + y2) / 2], [x2 - 8, (y1 + y2) / 2], [x2 - 8, y2], [x2, y2]]
    arrows.push({ key: `${task.predId}-${task.id}`, points: pts.map((p) => p.join(',')).join(' ') })
  })
  const todayX = (today - range.start) * px + px / 2

  const cell = (task, no, key) => {
    const p = progressOf(all, task)
    const leaf = isLeaf(all, task)
    const r = leaf ? { start: task.start, end: task.end } : rangeOf(all, task)
    return {
      no: no,
      name: task.name,
      assignee: leaf ? task.assignee || '-' : '',
      start: r.start ? r.start.slice(5) : '-',
      end: r.end ? r.end.slice(5) : '-',
      pct: `${p}%`,
    }[key]
  }

  return (
    <div>
      <div className="pms-toolbar" style={{ marginBottom: 8 }}>
        <div className="pms-seg" title="눈금 단위">
          {Object.entries(ZOOMS).map(([k, z]) => (
            <button key={k} className={zoom === k ? 'on' : ''} onClick={() => setZoom(k)}>
              {z.label}
            </button>
          ))}
        </div>
        <div className="pms-seg" title="왼쪽 WBS 항목 표시 — 숨기면 타임라인을 더 넓게 볼 수 있습니다">
          <button className={leftMode === 'full' ? 'on' : ''} onClick={() => setLeftMode('full')}>
            항목 전체
          </button>
          <button className={leftMode === 'name' ? 'on' : ''} onClick={() => setLeftMode('name')}>
            작업명만
          </button>
          <button className={leftMode === 'none' ? 'on' : ''} onClick={() => setLeftMode('none')}>
            항목 숨김
          </button>
        </div>
        <button className="secondary small" onClick={scrollToToday}>
          오늘로 이동
        </button>
        <span className="muted" style={{ marginLeft: 'auto', display: 'flex', gap: 10, alignItems: 'center' }}>
          {Object.entries(STATUS_COLOR).map(([k, c]) => (
            <span key={k} style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
              <i style={{ width: 10, height: 10, background: c, borderRadius: 2, display: 'inline-block' }} /> {k}
            </span>
          ))}
          <span style={{ color: '#d64545' }}>┆ 오늘</span>
        </span>
      </div>

      <div ref={scroller} style={{ overflow: 'auto', maxHeight: '68vh', border: '1px solid #dfe3ec', borderRadius: 6, background: '#fff' }}>
        <div style={{ display: 'flex', width: leftW + width, minWidth: '100%' }}>
          {leftMode !== 'none' && (
            <div style={{ position: 'sticky', left: 0, zIndex: 4, flex: 'none', width: leftW, background: '#fff', borderRight: '2px solid #cfd5e3' }}>
              <div style={{ position: 'sticky', top: 0, zIndex: 5, height: HEAD_H, display: 'flex', alignItems: 'flex-end', background: '#f4f5f9', borderBottom: '1px solid #dfe3ec' }}>
                {cols.map(([k, label, w]) => (
                  <div key={k} style={{ width: w, padding: '0 6px 6px', fontSize: 12, fontWeight: 600, color: '#555', flex: 'none' }}>
                    {label}
                  </div>
                ))}
              </div>
              {rows.map(({ task, no, depth }) => {
                const leaf = isLeaf(all, task)
                return (
                  <div
                    key={task.id}
                    style={{ display: 'flex', alignItems: 'center', height: ROW_H, borderBottom: '1px solid #eef0f4', fontSize: 13, background: depth === 0 ? '#eef1f8' : leaf ? '#fff' : '#f8f9fc' }}
                  >
                    {cols.map(([k, , w]) => (
                      <div
                        key={k}
                        style={{
                          width: w,
                          flex: 'none',
                          padding: '0 6px',
                          overflow: 'hidden',
                          whiteSpace: 'nowrap',
                          textOverflow: 'ellipsis',
                          fontWeight: k === 'name' && !leaf ? 600 : 400,
                          paddingLeft: k === 'name' ? 6 + depth * 14 : 6,
                          textAlign: ['start', 'end', 'pct'].includes(k) ? 'center' : 'left',
                        }}
                        title={k === 'name' ? task.name : undefined}
                      >
                        {k === 'name' && !leaf ? (
                          <button onClick={() => toggle(task.id)} style={{ border: 'none', background: 'none', cursor: 'pointer', padding: 0, width: 18, fontSize: 10, color: '#555' }}>
                            {expanded.has(task.id) ? '▼' : '▶'}
                          </button>
                        ) : k === 'name' ? (
                          <span style={{ display: 'inline-block', width: 18 }} />
                        ) : null}
                        {cell(task, no, k)}
                      </div>
                    ))}
                  </div>
                )
              })}
            </div>
          )}

          <div style={{ flex: 'none', width, position: 'relative' }}>
            <div style={{ position: 'sticky', top: 0, zIndex: 3, height: HEAD_H, background: '#f4f5f9', borderBottom: '1px solid #dfe3ec' }}>
              <div style={{ display: 'flex', height: 24 }}>
                {months.map((m) => (
                  <div key={m.label} style={{ width: m.w, flex: 'none', fontSize: 12, fontWeight: 600, padding: '4px 6px', borderRight: '1px solid #dfe3ec', overflow: 'hidden', whiteSpace: 'nowrap' }}>
                    {m.label}
                  </div>
                ))}
              </div>
              <div style={{ display: 'flex', height: 24 }}>
                {ticks.map((t) => (
                  <div
                    key={t.d}
                    style={{ width: t.w, flex: 'none', fontSize: 11, textAlign: 'center', lineHeight: '24px', color: t.d === today ? '#d64545' : t.weekend ? '#c0392b' : '#777', fontWeight: t.d === today ? 700 : 400, background: t.weekend ? '#f1e8e8' : undefined, overflow: 'hidden' }}
                  >
                    {t.label}
                  </div>
                ))}
              </div>
            </div>

            <div style={{ position: 'relative', height: rows.length * ROW_H, ...gridBg }}>
              {rows.map(({ depth }, i) => (
                <div key={i} style={{ position: 'absolute', left: 0, right: 0, top: i * ROW_H, height: ROW_H, borderBottom: '1px solid #eef0f4', background: depth === 0 ? 'rgba(238,241,248,.6)' : undefined }} />
              ))}
              {today >= range.start && today <= range.end && (
                <div style={{ position: 'absolute', left: todayX, top: 0, bottom: 0, width: 0, borderLeft: '2px dashed #d64545', zIndex: 2, pointerEvents: 'none' }} />
              )}
              {rows.map(({ task }, i) => {
                const g = geo[i]
                if (!g) return null
                const p = progressOf(all, task)
                const status = statusOf(all, task)
                const color = g.leaf ? STATUS_COLOR[status] : '#4b556b'
                const h = g.leaf ? 18 : 10
                const label = leftMode === 'none' ? `${task.name} · ${p}%` : null
                return (
                  <div key={task.id}>
                    <div
                      onClick={() => canEdit && onEdit?.(task)}
                      title={`${task.name}\n${task.start ?? ''} ~ ${task.end ?? ''}${g.leaf ? `\n담당 ${task.assignee || '미배정'}` : ''}\n실적 ${p}% · ${status}${canEdit ? '\n(클릭하여 수정)' : ''}`}
                      style={{ position: 'absolute', left: g.x, width: g.w, top: i * ROW_H + (ROW_H - h) / 2, height: h, borderRadius: g.leaf ? 4 : 2, background: color + '40', border: `1px solid ${color}`, cursor: canEdit ? 'pointer' : 'default', overflow: 'hidden', zIndex: 1 }}
                    >
                      <div style={{ width: `${p}%`, height: '100%', background: color }} />
                      {g.leaf && g.w > 46 && (
                        <span style={{ position: 'absolute', left: 6, top: 0, lineHeight: `${h - 2}px`, fontSize: 11, color: p >= 55 ? '#fff' : '#333', whiteSpace: 'nowrap' }}>{p}%</span>
                      )}
                    </div>
                    {label && (
                      <span style={{ position: 'absolute', left: g.x + g.w + 6, top: i * ROW_H + 7, fontSize: 12, color: '#444', whiteSpace: 'nowrap', zIndex: 1, pointerEvents: 'none' }}>{label}</span>
                    )}
                  </div>
                )
              })}
              <svg width={width} height={rows.length * ROW_H} style={{ position: 'absolute', left: 0, top: 0, pointerEvents: 'none', zIndex: 2 }}>
                <defs>
                  <marker id="gantt-arrow" markerWidth="6" markerHeight="6" refX="5" refY="3" orient="auto">
                    <path d="M0,0 L6,3 L0,6 z" fill="#7a869a" />
                  </marker>
                </defs>
                {arrows.map((a) => (
                  <polyline key={a.key} points={a.points} fill="none" stroke="#7a869a" strokeWidth="1.2" markerEnd="url(#gantt-arrow)" />
                ))}
              </svg>
            </div>
          </div>
        </div>
      </div>
      <p className="muted" style={{ margin: '6px 0 0' }}>
        막대 안의 채움은 실적 진척률이고, 얇은 어두운 막대는 하위 업무 기간을 합친 단계·분류입니다. 화살표는 선행 업무(앞 업무가 끝나야 시작)입니다.
        {canEdit ? ' 막대를 누르면 업무를 수정할 수 있습니다.' : ''}
      </p>
    </div>
  )
}
