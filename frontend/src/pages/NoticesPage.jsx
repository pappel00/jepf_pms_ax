import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { noticesApi, portalsApi } from '../api/entities'
import { loadCodeOptions } from '../api/codeOptions'

const EMPTY_FORM = {
  portalId: '',
  noticeType: '공지',
  title: '',
  content: '',
  startDate: '',
  endDate: '',
  popupYn: 'N',
  applyType: '',
  popupFrequency: '',
  popupSize: '',
  popupShape: '',
  popupPosition: '',
  useYn: 'Y',
}

// 팝업 상세 설정(적용유형/팝업빈도/크기/팝업모양/위치)의 선택지는 하드코딩하지 않고 공통코드
// (module-code)에서 불러온다 — 이 그룹코드들은 NoticeCodeBootstrapListener가 테넌트 생성 시
// 자동으로 만들어둔다. 코드 관리 화면에서 관리자가 옵션을 추가/변경할 수 있다.
const POPUP_CODE_GROUPS = {
  applyType: 'NOTICE_APPLY_TYPE',
  popupFrequency: 'NOTICE_POPUP_FREQUENCY',
  popupSize: 'NOTICE_POPUP_SIZE',
  popupShape: 'NOTICE_POPUP_SHAPE',
  popupPosition: 'NOTICE_POPUP_POSITION',
}

const POPUP_FIELD_LABELS = {
  applyType: '적용유형',
  popupFrequency: '팝업빈도',
  popupSize: '크기',
  popupShape: '팝업모양',
  popupPosition: '위치',
}

/**
 * 공지/동의서 관리: 검색조건 + 단일 목록. module-code/module-file/module-terms와 달리
 * 기본+상세(버전 이력) 2단 구조가 아니다 — 공지 하나하나가 제목/내용/게시기간을 통째로 갖는
 * 완결된 항목이라 개정 이력을 추적할 필요가 없다고 판단했다(약관과 다른 점).
 *
 * noticeType으로 "공지"와 "동의서"를 한 화면에서 같이 관리한다. 약관과 동일하게 포탈별로
 * 관리되며(portalId 필수), 생성 후에는 포탈을 바꿀 수 없다.
 *
 * 팝업 상세 설정(적용유형/팝업빈도/크기/팝업모양/위치)은 공통코드에서 불러온 값으로 셀렉트를
 * 채운다(POPUP_CODE_GROUPS 참고) — popupYn="Y"일 때만 의미가 있지만, 필드 자체는 항상 입력받는다.
 *
 * 검색은 지금은 백엔드 쿼리 파라미터(portalId는 백엔드가 지원하지만)가 아니라 이미 불러온
 * 목록을 클라이언트에서 필터링하는 방식이다 — 다른 Data Management 화면들과 동일한 이유.
 */
