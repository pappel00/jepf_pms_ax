import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { codeGroupsApi, codeDetailsApi } from '../api/entities'

/**
 * 코드 관리: 코드그룹(코드기본) 목록 + 검색조건 + 선택한 그룹의 코드상세를 같은 화면 하단에서
 * 관리한다 (사진 속 BackOffice "코드 관리" 화면 구조 그대로 — 검색조건 → 목록 → 상세 패널).
 *
 * 검색은 지금은 백엔드 쿼리 파라미터가 아니라 이미 불러온 목록을 클라이언트에서 필터링하는
 * 방식이다 — 코드그룹 개수가 많지 않을 거라는 전제하에 우선 빠르게 화면을 갖췄다.
 */
export default function CodesPage() {
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
      const res = await codeGroupsApi.list('?size=200')
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
    setGroupForm({ groupCode: '', groupName: '', description: '' })
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
    if (!confirm(`${group.groupName} 코드그룹을 삭제하시겠습니까? (하위 코드상세도 함께 무의미해집니다)`)) return
    try {
      await codeGroupsApi.remove(group.id)
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
        await codeGroupsApi.update(editingGroup.id, {
          groupName: groupForm.groupName,
          description: groupForm.description,
          useYn: groupForm.useYn,
        })
      } else {
        await codeGroupsApi.create(groupForm)
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
          <h2>코드 관리</h2>
          <p className="page-description">코드기본(코드그룹)과 그 하위 코드상세로 구성됩니다. 그룹을 선택하면 하단에 상세 목록이 나타납니다.</p>
        </div>
        <button className="primary small" onClick={openCreateGroup}>
          + 코드그룹 생성
        </button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>코드그룹 검색</label>
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

      {selectedGroup && <CodeDetailSection group={selectedGroup} />}

      {groupModalOpen && (
        <Modal title={editingGroup ? '코드그룹 수정' : '코드그룹 생성'} onClose={() => setGroupModalOpen(false)}>
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

/** 선택된 코드그룹의 코드상세 목록 + 생성/수정/삭제. */
function CodeDetailSection({ group }) {
  const api = codeDetailsApi(group.id)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ code: '', codeName: '', sortOrder: 0, description: '', useYn: 'Y' })
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState(null)

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
    setForm({ code: '', codeName: '', sortOrder: 0, description: '' })
    setModalOpen(true)
  }

  const openEdit = (item) => {
    setEditing(item)
    setFormError(null)
    setForm({
      code: item.code,
      codeName: item.codeName,
      sortOrder: item.sortOrder,
      description: item.description || '',
      useYn: item.useYn,
    })
    setModalOpen(true)
  }

  const handleDelete = async (item) => {
    if (!confirm(`${item.codeName} 코드를 삭제하시겠습니까?`)) return
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
      if (editing) {
        await api.update(editing.id, {
          codeName: form.codeName,
          sortOrder: Number(form.sortOrder) || 0,
          description: form.description,
          useYn: form.useYn,
        })
      } else {
        await api.create({ ...form, sortOrder: Number(form.sortOrder) || 0 })
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
        <h3>코드상세 — {group.groupName} ({group.groupCode})</h3>
        <button className="primary small" onClick={openCreate}>
          + 코드 추가
        </button>
      </div>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>코드</th>
              <th>코드명</th>
              <th>순서</th>
              <th>설명</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 120 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.code}</td>
                <td>{item.codeName}</td>
                <td>{item.sortOrder}</td>
                <td>{item.description}</td>
                <td>{item.useYn}</td>
                <td>{item.deletedYn}</td>
                <td>
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
                  등록된 코드가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {modalOpen && (
        <Modal title={editing ? '코드 수정' : '코드 생성'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            <div className="field">
              <label>코드 값</label>
              <input
                value={form.code}
                disabled={!!editing}
                required
                onChange={(e) => setForm({ ...form, code: e.target.value })}
              />
            </div>
            <div className="field">
              <label>코드명</label>
              <input
                value={form.codeName}
                required
                onChange={(e) => setForm({ ...form, codeName: e.target.value })}
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
