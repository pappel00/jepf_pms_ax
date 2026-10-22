import './pms.css'
import { CHAIN, MEMBERS } from './pmsStore'
import { MANAGER_ROLES, NOTIFY_CHANNELS, TEAM_ROLES, ACCESS_LEVELS, calcEnd, gapDays, isPartner, monthsBetween, newMember, orgOf } from './teamConfig'

/**
 * 프로젝트 관리자(1명) + 투입 인력 편집기 — 단계전환(3번)과 수행 단계 「프로젝트 관리」가 같이 쓴다.
 *  - 관리자: 수행 단계 기본 정보(프로젝트 관리 · 문서 테일러링 · WBS/진척)를 관리, 역할은 PM/PL/PMO
 *  - 투입 인력: 역할, 투입 단계(전체 가능), 투입 M/M, 투입 기간(FROM~TO) — 이후 인력관리(투입관리)의 기준 데이터
 *  - 협력사 인력: 로그인 없이 내 업무 알림(메신저)으로 진척·이슈를 처리하므로 알림 채널을 지정(관리자가 대신 입력 가능)
 * period: 프로젝트 기간 { start, end } — 새 인력의 투입 기간 기본값과 범위 경고에 쓴다.
 */
const nowrap = { whiteSpace: 'nowrap' }