export default function NoticesPage() {
  const [notices, setNotices] = useState([])
  const [portals, setPortals] = useState([])
  const [popupCodeOptions, setPopupCodeOptions] = useState({
    applyType: [],
    popupFrequency: [],
    popupSize: [],
    popupShape: [],
    popupPosition: [],
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchPortalId, setSearchPortalId] = useState('ALL')
  const [searchNoticeType, setSearchNoticeType] = useState('ALL')
  const [searchUseYn, setSearchUseYn] = useState('ALL')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedPortalId, setAppliedPortalId] = useState('ALL')
  const [appliedNoticeType, setAppliedNoticeType] = useState('ALL')
  const [appliedUseYn, setAppliedUseYn] = useState('ALL')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)

  const loadNotices = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await noticesApi.list('?size=200')
      setNotices(res.data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  const loadPortals = useCallback(async () => {
    try {
      const res = await portalsApi.list('?size=200')
      setPortals(res.data || [])
    } catch {
      // 포탈 목록을 못 가져와도 화면 자체는 계속 쓸 수 있게 조용히 넘어간다.
    }
  }, [])

  // 공통코드를 못 가져와도 화면 자체는 계속 쓸 수 있게 loadCodeOptions()가 빈 배열을 돌려준다
  // (셀렉트가 비어있으면 아래 renderPopupCodeSelect()가 텍스트 입력으로 대체한다).
  const loadPopupCodeOptions = useCallback(async () => {
    setPopupCodeOptions(await loadCodeOptions(POPUP_CODE_GROUPS))
  }, [])

  useEffect(() => {
    loadNotices()
    loadPortals()
    loadPopupCodeOptions()
  }, [loadNotices, loadPortals, loadPopupCodeOptions])

  const portalName = (portalId) => portals.find((p) => p.id === portalId)?.portalName || portalId

  const filteredNotices = notices.filter((n) => {
    const matchesKeyword = !appliedKeyword || n.title.toLowerCase().includes(appliedKeyword.toLowerCase())
    const matchesPortal = appliedPortalId === 'ALL' || n.portalId === appliedPortalId
    const matchesType = appliedNoticeType === 'ALL' || n.noticeType === appliedNoticeType
    const matchesUseYn = appliedUseYn === 'ALL' || n.useYn === appliedUseYn
    return matchesKeyword && matchesPortal && matchesType && matchesUseYn
  })

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedPortalId(searchPortalId)
    setAppliedNoticeType(searchNoticeType)
    setAppliedUseYn(searchUseYn)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchPortalId('ALL')
    setSearchNoticeType('ALL')
    setSearchUseYn('ALL')
    setAppliedKeyword('')
    setAppliedPortalId('ALL')
    setAppliedNoticeType('ALL')
    setAppliedUseYn('ALL')
  }

  const openCreate = () => {
    setEditing(null)
    setFormError(null)
    setForm({
      ...EMPTY_FORM,
      portalId: portals[0]?.id || '',
      applyType: popupCodeOptions.applyType[0]?.value || '',
      popupFrequency: popupCodeOptions.popupFrequency[0]?.value || '',
      popupSize: popupCodeOptions.popupSize[0]?.value || '',
      popupShape: popupCodeOptions.popupShape[0]?.value || '',
      popupPosition: popupCodeOptions.popupPosition[0]?.value || '',
    })
    setModalOpen(true)
  }

  const openEdit = (notice) => {
    setEditing(notice)
    setFormError(null)
    setForm({
      portalId: notice.portalId,
      noticeType: notice.noticeType,
      title: notice.title,
      content: notice.content,
      startDate: notice.startDate,
      endDate: notice.endDate,
      popupYn: notice.popupYn,
      applyType: notice.applyType || '',
      popupFrequency: notice.popupFrequency || '',
      popupSize: notice.popupSize || '',
      popupShape: notice.popupShape || '',
      popupPosition: notice.popupPosition || '',
      useYn: notice.useYn,
    })
    setModalOpen(true)
  }

  const handleDelete = async (notice) => {
    if (!confirm(`${notice.title} 항목을 삭제하시겠습니까?`)) return
    try {
      await noticesApi.remove(notice.id)
      loadNotices()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleRestore = async (notice) => {
    if (!confirm(`${notice.title} 항목을 복구하시겠습니까?`)) return
    try {
      await noticesApi.restore(notice.id)
      loadNotices()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      const popupFields = {
        applyType: form.applyType,
        popupFrequency: form.popupFrequency,
        popupSize: form.popupSize,
        popupShape: form.popupShape,
        popupPosition: form.popupPosition,
      }
      if (editing) {
        await noticesApi.update(editing.id, {
          noticeType: form.noticeType,
          title: form.title,
          content: form.content,
          startDate: form.startDate,
          endDate: form.endDate,
          popupYn: form.popupYn,
          ...popupFields,
          useYn: form.useYn,
        })
      } else {
        await noticesApi.create({
          portalId: form.portalId,
          noticeType: form.noticeType,
          title: form.title,
          content: form.content,
          startDate: form.startDate,
          endDate: form.endDate,
          popupYn: form.popupYn,
          ...popupFields,
        })
      }
      setModalOpen(false)
      loadNotices()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const renderPopupCodeSelect = (fieldKey) => {
    const options = popupCodeOptions[fieldKey] || []
    return (
      <div className="field" key={fieldKey}>
        <label>{POPUP_FIELD_LABELS[fieldKey]}</label>
        {options.length === 0 ? (
          <input value={form[fieldKey]} onChange={(e) => setForm({ ...form, [fieldKey]: e.target.value })} />
        ) : (
          <select value={form[fieldKey]} onChange={(e) => setForm({ ...form, [fieldKey]: e.target.value })}>
            {options.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        )}
      </div>
    )
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>공지/동의서 관리</h2>
          <p className="page-description">포탈별로 관리됩니다. 공지와 동의서를 한 화면에서 같이 관리합니다.</p>
        </div>
        <button className="primary small" onClick={openCreate} disabled={portals.length === 0}>
          + 새로 만들기
        </button>
      </div>

      {portals.length === 0 && !loading && (
        <div className="error-box">
          등록된 포탈이 없습니다. 먼저 포탈 관리 화면에서 포탈을 만들어야 등록할 수 있습니다.
        </div>
      )}

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>제목 검색</label>
          <input placeholder="제목" value={searchKeyword} onChange={(e) => setSearchKeyword(e.target.value)} />
        </div>
        <div className="search-field">
          <label>포탈</label>
          <select value={searchPortalId} onChange={(e) => setSearchPortalId(e.target.value)}>
            <option value="ALL">전체</option>
            {portals.map((p) => (
              <option key={p.id} value={p.id}>
                {p.portalName}
              </option>
            ))}
          </select>
        </div>
        <div className="search-field">
          <label>구분</label>
          <select value={searchNoticeType} onChange={(e) => setSearchNoticeType(e.target.value)}>
            <option value="ALL">전체</option>
            <option value="공지">공지</option>
            <option value="동의서">동의서</option>
          </select>
        </div>
        <div className="search-field">
          <label>사용여부</label>
          <select value={searchUseYn} onChange={(e) => setSearchUseYn(e.target.value)}>
            <option value="ALL">전체</option>
            <option value="Y">사용</option>
            <option value="N">미사용</option>
          </select>
        </div>
        <div className="search-actions">
          <button type="button" className="secondary small" onClick={handleReset}>
            초기화
          </button>
          <button type="submit" className="primary small">
            검색
          </button>
        </div>
      </form>

      {error && <div className="error-box">{error}</div>}

      <p className="muted" style={{ margin: '4px 0 8px' }}>
        총 {filteredNotices.length}건
      </p>

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>포탈</th>
              <th>구분</th>
              <th>제목</th>
              <th>게시기간</th>
              <th>팝업</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 120 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {filteredNotices.map((notice) => (
              <tr key={notice.id}>
                <td>{portalName(notice.portalId)}</td>
                <td>{notice.noticeType}</td>
                <td>{notice.title}</td>
                <td>
                  {notice.startDate} ~ {notice.endDate}
                </td>
                <td>{notice.popupYn}</td>
                <td>{notice.useYn}</td>
                <td>{notice.deletedYn}</td>
                <td>
                  <button className="link-btn" onClick={() => openEdit(notice)}>
                    수정
                  </button>
                  {notice.deletedYn === 'Y' ? (
                    <button className="link-btn" onClick={() => handleRestore(notice)}>
                      복구
                    </button>
                  ) : (
                    <button className="link-btn danger" onClick={() => handleDelete(notice)}>
                      삭제
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filteredNotices.length === 0 && (
              <tr>
                <td colSpan={8} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {modalOpen && (
        <Modal title={editing ? '수정' : '새로 만들기'} onClose={() => setModalOpen(false)} width={640}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            <div className="field">
              <label>포탈</label>
              {editing ? (
                <input value={portalName(form.portalId)} disabled />
              ) : (
                <select
                  value={form.portalId}
                  required
                  onChange={(e) => setForm({ ...form, portalId: e.target.value })}
                >
                  {portals.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.portalName}
                    </option>
                  ))}
                </select>
              )}
            </div>
            <div className="field">
              <label>구분</label>
              <select value={form.noticeType} onChange={(e) => setForm({ ...form, noticeType: e.target.value })}>
                <option value="공지">공지</option>
                <option value="동의서">동의서</option>
              </select>
            </div>
            <div className="field">
              <label>제목</label>
              <input value={form.title} required onChange={(e) => setForm({ ...form, title: e.target.value })} />
            </div>
            <div className="field">
              <label>내용</label>
              <textarea
                rows={8}
                value={form.content}
                required
                onChange={(e) => setForm({ ...form, content: e.target.value })}
                style={{ width: '100%', padding: '10px 12px', border: '1px solid #ddd', borderRadius: 6, fontSize: 14, fontFamily: 'inherit' }}
              />
            </div>
            <div className="field">
              <label>게시 시작일</label>
              <input
                type="date"
                value={form.startDate}
                required
                onChange={(e) => setForm({ ...form, startDate: e.target.value })}
              />
            </div>
            <div className="field">
              <label>게시 종료일</label>
              <input
                type="date"
                value={form.endDate}
                required
                onChange={(e) => setForm({ ...form, endDate: e.target.value })}
              />
            </div>
            <div className="field">
              <label>
                <input
                  type="checkbox"
                  checked={form.popupYn === 'Y'}
                  onChange={(e) => setForm({ ...form, popupYn: e.target.checked ? 'Y' : 'N' })}
                  style={{ marginRight: 6 }}
                />
                팝업으로 노출
              </label>
            </div>

            {form.popupYn === 'Y' && (
              <>
                {renderPopupCodeSelect('applyType')}
                {renderPopupCodeSelect('popupFrequency')}
                {renderPopupCodeSelect('popupSize')}
                {renderPopupCodeSelect('popupShape')}
                {renderPopupCodeSelect('popupPosition')}
              </>
            )}

            {editing && (
              <div className="field">
                <label>
                  <input
                    type="checkbox"
                    checked={form.useYn === 'Y'}
                    onChange={(e) => setForm({ ...form, useYn: e.target.checked ? 'Y' : 'N' })}
                    style={{ marginRight: 6 }}
                  />
                  사용여부
                </label>
              </div>
            )}
            <button className="primary" type="submit" disabled={saving}>
              {saving ? '저장 중...' : '저장'}
            </button>
          </form>
        </Modal>
      )}
    </div>
  )
}
