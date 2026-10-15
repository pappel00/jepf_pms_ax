import { useState } from 'react'
import Modal from '../../../components/Modal'
import './pms.css'
import { ME, TODAY } from './pmsStore'
import {
  deleteLatest,
  docOf,
  docStatusOf,
  setApproved,
  stageSummary,
  tailoringOf,
  updateLatest,
  uploadVersion,
  useDocs,
} from './docStore'

const STATUS_CLASS = { 미등록: 'late', 등록: 'doing', '검토 완료': 'done' }

const download = (name) => window.alert(`목업: "${name}" 다운로드`)

/** 문서 등록(새 버전) / 수정(최신 버전의 파일 교체·비고) 폼. */
function UploadForm({ projectId, item, doc, mode, onClose }) {
  const latest = doc.versions[doc.versions.length - 1]
  const [file, setFile] = useState({ fileName: '', fileSize: '' })
  const [note, setNote] = useState(mode === 'edit' ? latest?.note ?? '' : '')
  const [error, setError] = useState('')
  const isEdit = mode === 'edit'

  const submit = () => {
    if (!isEdit && !file.fileName) {
      setError('등록할 파일을 선택하세요.')
      return
    }
    if (isEdit) {
      updateLatest(projectId, item.id, { note, ...(file.fileName ? { ...file, date: TODAY, author: ME } : {}) })
    } else {
      uploadVersion(projectId, item.id, { ...file, note, author: ME, date: TODAY })
    }
    onClose()
  }

  return (
    <Modal
      title={isEdit ? `산출물 수정 — ${item.name} (${latest?.ver})` : doc.versions.length ? `새 버전 등록 — ${item.name}` : `산출물 등록 — ${item.name}`}
      onClose={onClose}
      width={520}
    >
      {isEdit && latest && (
        <p className="muted" style={{ marginTop: 0 }}>
          현재 파일: {latest.fileName} ({latest.fileSize}) — 새 파일을 선택하면 최신 버전의 파일이 교체됩니다.
        </p>
      )}
      <div className="field">
        <label>
          파일{!isEdit && <span className="req-mark"> *</span>}
          {isEdit && <span className="opt-mark"> (교체할 때만)</span>}
        </label>
        <input
          type="file"
          accept=".pdf,.ppt,.pptx,.doc,.docx,.hwp,.hwpx,.xls,.xlsx,.zip"
          onChange={(e) => {
            const f = e.target.files?.[0]
            setFile({ fileName: f?.name ?? '', fileSize: f ? `${(f.size / 1048576).toFixed(1)}MB` : '' })
          }}
        />
        <div className="pms-field-hint">목업이라 파일명만 저장합니다. 등록 버전은 자동 채번됩니다(v1.0, v1.1 …).</div>
      </div>
      <div className="field">
        <label>
          비고<span className="opt-mark"> (옵션)</span>
        </label>
        <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="예) 2차 검토 의견 반영" />
      </div>
      {error && <div className="pms-field-error">{error}</div>}
      <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
        <button className="primary" style={{ flex: 1 }} onClick={submit}>
          {isEdit ? '수정 저장' : '등록'}
        </button>
        <button className="secondary" style={{ marginTop: 0 }} onClick={onClose}>
          취소
        </button>
      </div>
    </Modal>
  )
}

/**
 * 단계말 산출물 관리 — WBS 「단계말 검토」에서 연다. 문서 테일러링에서 "적용"으로 정한 문서만 대상이며,
 * 등록 / 새 버전 / 수정 / 다운로드 / 삭제(최신 버전) / 검토 완료를 한 화면에서 처리한다.
 */
