import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { formGroupsApi, formVersionsApi, portalsApi, uploadFormVersion, downloadFormVersion } from '../api/entities'

/**
 * 표준양식 관리: 양식기본 목록 + 검색조건 + 선택한 양식의 버전(개정 이력)을 같은 화면 하단에서
 * 관리한다 (사진 속 BackOffice "표준양식 관리" 화면 구조 그대로 — 검색조건 → 양식 목록 →
 * 버전 이력). module-terms(이용약관)/module-privacy(개인정보처리방침)와 같은 2단 구조지만,
 * 버전이 텍스트가 아니라 **실제 엑셀/워드 파일**이라는 점이 다르다 — 관리자가 표준양식
 * 파일(.xlsx/.docx 등)을 그대로 업로드해서 버전별로 관리한다(module-file과 동일한 실제
 * 업로드/다운로드 방식, FormVersionSection 참고). 버전 정보(버전번호/시행일자/비고/사용여부)
 * 수정과 파일 업로드는 분리된 동작이다 — 파일을 바꾸려면 새 버전을 업로드해야 한다.
 *
 * category(분류)는 약관/개인정보처리방침에는 없던 필드다 — 표준양식은 보통 인사/총무/재무
 * 같은 부서·업무 분류가 있는 경우가 많아서 검색조건에도 추가했다.
 *
 * 검색은 지금은 백엔드 쿼리 파라미터(portalId는 백엔드가 지원하지만)가 아니라 이미 불러온
 * 목록을 클라이언트에서 필터링하는 방식이다 — 다른 Data Management 화면들과 동일한 이유.
 */