export default function TeamEditor({ manager, setManager, team, setTeam, phases, period, readOnly = false, managerRoles = MANAGER_ROLES, roles = TEAM_ROLES, phaseList = CHAIN, managerHint }) {
  // M/M·투입 비율·시작일·공백 구간이 바뀌면(자동 모드) 종료일을 다시 계산한다. 종료일을 직접 고치면 자동 모드가 해제된다.
  const patchMember = (idx, patch) =>
    setTeam((prev) =>
      prev.map((m, i) => {
        if (i !== idx) return m
        const next = { ...m, ...patch }
        if ('to' in patch && !('auto' in patch)) next.auto = false
        if (next.auto && ['mm', 'rate', 'from', 'gaps'].some((k) => k in patch)) {
          const end = calcEnd(next.from, next.mm, next.rate, next.gaps)
          if (end) next.to = end
        }
        return next
      }),
    )
  const patchGap = (idx, gi, patch) => patchMember(idx, { gaps: (team[idx].gaps ?? []).map((g, j) => (j === gi ? { ...g, ...patch } : g)) })
  const addGap = (idx) => patchMember(idx, { gaps: [...(team[idx].gaps ?? []), { from: '', to: '' }] })
  const removeGap = (idx, gi) => patchMember(idx, { gaps: (team[idx].gaps ?? []).filter((_, j) => j !== gi) })
  const removeMember = (idx) => setTeam((prev) => prev.filter((_, i) => i !== idx))
  const addMember = () =>
    setTeam((prev) => {
      const free = MEMBERS.find((m) => !prev.some((x) => x.name === m.name))
      return [...prev, newMember(free?.name ?? '', period, phaseList)]
    })
  const togglePhase = (idx, ph) =>
    setTeam((prev) =>
      prev.map((m, i) => (i === idx ? { ...m, phases: m.phases.includes(ph) ? m.phases.filter((x) => x !== ph) : [...m.phases, ph] } : m)),
    )

  const totalMm = team.reduce((s, m) => s + (Number(m.mm) || 0), 0)
  const partners = team.filter((m) => isPartner(m.name)).length

  return (
    <div>
      <div className="pms-form-section" style={{ marginTop: 4 }}>
        프로젝트 관리자 (1명)<span className="req-mark"> *</span>
      </div>
      <div className="pms-form-grid" style={{ gridTemplateColumns: '1fr 1fr 1.8fr' }}>
        <div className="field pms-field">
          <label>관리자</label>
          <select disabled={readOnly} value={manager.name} onChange={(e) => setManager({ ...manager, name: e.target.value })}>
            {MEMBERS.map((m) => (
              <option key={m.name} value={m.name}>
                {m.name} ({orgOf(m.name)})
              </option>
            ))}
          </select>
        </div>
        <div className="field pms-field">
          <label>프로젝트 내 역할</label>
          <select disabled={readOnly} value={manager.role} onChange={(e) => setManager({ ...manager, role: e.target.value })}>
            {managerRoles.map((r) => (
              <option key={r}>{r}</option>
            ))}
          </select>
        </div>
        <div className="pms-field-hint" style={{ alignSelf: 'center' }}>
          {managerHint ?? (
            <>
              수행 단계의 기본 정보(<b>프로젝트 관리 · 문서 테일러링 · WBS/진척 관리</b>)를 관리하는 사람입니다. 역할은 PM, PL 또는 PMO가 될 수 있고, 관리자도
              투입 인력에 함께 지정할 수 있습니다. 사업관리·단계말 검토 업무의 담당이 됩니다.
            </>
          )}
        </div>
      </div>

      <div className="pms-form-section">
        투입 인력 <span className="muted" style={{ fontWeight: 400 }}>{team.length}명 · 투입 M/M 합계 {totalMm || 0}{partners ? ` · 협력사 ${partners}명` : ''}</span>
      </div>
      <div style={{ overflowX: 'auto' }}>
      <table className="data-table pms-wbs pms-team">
        <thead>
          <tr>
            <th>인력</th>
            <th>프로젝트 내 역할</th>
            <th>투입 단계</th>
            <th>투입 M/M</th>
            <th>투입 비율</th>
            <th>접근 권한</th>
            <th>투입 기간 (FROM ~ TO)</th>
            {!readOnly && <th></th>}
          </tr>
        </thead>
        <tbody>
          {team.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={8}>
                투입 인력이 없습니다.{readOnly ? '' : ' 「+ 투입 인력 추가」로 지정하세요.'}
              </td>
            </tr>
          )}
          {team.map((m, idx) => {
            const all4 = phaseList.every((ph) => m.phases.includes(ph))
            const badPeriod = m.from && m.to && m.to < m.from
            const outside = period && m.from && m.to && !badPeriod && (m.from < period.start || m.to > period.end)
            const suggest = monthsBetween(m.from, m.to)
            return (
              <tr key={idx}>
                <td style={nowrap}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <select
                      className="pms-cell-input"
                      style={{ minWidth: 0, width: 130 }}
                      disabled={readOnly}
                      value={m.name}
                      onChange={(e) =>
                        patchMember(idx, { name: e.target.value, notify: isPartner(e.target.value) ? m.notify || 'Mattermost' : '' })
                      }
                    >
                      <option value="">선택</option>
                      {MEMBERS.filter((x) => x.name === m.name || !team.some((t) => t.name === x.name)).map((x) => (
                        <option key={x.name} value={x.name}>
                          {x.name} ({orgOf(x.name)})
                        </option>
                      ))}
                    </select>
                    {isPartner(m.name) && (
                      <>
                        <span className="pms-tag manual" title="협력사 인력은 내 업무 알림(메신저)으로 진척·이슈를 처리합니다. 관리자가 대신 입력할 수도 있습니다.">
                          협력
                        </span>
                        <select className="pms-cell-input" style={{ minWidth: 0, width: 84, padding: '2px 4px' }} title="알림 채널" disabled={readOnly} value={m.notify || '미설정'} onChange={(e) => patchMember(idx, { notify: e.target.value })}>
                          {NOTIFY_CHANNELS.map((c) => (
                            <option key={c}>{c}</option>
                          ))}
                        </select>
                      </>
                    )}
                  </div>
                </td>
                <td>
                  <select className="pms-cell-input" disabled={readOnly} value={m.role} onChange={(e) => patchMember(idx, { role: e.target.value })}>
                    {roles.map((r) => (
                      <option key={r}>{r}</option>
                    ))}
                  </select>
                </td>
                <td>
                  <label style={{ marginRight: 12, fontWeight: 700, fontSize: 13 }}>
                    <input type="checkbox" disabled={readOnly} checked={all4} onChange={(e) => patchMember(idx, { phases: e.target.checked ? [...phaseList] : [] })} /> 전체
                  </label>
                  {phaseList.map((ph) => (
                    <label key={ph} style={{ marginRight: 10, fontSize: 13, opacity: !phases || phases.has(ph) ? 1 : 0.4 }} title={!phases || phases.has(ph) ? '' : '선택하지 않은 단계'}>
                      <input type="checkbox" disabled={readOnly} checked={m.phases.includes(ph)} onChange={() => togglePhase(idx, ph)} /> {ph}
                    </label>
                  ))}
                </td>
                <td>
                  <input
                    type="number"
                    min="0"
                    step="0.5"
                    className="pms-cell-input pms-md"
                    disabled={readOnly}
                    value={m.mm ?? ''}
                    placeholder="M/M"
                    onChange={(e) => patchMember(idx, { mm: e.target.value === '' ? '' : Number(e.target.value) })}
                  />
                  {!readOnly && suggest > 0 && (
                    <button className="link-btn" style={{ marginLeft: 6, fontSize: 11 }} title="투입 기간 기준(투입률 100%) M/M으로 채웁니다" onClick={() => patchMember(idx, { mm: suggest })}>
                      기간 {suggest}
                    </button>
                  )}
                </td>
                <td>
                  <input
                    type="number"
                    min="1"
                    max="200"
                    step="5"
                    className="pms-cell-input"
                    style={{ width: 64, minWidth: 0 }}
                    disabled={readOnly}
                    value={m.rate ?? ''}
                    placeholder="%"
                    onChange={(e) => patchMember(idx, { rate: e.target.value === '' ? '' : Number(e.target.value) })}
                  />
                  <span style={{ marginLeft: 2 }}>%</span>
                </td>
                <td>
                  <select className="pms-cell-input" style={{ minWidth: 0 }} disabled={readOnly} value={m.access ?? '편집'} onChange={(e) => patchMember(idx, { access: e.target.value })} title="편집: WBS 진척·이슈 입력 가능 / 열람: 조회만">
                    {ACCESS_LEVELS.map((a) => (
                      <option key={a}>{a}</option>
                    ))}
                  </select>
                </td>
                <td style={nowrap}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <input type="date" className="pms-cell-input" style={{ width: 128, minWidth: 0 }} disabled={readOnly} value={m.from ?? ''} onChange={(e) => patchMember(idx, { from: e.target.value })} />
                    <span>~</span>
                    <input type="date" className="pms-cell-input" style={{ width: 128, minWidth: 0 }} disabled={readOnly} value={m.to ?? ''} onChange={(e) => patchMember(idx, { to: e.target.value })} />
                    {!readOnly && m.rate != null && (
                      m.auto ? (
                        <span className="pms-tag done" title="종료일 자동 계산 (M/M ÷ 투입 비율). 종료일을 직접 고치면 수동 지정이 됩니다.">자동</span>
                      ) : (
                        <button className="link-btn" style={{ fontSize: 11 }} title="종료일을 M/M ÷ 투입 비율로 다시 계산합니다" onClick={() => patchMember(idx, { auto: true, from: m.from })}>
                          수동 · 자동계산
                        </button>
                      )
                    )}
                    {!readOnly && m.rate != null && (
                      <button className="link-btn" style={{ fontSize: 11 }} title="투입이 중간에 비는 기간(타 업무 병행·휴지기 등)을 등록하면 그 기간은 계획 M/M에서 제외됩니다" onClick={() => addGap(idx)}>
                        +공백
                      </button>
                    )}
                    {(m.gaps ?? []).length > 0 && m.from && m.to && <span className="pms-tag late" title="공백 구간으로 제외된 일수">공백 {gapDays(m.gaps, m.from, m.to)}일</span>}
                    {badPeriod && <span className="pms-tag late" title="종료일이 시작일보다 빠릅니다.">⚠ 기간 오류</span>}
                    {outside && <span className="pms-tag manual" title={`프로젝트 기간(${period.start} ~ ${period.end}) 밖입니다.`}>⚠ 기간 밖</span>}
                  </div>
                  {(m.gaps ?? []).map((g, gi) => (
                    <div key={gi} style={{ marginTop: 4, fontSize: 12 }}>
                      <span className="pms-tag late">공백</span>{' '}
                      <input type="date" className="pms-cell-input" style={{ width: 118, minWidth: 0 }} disabled={readOnly} value={g.from} onChange={(e) => patchGap(idx, gi, { from: e.target.value })} />
                      {' ~ '}
                      <input type="date" className="pms-cell-input" style={{ width: 118, minWidth: 0 }} disabled={readOnly} value={g.to} onChange={(e) => patchGap(idx, gi, { to: e.target.value })} />
                      {!readOnly && <button className="link-btn danger" onClick={() => removeGap(idx, gi)}>✕</button>}
                    </div>
                  ))}
                </td>
                {!readOnly && (
                  <td>
                    <button className="link-btn danger" onClick={() => removeMember(idx)}>
                      삭제
                    </button>
                  </td>
                )}
              </tr>
            )
          })}
          {team.length > 0 && (
            <tr className="total-row">
              <td colSpan={3}>투입 M/M 합계</td>
              <td>{totalMm || 0}</td>
              <td colSpan={readOnly ? 3 : 4} />
            </tr>
          )}
        </tbody>
      </table>
      </div>
      {!readOnly && (
        <div style={{ display: 'flex', gap: 8, marginTop: 8, alignItems: 'center', flexWrap: 'wrap' }}>
          <button className="secondary small" onClick={addMember} disabled={team.length >= MEMBERS.length}>
            + 투입 인력 추가
          </button>
          <button className="link-btn" onClick={() => setTeam([])}>
            모두 삭제
          </button>
          <span className="pms-field-hint">M/M·투입 기간은 이후 인력관리(투입관리)의 기준이 됩니다. M/M은 단계별 업무 분배 비율에도 쓰입니다.</span>
        </div>
      )}
    </div>
  )
}