export default function StageDocsModal({ projectId, phase, onClose }) {
  const s = useDocs()
  const [form, setForm] = useState(null) // { item, mode }
  const [openHistory, setOpenHistory] = useState(null)

  const items = tailoringOf(s, projectId).filter((t) => t.phase === phase)
  const applied = items.filter((t) => t.applied === '적용')
  const excluded = items.filter((t) => t.applied !== '적용')
  const sum = stageSummary(s, projectId, phase)
  const pct = sum.total ? Math.round((sum.registered / sum.total) * 100) : 0
  const missingRequired = applied.filter((t) => t.required && docOf(s, projectId, t.id).versions.length === 0).length

  return (
    <Modal title={`${phase} 단계말 산출물 관리`} onClose={onClose} width={1100}>
      <div className="pms-summary" style={{ marginBottom: 12 }}>
        <div className="pms-stat">
          <div className="label">제출 대상 (테일러링 적용)</div>
          <div className="value">{sum.total}</div>
        </div>
        <div className="pms-stat">
          <div className="label">등록 완료</div>
          <div className="value">
            {sum.registered}/{sum.total}
          </div>
          <div className="pms-bar" style={{ width: '100%', marginTop: 6 }}>
            <span style={{ width: `${pct}%` }} />
          </div>
        </div>
        <div className="pms-stat ok">
          <div className="label">검토 완료</div>
          <div className="value">{sum.approved}</div>
        </div>
        <div className={`pms-stat ${missingRequired ? 'danger' : 'ok'}`}>
          <div className="label">필수 산출물 미등록</div>
          <div className="value">{missingRequired}</div>
        </div>
      </div>

      <table className="data-table pms-wbs">
        <thead>
          <tr>
            <th>문서 종류</th>
            <th>구분</th>
            <th>담당</th>
            <th>상태</th>
            <th>최신 버전</th>
            <th>파일</th>
            <th>등록일 / 등록자</th>
            <th>비고</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          {applied.length === 0 && (
            <tr>
              <td className="empty-cell" colSpan={9}>
                이 단계에 적용된 문서가 없습니다. 문서 테일러링 관리에서 적용 문서를 정하세요.
              </td>
            </tr>
          )}
          {applied.flatMap((t) => {
            const d = docOf(s, projectId, t.id)
            const status = docStatusOf(d)
            const latest = d.versions[d.versions.length - 1]
            const rows = [
              <tr key={t.id}>
                <td className="name-cell">
                  <b>{t.name}</b> {t.custom && <span className="pms-tag manual">추가</span>}
                </td>
                <td>{t.required ? <span className="pms-tag late">필수</span> : <span className="pms-tag">권장</span>}</td>
                <td>{t.owner || <span className="muted">미지정</span>}</td>
                <td>
                  <span className={`pms-tag ${STATUS_CLASS[status]}`}>{status}</span>
                </td>
                <td>{latest ? <b>{latest.ver}</b> : '-'}</td>
                <td>{latest ? `📎 ${latest.fileName} (${latest.fileSize})` : '-'}</td>
                <td>{latest ? `${latest.date} · ${latest.author}` : '-'}</td>
                <td className="name-cell" style={{ color: '#666', maxWidth: 200 }}>
                  {latest?.note || ''}
                </td>
                <td style={{ whiteSpace: 'nowrap' }}>
                  <button className="link-btn" onClick={() => setForm({ item: t, mode: 'new' })}>
                    {latest ? '새 버전' : '등록'}
                  </button>
                  {latest && (
                    <>
                      <button className="link-btn" onClick={() => setForm({ item: t, mode: 'edit' })}>
                        수정
                      </button>
                      <button className="link-btn" onClick={() => download(latest.fileName)}>
                        다운로드
                      </button>
                      <button className="link-btn" onClick={() => setApproved(projectId, t.id, !d.approved)}>
                        {d.approved ? '검토 해제' : '검토 완료'}
                      </button>
                      <button className="link-btn" onClick={() => setOpenHistory(openHistory === t.id ? null : t.id)}>
                        이력({d.versions.length})
                      </button>
                      <button
                        className="link-btn danger"
                        onClick={() => {
                          if (window.confirm(`"${t.name}" 최신 버전(${latest.ver})을 삭제할까요?`)) deleteLatest(projectId, t.id)
                        }}
                      >
                        삭제
                      </button>
                    </>
                  )}
                </td>
              </tr>,
            ]
            if (openHistory === t.id) {
              rows.push(
                <tr key={`${t.id}-h`}>
                  <td colSpan={9} style={{ background: '#fafbfd' }}>
                    {[...d.versions].reverse().map((v) => (
                      <div key={v.ver} style={{ display: 'flex', gap: 14, padding: '4px 8px', fontSize: 12 }}>
                        <b style={{ width: 40 }}>{v.ver}</b>
                        <span style={{ width: 150 }}>
                          {v.date} · {v.author}
                        </span>
                        <span style={{ flex: 1 }}>
                          📎 {v.fileName} ({v.fileSize}) {v.note && <span className="muted">— {v.note}</span>}
                        </span>
                        <button className="link-btn" onClick={() => download(v.fileName)}>
                          다운로드
                        </button>
                      </div>
                    ))}
                  </td>
                </tr>,
              )
            }
            return rows
          })}
        </tbody>
      </table>

      {excluded.length > 0 && (
        <div className="pms-hint" style={{ marginTop: 14 }}>
          <b>테일러링으로 제외된 문서 ({excluded.length}건)</b>
          {excluded.map((t) => (
            <div key={t.id}>
              • {t.name} <span className="muted">— {t.reason || '사유 미입력'}</span>
            </div>
          ))}
        </div>
      )}

      {form && (
        <UploadForm
          key={`${form.item.id}-${form.mode}`}
          projectId={projectId}
          item={form.item}
          doc={docOf(s, projectId, form.item.id)}
          mode={form.mode}
          onClose={() => setForm(null)}
        />
      )}
    </Modal>
  )
}