export default function FormsPage() {
  const [groups, setGroups] = useState([])
  const [portals, setPortals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchPortalId, setSearchPortalId] = useState('ALL')
  const [searchCategory, setSearchCategory] = useState('')
  const [searchUseYn, setSearchUseYn] = useState('ALL')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedPortalId, setAppliedPortalId] = useState('ALL')
  const [appliedCategory, setAppliedCategory] = useState('')
  const [appliedUseYn, setAppliedUseYn] = useState('ALL')

  const [selectedGroup, setSelectedGroup] = useState(null)

  const [groupModalOpen, setGroupModalOpen] = useState(false)
  const [editingGroup, setEditingGroup] = useState(null)
  const [groupForm, setGroupForm] = useState({ portalId: '', formCode: '', formName: '', category: '', description: '', useYn: 'Y' })
  const [groupFormError, setGroupFormError] = useState(null)
  const [savingGroup, setSavingGroup] = useState(false)

  const loadGroups = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await formGroupsApi.list('?size=200')
      setGroups(res.data || [])
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

  useEffect(() => {
    loadGroups()
    loadPortals()
  }, [loadGroups, loadPortals])

  const portalName = (portalId) => portals.find((p) => p.id === portalId)?.portalName || portalId

  const filteredGroups = groups.filter((g) => {
    const matchesKeyword =
      !appliedKeyword ||
      g.formCode.toLowerCase().includes(appliedKeyword.toLowerCase()) ||
      g.formName.toLowerCase().includes(appliedKeyword.toLowerCase())
    const matchesPortal = appliedPortalId === 'ALL' || g.portalId === appliedPortalId
    const matchesCategory = !appliedCategory || (g.category || '').includes(appliedCategory)
    const matchesUseYn = appliedUseYn === 'ALL' || g.useYn === appliedUseYn
    return matchesKeyword && matchesPortal && matchesCategory && matchesUseYn
  })

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedPortalId(searchPortalId)
    setAppliedCategory(searchCategory)
    setAppliedUseYn(searchUseYn)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchPortalId('ALL')
    setSearchCategory('')
    setSearchUseYn('ALL')
    setAppliedKeyword('')
    setAppliedPortalId('ALL')
    setAppliedCategory('')
    setAppliedUseYn('ALL')
  }

  const openCreateGroup = () => {
    setEditingGroup(null)
    setGroupFormError(null)
    setGroupForm({ portalId: portals[0]?.id || '', formCode: '', formName: '', category: '', description: '', useYn: 'Y' })
    setGroupModalOpen(true)
  }

  const openEditGroup = (group) => {
    setEditingGroup(group)
    setGroupFormError(null)
    setGroupForm({
      portalId: group.portalId,
      formCode: group.formCode,
      formName: group.formName,
      category: group.category || '',
      description: group.description || '',
      useYn: group.useYn,
    })
    setGroupModalOpen(true)
  }

  const handleDeleteGroup = async (group) => {
    if (!confirm(`${group.formName} 양식을 삭제하시겠습니까? (하위 버전 이력도 함께 무의미해집니다)`)) return
    try {
      await formGroupsApi.remove(group.id)
      if (selectedGroup?.id === group.id) setSelectedGroup(null)
      loadGroups()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleRestoreGroup = async (group) => {
    if (!confirm(`${group.formName} 양식을 복구하시겠습니까?`)) return
    try {
      await formGroupsApi.restore(group.id)
      loadGroups()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleSubmitGroup = async (e) => {
    e.preventDefault()
    setSavingGroup(true)
    setGroupFormError(null)
    try {
      if (editingGroup) {
        await formGroupsApi.update(editingGroup.id, {
          formName: groupForm.formName,
          category: groupForm.category,
          description: groupForm.description,
          useYn: groupForm.useYn,
        })
      } else {
        await formGroupsApi.create(groupForm)
      }
      setGroupModalOpen(false)
      loadGroups()
    } catch (err) {
      setGroupFormError(err.message)
    } finally {
      setSavingGroup(false)
    }
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>표준양식 관리</h2>
          <p className="page-description">포탈별로 관리됩니다. 양식을 선택하면 하단에 버전(개정 이력) 목록이 나타납니다.</p>
        </div>
        <button className="primary small" onClick={openCreateGroup} disabled={portals.length === 0}>
          + 양식 생성
        </button>
      </div>

      {portals.length === 0 && !loading && (
        <div className="error-box">
          등록된 포탈이 없습니다. 먼저 포탈 관리 화면에서 포탈을 만들어야 양식을 등록할 수 있습니다.
        </div>
      )}

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>양식 검색</label>
          <input
            placeholder="양식코드 또는 양식명"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
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
          <label>분류</label>
          <input placeholder="예: 인사" value={searchCategory} onChange={(e) => setSearchCategory(e.target.value)} />
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
        총 {filteredGroups.length}건
      </p>

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>포탈</th>
              <th>양식코드</th>
              <th>양식명</th>
              <th>분류</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 150 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {filteredGroups.map((group) => (
              <tr
                key={group.id}
                className={selectedGroup?.id === group.id ? 'row-selected' : ''}
                onClick={() => setSelectedGroup(group)}
                style={{ cursor: 'pointer' }}
              >
                <td>{portalName(group.portalId)}</td>
                <td>{group.formCode}</td>
                <td>{group.formName}</td>
                <td>{group.category}</td>
                <td>{group.useYn}</td>
                <td>{group.deletedYn}</td>
                <td>
                  <button
                    className="link-btn"
                    onClick={(e) => {
                      e.stopPropagation()
                      openEditGroup(group)
                    }}
                  >
                    수정
                  </button>
                  {group.deletedYn === 'Y' ? (
                    <button
                      className="link-btn"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleRestoreGroup(group)
                      }}
                    >
                      복구
                    </button>
                  ) : (
                    <button
                      className="link-btn danger"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleDeleteGroup(group)
                      }}
                    >
                      삭제
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filteredGroups.length === 0 && (
              <tr>
                <td colSpan={7} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {selectedGroup && <FormVersionSection group={selectedGroup} />}

      {groupModalOpen && (
        <Modal title={editingGroup ? '양식 수정' : '양식 생성'} onClose={() => setGroupModalOpen(false)}>
          <form onSubmit={handleSubmitGroup}>
            {groupFormError && <div className="error-box">{groupFormError}</div>}
            <div className="field">
              <label>포탈</label>
              {editingGroup ? (
                <input value={portalName(groupForm.portalId)} disabled />
              ) : (
                <select
                  value={groupForm.portalId}
                  required
                  onChange={(e) => setGroupForm({ ...groupForm, portalId: e.target.value })}
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
              <label>양식 코드</label>
              <input
                value={groupForm.formCode}
                disabled={!!editingGroup}
                required
                onChange={(e) => setGroupForm({ ...groupForm, formCode: e.target.value })}
              />
            </div>
            <div className="field">
              <label>양식명</label>
              <input
                value={groupForm.formName}
                required
                onChange={(e) => setGroupForm({ ...groupForm, formName: e.target.value })}
              />
            </div>
            <div className="field">
              <label>분류</label>
              <input
                placeholder="예: 인사"
                value={groupForm.category}
                onChange={(e) => setGroupForm({ ...groupForm, category: e.target.value })}
              />
            </div>
            <div className="field">
              <label>설명</label>
              <input
                value={groupForm.description}
                onChange={(e) => setGroupForm({ ...groupForm, description: e.target.value })}
              />
            </div>
            {editingGroup && (
              <div className="field">
                <label>
                  <input
                    type="checkbox"
                    checked={groupForm.useYn === 'Y'}
                    onChange={(e) => setGroupForm({ ...groupForm, useYn: e.target.checked ? 'Y' : 'N' })}
                    style={{ marginRight: 6 }}
                  />
                  사용여부
                </label>
              </div>
            )}
            <button className="primary" type="submit" disabled={savingGroup}>
              {savingGroup ? '저장 중...' : '저장'}
            </button>
          </form>
        </Modal>
      )}
    </div>
  )
}

/** 선택된 양식의 버전(개정 이력) 목록 + 생성/수정/삭제. */
function FormVersionSection({ group }) {
  const api = formVersionsApi(group.id)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // 메타데이터만 수정(파일 교체 없음) — 버전번호/시행일자/비고/사용여부만 바꾼다.
  const [editModalOpen, setEditModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [editForm, setEditForm] = useState({ versionNo: '', effectiveDate: '', description: '', useYn: 'Y' })
  const [savingEdit, setSavingEdit] = useState(false)
  const [editError, setEditError] = useState(null)

  // 새 버전 업로드 — 실제 엑셀/워드 파일을 올린다.
  const [uploadModalOpen, setUploadModalOpen] = useState(false)
  const [uploadFile, setUploadFile] = useState(null)
  const [uploadVersionNo, setUploadVersionNo] = useState('')
  const [uploadEffectiveDate, setUploadEffectiveDate] = useState('')
  const [uploadDescription, setUploadDescription] = useState('')
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState(null)

  const [downloadingId, setDownloadingId] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await api.list()
      setItems(res.data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }
  }, [group.id])

  useEffect(() => {
    load()
  }, [load])

  const openUpload = () => {
    setUploadError(null)
    setUploadFile(null)
    setUploadVersionNo('')
    setUploadEffectiveDate('')
    setUploadDescription('')
    setUploadModalOpen(true)
  }

  const handleUpload = async (e) => {
    e.preventDefault()
    if (!uploadFile) {
      setUploadError('업로드할 파일을 선택하세요.')
      return
    }
    setUploading(true)
    setUploadError(null)
    try {
      await uploadFormVersion(group.id, uploadFile, {
        versionNo: uploadVersionNo,
        effectiveDate: uploadEffectiveDate,
        description: uploadDescription,
      })
      setUploadModalOpen(false)
      load()
    } catch (err) {
      setUploadError(err.message)
    } finally {
      setUploading(false)
    }
  }

  const handleDownload = async (item) => {
    setDownloadingId(item.id)
    try {
      await downloadFormVersion(group.id, item.id, item.fileName)
    } catch (err) {
      alert(err.message)
    } finally {
      setDownloadingId(null)
    }
  }

  const openEdit = (item) => {
    setEditing(item)
    setEditError(null)
    setEditForm({
      versionNo: item.versionNo,
      effectiveDate: item.effectiveDate,
      description: item.description || '',
      useYn: item.useYn,
    })
    setEditModalOpen(true)
  }

  const handleDelete = async (item) => {
    if (!confirm(`버전 ${item.versionNo}(${item.fileName})을(를) 삭제하시겠습니까?`)) return
    try {
      await api.remove(item.id)
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleRestore = async (item) => {
    if (!confirm(`버전 ${item.versionNo}(${item.fileName})을(를) 복구하시겠습니까?`)) return
    try {
      await api.restore(item.id)
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleEditSubmit = async (e) => {
    e.preventDefault()
    setSavingEdit(true)
    setEditError(null)
    try {
      await api.update(editing.id, editForm)
      setEditModalOpen(false)
      load()
    } catch (err) {
      setEditError(err.message)
    } finally {
      setSavingEdit(false)
    }
  }

  return (
    <div className="detail-section">
      <div className="page-header">
        <h3>버전 이력 — {group.formName} ({group.formCode})</h3>
        <button className="primary small" onClick={openUpload}>
          ⬆ 새 버전 업로드
        </button>
      </div>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>버전</th>
              <th>파일명</th>
              <th>크기(byte)</th>
              <th>시행일자</th>
              <th>설명</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 170 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.versionNo}</td>
                <td>{item.fileName}</td>
                <td>{item.fileSize ?? '-'}</td>
                <td>{item.effectiveDate}</td>
                <td>{item.description}</td>
                <td>{item.useYn}</td>
                <td>{item.deletedYn}</td>
                <td>
                  <button
                    className="link-btn"
                    disabled={downloadingId === item.id}
                    onClick={() => handleDownload(item)}
                  >
                    {downloadingId === item.id ? '다운로드 중...' : '다운로드'}
                  </button>
                  <button className="link-btn" onClick={() => openEdit(item)}>
                    수정
                  </button>
                  {item.deletedYn === 'Y' ? (
                    <button className="link-btn" onClick={() => handleRestore(item)}>
                      복구
                    </button>
                  ) : (
                    <button className="link-btn danger" onClick={() => handleDelete(item)}>
                      삭제
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {items.length === 0 && (
              <tr>
                <td colSpan={8} className="empty-cell">
                  등록된 버전이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {uploadModalOpen && (
        <Modal title="새 버전 업로드" onClose={() => setUploadModalOpen(false)}>
          <form onSubmit={handleUpload}>
            {uploadError && <div className="error-box">{uploadError}</div>}
            <div className="field">
              <label>파일 (엑셀, 워드 등)</label>
              <input
                type="file"
                accept=".xlsx,.xls,.docx,.doc,.pdf"
                required
                onChange={(e) => setUploadFile(e.target.files?.[0] || null)}
              />
            </div>
            <div className="field">
              <label>버전 (예: 1.0)</label>
              <input value={uploadVersionNo} required onChange={(e) => setUploadVersionNo(e.target.value)} />
            </div>
            <div className="field">
              <label>시행일자</label>
              <input
                type="date"
                value={uploadEffectiveDate}
                required
                onChange={(e) => setUploadEffectiveDate(e.target.value)}
              />
            </div>
            <div className="field">
              <label>비고 (개정 사유 등)</label>
              <input value={uploadDescription} onChange={(e) => setUploadDescription(e.target.value)} />
            </div>
            <button className="primary" type="submit" disabled={uploading}>
              {uploading ? '업로드 중...' : '업로드'}
            </button>
          </form>
        </Modal>
      )}

      {editModalOpen && (
        <Modal title="버전 정보 수정" onClose={() => setEditModalOpen(false)}>
          <form onSubmit={handleEditSubmit}>
            {editError && <div className="error-box">{editError}</div>}
            <p className="muted" style={{ marginTop: 0 }}>
              파일: {editing?.fileName} (파일을 바꾸려면 새 버전을 업로드하세요)
            </p>
            <div className="field">
              <label>버전 (예: 1.0)</label>
              <input
                value={editForm.versionNo}
                required
                onChange={(e) => setEditForm({ ...editForm, versionNo: e.target.value })}
              />
            </div>
            <div className="field">
              <label>시행일자</label>
              <input
                type="date"
                value={editForm.effectiveDate}
                required
                onChange={(e) => setEditForm({ ...editForm, effectiveDate: e.target.value })}
              />
            </div>
            <div className="field">
              <label>비고 (개정 사유 등)</label>
              <input
                value={editForm.description}
                onChange={(e) => setEditForm({ ...editForm, description: e.target.value })}
              />
            </div>
            <div className="field">
              <label>
                <input
                  type="checkbox"
                  checked={editForm.useYn === 'Y'}
                  onChange={(e) => setEditForm({ ...editForm, useYn: e.target.checked ? 'Y' : 'N' })}
                  style={{ marginRight: 6 }}
                />
                사용여부
              </label>
            </div>
            <button className="primary" type="submit" disabled={savingEdit}>
              {savingEdit ? '저장 중...' : '저장'}
            </button>
          </form>
        </Modal>
      )}
    </div>
  )
}
