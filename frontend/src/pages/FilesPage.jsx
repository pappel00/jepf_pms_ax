import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { fileGroupsApi, fileItemsApi, uploadFileItem, downloadFileItem } from '../api/entities'

/**
 * 첨부파일 그룹 관리: 첨부그룹(첨부기본) 목록 + 검색조건 + 선택한 그룹의 첨부항목을 같은 화면
 * 하단에서 관리한다 (사진 속 BackOffice "첨부파일 그룹 관리" 화면 구조 그대로 — 검색조건 →
 * 첨부그룹 목록 → 첨부항목). module-code의 코드 관리 화면과 동일한 패턴이다.
 *
 * 첨부항목은 두 가지 방식으로 등록할 수 있다: "파일 업로드"(실제 파일을 서버에 저장, 저장
 * 위치는 백엔드 application.yml의 framework.file.storage.base-path로 설정)와 "메타데이터만
 * 등록"(파일 경로를 직접 입력, 실제 파일 없이). 목록에서 "다운로드"를 누르면 실제 저장된
 * 파일을 받을 수 있다(메타데이터만 등록한 항목은 실제 파일이 없어 다운로드가 실패한다).
 *
 * 검색은 지금은 백엔드 쿼리 파라미터가 아니라 이미 불러온 목록을 클라이언트에서 필터링하는
 * 방식이다 — 코드 관리 화면과 동일한 이유(첨부그룹 개수가 많지 않을 거라는 전제).
 */
