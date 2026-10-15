import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import Modal from '../../../components/Modal'
import './pms.css'
import { DEFAULT_PROJECT_ID, DOC_PHASES, addCustomDoc, docOf, patchItem, removeItem, resetTailoring, tailoringOf, useDocs } from './docStore'
import { MEMBERS } from './pmsStore'
import { useProjects } from './projectStore'

function AddDocForm({ projectId, onClose }) {
  const [f, setF] = useState({ phase: DOC_PHASES[0], name: '', required: false, owner: MEMBERS[1].name })
  const [error, setError] = useState('')
  return (
    <Modal title="문서 종류 추가 (프로젝트 고유)" onClose={onClose} width={500}>
      <div className="field">
        <label>
          단계<span className="req-mark"> *</span>
        </label>
        <select value={f.phase} onChange={(e) => setF({ ...f, phase: e.target.value })}>
          {DOC_PHASES.map((p) => (
            <option key={p}>{p}</option>
          ))}
        </select>
      </div>
      <div className="field">
        <label>
          문서 종류<span className="req-mark"> *</span>
        </label>
        <input value={f.name} onChange={(e) => setF({ ...f, name: e.target.value })} placeholder="예) 모바일 단말 호환성 시험서" />
      </div>
      <div className="pms-form-grid">
        <div className="field">
          <label>구분</label>
          <select value={f.required ? 'Y' : 'N'} onChange={(e) => setF({ ...f, required: e.target.value === 'Y' })}>
            <option value="N">권장</option>
            <option value="Y">필수</option>
          </select>
        </div>
        <div className="field">
          <label>담당자</label>
          <select value={f.owner} onChange={(e) => setF({ ...f, owner: e.target.value })}>
            {MEMBERS.map((m) => (
              <option key={m.name}>{m.name}</option>
            ))}
          </select>
        </div>
      </div>
      {error && <div className="pms-field-error">{error}</div>}
      <button
        className="primary"
        onClick={() => {
          if (!f.name.trim()) {
            setError('문서 종류를 입력하세요.')
            return
          }
          addCustomDoc(projectId, { ...f, name: f.name.trim() })
          onClose()
        }}
      >
        추가
      </button>
    </Modal>
  )
}

/**
 * 문서 테일러링 관리 — 표준 산출물 목록(단계별 문서 종류)에서 프로젝트에 적용할 문서를 정한다.
 * 적용된 문서가 WBS의 단계말 검토에서 등록·관리되는 대상이 된다. 필수 산출물을 제외하려면 사유가 필요하다.
 */
