import './pms.css'
import { CHAIN, MEMBERS } from './pmsStore'
import { MANAGER_ROLES, NOTIFY_CHANNELS, TEAM_ROLES, isPartner, monthsBetween, newMember, orgOf } from './teamConfig'

/**
 * 프로젝트 관리자(1명) + 투입 인력 편집기 — 단계전환(3번)과 수행 단계 「프로젝트 관리」가 같이 쓴다.
 *  - 관리자: 수행 단계 기본 정보(프로젝트 관리 · 문서 테일러링 · WBS/진척)를 관리, 역할은 PM/PL/PMO
 *  - 투입 인력: 역할, 투입 단계(전체 가능), 투입 M/M, 투입 기간(FROM~TO) — 이후 인력관리(투입관리)의 기준 데이터
 *  - 협력사 인력: 로그인 없이 내 업무 알림(메신저)으로 진척·이슈를 처리하므로 알림 채널을 지정(관리자가 대신 입력 가능)
 * period: 프로젝트 기간 { start, end } — 새 인력의 투입 기간 기본값과 범위 경고에 쓴다.
 */
export default function TeamEditor({ manager, setManager, team, setTeam, phases, period, readOnly = false, managerRoles = MANAGER_ROLES, roles = TEAM_ROLES, phaseList = CHAIN, managerHint }) {
  const patchMember = (idx, patch) => setTeam((prev) => prev.map((m, i) => (i === idx ? { ...m, ...patch } : m)))
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
      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th style={{ width: 170 }}>인력</th>
            <th style={{ width: 120 }}>프로젝트 내 역할</th>
            <th>투입 단계</th>
            <th style={{ width: 130 }}>투입 M/M</th>
            <th style={{ width: 300 }}>투입 기간 (FROM ~ TO)</th>
            {!readOnly && <th></th>}
          </tr>
        </thead>
        <tbody>
          {team.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={6}>
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
                <td>
                  <select
                    className="pms-cell-input"
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
                    <div style={{ marginTop: 4, fontSize: 11 }} title="협력사 인력은 내 업무 알림(메신저)으로 진척·이슈를 처리합니다. 관리자가 대신 입력할 수도 있습니다.">
                      <span className="pms-tag manual">협력사</span> 알림{' '}
                      <select className="pms-cell-input" style={{ minWidth: 0, width: 90, padding: '2px 4px' }} disabled={readOnly} value={m.notify || '미설정'} onChange={(e) => patchMember(idx, { notify: e.target.value })}>
                        {NOTIFY_CHANNELS.map((c) => (
                          <option key={c}>{c}</option>
                        ))}
                      </select>
                    </div>
                  )}
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
                  <input type="date" className="pms-cell-input" style={{ width: 128, minWidth: 0 }} disabled={readOnly} value={m.from ?? ''} onChange={(e) => patchMember(idx, { from: e.target.value })} />
                  {' ~ '}
                  <input type="date" className="pms-cell-input" style={{ width: 128, minWidth: 0 }} disabled={readOnly} value={m.to ?? ''} onChange={(e) => patchMember(idx, { to: e.target.value })} />
                  {badPeriod && <div className="pms-field-error">종료일이 시작일보다 빠릅니다.</div>}
                  {outside && <div className="pms-field-hint">프로젝트 기간({period.start} ~ {period.end}) 밖입니다.</div>}
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
              <td colSpan={readOnly ? 1 : 2} />
            </tr>
          )}
        </tbody>
      </table>
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