export default function FilesPage() {
  const [groups, setGroups] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchUseYn, setSearchUseYn] = useState('ALL')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedUseYn, setAppliedUseYn] = useState('ALL')

  const [selectedGroup, setSelectedGroup] = useState(null)

  const [groupModalOpen, setGroupModalOpen] = useState(false)
  const [editingGroup, setEditingGroup] = useState(null)
  const [groupForm, setGroupForm] = useState({ groupCode: '', groupName: '', description: '', useYn: 'Y' })
  const [groupFormError, setGroupFormError] = useState(null)
  const [savingGroup, setSavingGroup] = useState(false)

  const loadGroups = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await fileGroupsApi.list('?size=200')
      setGroups(res.data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadGroups()
  }, [loadGroups])

  const filteredGroups = groups.filter((g) => {
    const matchesKeyword =
      !appliedKeyword ||
      g.groupCode.toLowerCase().includes(appliedKeyword.toLowerCase()) ||
      g.groupName.toLowerCase().includes(appliedKeyword.toLowerCase())
    const matchesUseYn = appliedUseYn === 'ALL' || g.useYn === appliedUseYn
    return matchesKeyword && matchesUseYn
  })

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedUseYn(searchUseYn)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchUseYn('ALL')
    setAppliedKeyword('')
    setAppliedUseYn('ALL')
  }

  const openCreateGroup = () => {
    setEditingGroup(null)
    setGroupFormError(null)
    setGroupForm({ groupCode: '', groupName: '', description: '', useYn: 'Y' })
    setGroupModalOpen(true)
  }

  const openEditGroup = (group) => {
    setEditingGroup(group)
    setGroupFormError(null)
    setGroupForm({
      groupCode: group.groupCode,
      groupName: group.groupName,
      description: group.description || '',
      useYn: group.useYn,
    })
    setGroupModalOpen(true)
  }

  const handleDeleteGroup = async (group) => {
    if (!confirm(`${group.groupName} 첨부그룹을 삭제하시겠습니까? (하위 첨부항목도 함께 무의미해집니다)`)) return
    try {
      await fileGroupsApi.remove(group.id)
      if (selectedGroup?.id === group.id) setSelectedGroup(null)
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
        await fileGroupsApi.update(editingGroup.id, {
          groupName: groupForm.groupName,
          description: groupForm.description,
          useYn: groupForm.useYn,
        })
      } else {
        await fileGroupsApi.create(groupForm)
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
          <h2>첨부파일 그룹 관리</h2>
          <p className="page-description">첨부그룹(첨부기본)과 그 하위 첨부항목으로 구성됩니다. 그룹을 선택하면 하단에 첨부항목 목록이 나타납니다.</p>
        </div>
        <button className="primary small" onClick={openCreateGroup}>
          + 첨부그룹 생성
        </button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>첨부그룹 검색</label>
          <input
            placeholder="그룹코드 또는 그룹명"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
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
              <th>그룹코드</th>
              <th>그룹명</th>
              <th>설명</th>
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
                <td>{group.groupCode}</td>
                <td>{group.groupName}</td>
                <td>{group.description}</td>
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
                  <button
                    className="link-btn danger"
                    onClick={(e) => {
                      e.stopPropagation()
                      handleDeleteGroup(group)
                    }}
                  >
                    삭제
                  </button>
                </td>
              </tr>
            ))}
            {filteredGroups.length === 0 && (
              <tr>
                <td colSpan={6} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {selectedGroup && <FileItemSection group={selectedGroup} />}

      {groupModalOpen && (
        <Modal title={editingGroup ? '첨부그룹 수정' : '첨부그룹 생성'} onClose={() => setGroupModalOpen(false)}>
          <form onSubmit={handleSubmitGroup}>
            {groupFormError && <div className="error-box">{groupFormError}</div>}
            <div className="field">
              <label>그룹 코드</label>
              <input
                value={groupForm.groupCode}
                disabled={!!editingGroup}
                required
                onChange={(e) => setGroupForm({ ...groupForm, groupCode: e.target.value })}
              />
            </div>
            <div className="field">
              <label>그룹명</label>
              <input
                value={groupForm.groupName}
                required
                onChange={(e) => setGroupForm({ ...groupForm, groupName: e.target.value })}
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

/** 선택된 첨부그룹의 첨부항목 목록 + 생성/수정/삭제. */
function FileItemSection({ group }) {
  const api = fileItemsApi(group.id)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ fileName: '', filePath: '', fileSize: '', sortOrder: 0, description: '', useYn: 'Y' })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState(null)

  const [uploadModalOpen, setUploadModalOpen] = useState(false)
  const [uploadFile, setUploadFile] = useState(null)
  const [uploadDescription, setUploadDescription] = useState('')
  const [uploadSortOrder, setUploadSortOrder] = useState(0)
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

  const openCreate = () => {
    setEditing(null)
    setFormError(null)
    setForm({ fileName: '', filePath: '', fileSize: '', sortOrder: 0, description: '', useYn: 'Y' })
    setModalOpen(true)
  }

  const openUpload = () => {
    setUploadError(null)
    setUploadFile(null)
    setUploadDescription('')
    setUploadSortOrder(0)
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
      await uploadFileItem(group.id, uploadFile, { sortOrder: uploadSortOrder, description: uploadDescription })
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
      await downloadFileItem(group.id, item.id, item.fileName)
    } catch (err) {
      alert(err.message)
    } finally {
      setDownloadingId(null)
    }
  }

  const openEdit = (item) => {
    setEditing(item)
    setFormError(null)
    setForm({
      fileName: item.fileName,
      filePath: item.filePath,
      fileSize: item.fileSize ?? '',
      sortOrder: item.sortOrder,
      description: item.description || '',
      useYn: item.useYn,
    })
    setModalOpen(true)
  }

  const handleDelete = async (item) => {
    if (!confirm(`${item.fileName} 첨부항목을 삭제하시겠습니까?`)) return
    try {
      await api.remove(item.id)
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setFormError(null)
    try {
      const body = {
        fileName: form.fileName,
        filePath: form.filePath,
        fileSize: form.fileSize === '' ? null : Number(form.fileSize),
        sortOrder: Number(form.sortOrder) || 0,
        description: form.description,
      }
      if (editing) {
        await api.update(editing.id, { ...body, useYn: form.useYn })
      } else {
        await api.create(body)
      }
      setModalOpen(false)
      load()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="detail-section">
      <div className="page-header">
        <h3>첨부항목 — {group.groupName} ({group.groupCode})</h3>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="secondary small" onClick={openUpload}>
            ⬆ 파일 업로드
          </button>
          <button className="primary small" onClick={openCreate}>
            + 메타데이터만 등록
          </button>
        </div>
      </div>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>파일명</th>
              <th>경로</th>
              <th>크기(byte)</th>
              <th>순서</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 170 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.fileName}</td>
                <td>{item.filePath}</td>
                <td>{item.fileSize ?? '-'}</td>
                <td>{item.sortOrder}</td>
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
                  <button className="link-btn danger" onClick={() => handleDelete(item)}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
            {items.length === 0 && (
              <tr>
                <td colSpan={7} className="empty-cell">
                  등록된 첨부항목이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {uploadModalOpen && (
        <Modal title="파일 업로드" onClose={() => setUploadModalOpen(false)}>
          <form onSubmit={handleUpload}>
            {uploadError && <div className="error-box">{uploadError}</div>}
            <div className="field">
              <label>파일</label>
              <input type="file" required onChange={(e) => setUploadFile(e.target.files?.[0] || null)} />
            </div>
            <div className="field">
              <label>정렬 순서</label>
              <input
                type="number"
                value={uploadSortOrder}
                onChange={(e) => setUploadSortOrder(e.target.value)}
              />
            </div>
            <div className="field">
              <label>설명</label>
              <input value={uploadDescription} onChange={(e) => setUploadDescription(e.target.value)} />
            </div>
            <button className="primary" type="submit" disabled={uploading}>
              {uploading ? '업로드 중...' : '업로드'}
            </button>
          </form>
        </Modal>
      )}

      {modalOpen && (
        <Modal title={editing ? '첨부항목 수정' : '메타데이터만 등록'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            <div className="field">
              <label>파일명</label>
              <input
                value={form.fileName}
                required
                onChange={(e) => setForm({ ...form, fileName: e.target.value })}
              />
            </div>
            <div className="field">
              <label>파일 경로/URL</label>
              <input
                value={form.filePath}
                required
                onChange={(e) => setForm({ ...form, filePath: e.target.value })}
              />
            </div>
            <div className="field">
              <label>파일 크기 (byte, 선택)</label>
              <input
                type="number"
                value={form.fileSize}
                onChange={(e) => setForm({ ...form, fileSize: e.target.value })}
              />
            </div>
            <div className="field">
              <label>정렬 순서</label>
              <input
                type="number"
                value={form.sortOrder}
                onChange={(e) => setForm({ ...form, sortOrder: e.target.value })}
              />
            </div>
            <div className="field">
              <label>설명</label>
              <input
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
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