export default function DocTailoringPage() {
  const s = useDocs()
  const projects = useProjects()
  const candidates = projects.filter((p) => ['수주', '수행 중', '수행 종료', '운영 중'].includes(p.status))
  const [params] = useSearchParams()
  const wanted = params.get('project') // 단계전환 직후 등에서 ?project=ID 로 들어오면 그 프로젝트를 먼저 연다
  const [projectId, setProjectId] = useState(
    candidates.find((p) => p.id === wanted)?.id ?? candidates.find((p) => p.id === DEFAULT_PROJECT_ID)?.id ?? candidates[0]?.id ?? '',
  )
  const [phaseFilter, setPhaseFilter] = useState('')
  const [addForm, setAddForm] = useState(false)

  if (!projectId) {
    return (
      <div>
        <div className="page-header">
          <h2>문서 테일러링 관리</h2>
        </div>
        <p className="muted">수주 이후(수주 / 수행 중 / 완료) 프로젝트가 없습니다.</p>
      </div>
    )
  }

  const items = tailoringOf(s, projectId)
  const applied = items.filter((t) => t.applied === '적용')
  const excluded = items.filter((t) => t.applied !== '적용')
  const requiredExcluded = excluded.filter((t) => t.required)
  const noReason = excluded.filter((t) => !t.reason.trim()).length
  const custom = items.filter((t) => t.custom).length
  const registeredCount = (t) => docOf(s, projectId, t.id).versions.length

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>
            문서 테일러링 관리 <span className="pms-mock-badge">목업 · 더미 데이터</span>
          </h2>
          <p className="page-description">
            단계별 표준 산출물(문서 종류) 중 이 프로젝트에 적용할 문서를 정합니다. <b>적용</b>된 문서가 WBS의 「단계말 검토」에서 등록·수정·다운로드되는
            대상이 됩니다.
          </p>
        </div>
      </div>

      <div className="pms-toolbar">
        <select value={projectId} onChange={(e) => setProjectId(e.target.value)}>
          {candidates.map((p) => (
            <option key={p.id} value={p.id}>
              {p.id} · {p.name} ({p.status})
            </option>
          ))}
        </select>
        <button className="primary small" style={{ marginLeft: 'auto' }} onClick={() => setAddForm(true)}>
          + 문서 종류 추가
        </button>
        <button
          className="secondary small"
          onClick={() => {
            if (window.confirm('표준 목록으로 초기화할까요? 적용 여부·사유·추가한 문서 종류가 모두 되돌려집니다(등록된 산출물 파일은 유지되지 않을 수 있음).')) resetTailoring(projectId)
          }}
        >
          표준 목록으로 초기화
        </button>
      </div>

      <div className="pms-summary">
        <div className="pms-stat">
          <div className="label">문서 종류 전체</div>
          <div className="value">{items.length}</div>
          <div className="muted">프로젝트 고유 추가 {custom}종</div>
        </div>
        <div className="pms-stat ok">
          <div className="label">적용</div>
          <div className="value">{applied.length}</div>
        </div>
        <div className="pms-stat">
          <div className="label">미적용(제외)</div>
          <div className="value">{excluded.length}</div>
        </div>
        <div className={`pms-stat ${requiredExcluded.length ? 'warn' : ''}`}>
          <div className="label">필수 산출물 제외</div>
          <div className="value">{requiredExcluded.length}</div>
          <div className="muted">사유 필수 · 발주사 합의 필요</div>
        </div>
        <div className={`pms-stat ${noReason ? 'danger' : ''}`}>
          <div className="label">사유 미입력</div>
          <div className="value">{noReason}</div>
        </div>
      </div>

      <div className="pms-chips">
        <button className={!phaseFilter ? 'on' : ''} onClick={() => setPhaseFilter('')}>
          전체 <span>{items.length}</span>
        </button>
        {DOC_PHASES.map((p) => (
          <button key={p} className={phaseFilter === p ? 'on' : ''} onClick={() => setPhaseFilter(p)}>
            {p} 단계 <span>{items.filter((t) => t.phase === p).length}</span>
          </button>
        ))}
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>문서 종류</th>
            <th>표준 구분</th>
            <th>적용 여부</th>
            <th>테일러링 사유</th>
            <th>담당자</th>
            <th>제출 시점</th>
            <th>등록 현황</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {DOC_PHASES.filter((p) => !phaseFilter || p === phaseFilter).flatMap((phase) => {
            const rows = items.filter((t) => t.phase === phase)
            if (rows.length === 0) return []
            return [
              <tr key={`h-${phase}`} className="phase-row">
                <td colSpan={8}>
                  {phase} 단계 <span className="muted">({rows.filter((t) => t.applied === '적용').length}/{rows.length}종 적용)</span>
                </td>
              </tr>,
              ...rows.map((t) => {
                const isApplied = t.applied === '적용'
                const needReason = !isApplied && !t.reason.trim()
                return (
                  <tr key={t.id} style={isApplied ? undefined : { background: '#fafafa' }}>
                    <td className="name-cell">
                      <b style={isApplied ? undefined : { color: '#999', textDecoration: 'line-through' }}>{t.name}</b>{' '}
                      {t.custom && <span className="pms-tag manual">추가</span>}
                    </td>
                    <td>{t.required ? <span className="pms-tag late">필수</span> : <span className="pms-tag">권장</span>}</td>
                    <td>
                      <select
                        className="pms-cell-input"
                        style={{ minWidth: 90 }}
                        value={t.applied}
                        onChange={(e) => patchItem(projectId, t.id, { applied: e.target.value })}
                      >
                        <option>적용</option>
                        <option>미적용</option>
                      </select>
                    </td>
                    <td style={{ minWidth: 260 }}>
                      <input
                        className="pms-cell-input"
                        style={needReason ? { borderColor: '#d32f2f' } : undefined}
                        disabled={isApplied}
                        value={t.reason}
                        placeholder={isApplied ? '-' : t.required ? '필수 산출물 제외 사유 (필수)' : '제외 사유 (필수)'}
                        onChange={(e) => patchItem(projectId, t.id, { reason: e.target.value })}
                      />
                      {needReason && <div className="pms-field-error">사유를 입력하세요.</div>}
                      {!isApplied && t.required && !needReason && <div className="pms-field-hint">필수 산출물 제외 — 발주사 합의 필요</div>}
                    </td>
                    <td>
                      <select
                        className="pms-cell-input"
                        style={{ minWidth: 80 }}
                        disabled={!isApplied}
                        value={t.owner}
                        onChange={(e) => patchItem(projectId, t.id, { owner: e.target.value })}
                      >
                        <option value="">미지정</option>
                        {MEMBERS.map((m) => (
                          <option key={m.name}>{m.name}</option>
                        ))}
                      </select>
                    </td>
                    <td>{isApplied ? `${t.phase} 단계말 검토` : '-'}</td>
                    <td>
                      {isApplied ? (
                        registeredCount(t) > 0 ? (
                          <span className="pms-tag doing">등록됨 ({registeredCount(t)}버전)</span>
                        ) : (
                          <span className="pms-tag late">미등록</span>
                        )
                      ) : (
                        '-'
                      )}
                    </td>
                    <td>
                      {t.custom && (
                        <button
                          className="link-btn danger"
                          onClick={() => {
                            if (window.confirm(`"${t.name}" 문서 종류를 삭제할까요?`)) removeItem(projectId, t.id)
                          }}
                        >
                          삭제
                        </button>
                      )}
                    </td>
                  </tr>
                )
              }),
            ]
          })}
        </tbody>
      </table>

      <div className="pms-hint" style={{ marginTop: 14 }}>
        • 표준 문서 종류는 시스템 표준 목록에서 오며, 프로젝트마다 <b>적용 / 미적용</b>을 정하고 필요한 문서를 추가할 수 있습니다.
        <br />• 적용한 문서는 WBS / 진척 관리 → 각 단계의 <b>단계말 검토</b> 행의 <b>산출물</b> 버튼에서 등록·수정·다운로드합니다.
        <br />• 이행 단계 문서는 후속 단계에서 다룹니다(WBS 단계말 검토가 있는 분석 · 설계 · 개발 · 테스트 단계만 대상).
      </div>

      {addForm && <AddDocForm projectId={projectId} onClose={() => setAddForm(false)} />}
    </div>
  )
}
