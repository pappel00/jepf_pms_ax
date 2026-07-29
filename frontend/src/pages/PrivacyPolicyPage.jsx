import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { privacyPolicyGroupsApi, privacyPolicyVersionsApi, portalsApi } from '../api/entities'

/**
 * 개인정보처리방침 관리: 방침기본 목록 + 검색조건 + 선택한 방침의 버전(개정 이력)을 같은 화면
 * 하단에서 관리한다 (사진 속 BackOffice "개인정보처리방침 관리" 화면 구조 그대로 — 검색조건 →
 * 방침 목록 → 버전 이력). module-terms(이용약관)와 완전히 동일한 구조다 — 개인정보처리방침도
 * 시점별 개정 이력을 추적해야 하기 때문(수집 당시 어떤 버전이 적용됐는지 증빙 필요).
 *
 * ⚠️ 이용약관과 달리 termsType(필수/선택) 같은 유형 구분 필드는 없다 — 개인정보처리방침은
 * 성격상 "필수/선택"으로 나눌 개념이 아니라서 뺐다.
 *
 * 검색은 지금은 백엔드 쿼리 파라미터(portalId는 백엔드가 지원하지만)가 아니라 이미 불러온
 * 목록을 클라이언트에서 필터링하는 방식이다 — 다른 Data Management 화면들과 동일한 이유.
 */
export default function PrivacyPolicyPage() {
  const [groups, setGroups] = useState([])
  const [portals, setPortals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchPortalId, setSearchPortalId] = useState('ALL')
  const [searchUseYn, setSearchUseYn] = useState('ALL')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedPortalId, setAppliedPortalId] = useState('ALL')
  const [appliedUseYn, setAppliedUseYn] = useState('ALL')

  const [selectedGroup, setSelectedGroup] = useState(null)

  const [groupModalOpen, setGroupModalOpen] = useState(false)
  const [editingGroup, setEditingGroup] = useState(null)
  const [groupForm, setGroupForm] = useState({ portalId: '', policyCode: '', policyName: '', description: '', useYn: 'Y' })
  const [groupFormError, setGroupFormError] = useState(null)
  const [savingGroup, setSavingGroup] = useState(false)

  const loadGroups = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await privacyPolicyGroupsApi.list('?size=200')
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
      g.policyCode.toLowerCase().includes(appliedKeyword.toLowerCase()) ||
      g.policyName.toLowerCase().includes(appliedKeyword.toLowerCase())
    const matchesPortal = appliedPortalId === 'ALL' || g.portalId === appliedPortalId
    const matchesUseYn = appliedUseYn === 'ALL' || g.useYn === appliedUseYn
    return matchesKeyword && matchesPortal && matchesUseYn
  })

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedPortalId(searchPortalId)
    setAppliedUseYn(searchUseYn)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchPortalId('ALL')
    setSearchUseYn('ALL')
    setAppliedKeyword('')
    setAppliedPortalId('ALL')
    setAppliedUseYn('ALL')
  }

  const openCreateGroup = () => {
    setEditingGroup(null)
    setGroupFormError(null)
    setGroupForm({ portalId: portals[0]?.id || '', policyCode: '', policyName: '', description: '', useYn: 'Y' })
    setGroupModalOpen(true)
  }

  const openEditGroup = (group) => {
    setEditingGroup(group)
    setGroupFormError(null)
    setGroupForm({
      portalId: group.portalId,
      policyCode: group.policyCode,
      policyName: group.policyName,
      description: group.description || '',
      useYn: group.useYn,
    })
    setGroupModalOpen(true)
  }

  const handleDeleteGroup = async (group) => {
    if (!confirm(`${group.policyName} 방침을 삭제하시겠습니까? (하위 버전 이력도 함께 무의미해집니다)`)) return
    try {
      await privacyPolicyGroupsApi.remove(group.id)
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
        await privacyPolicyGroupsApi.update(editingGroup.id, {
          policyName: groupForm.policyName,
          description: groupForm.description,
          useYn: groupForm.useYn,
        })
      } else {
        await privacyPolicyGroupsApi.create(groupForm)
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
          <h2>개인정보처리방침 관리</h2>
          <p className="page-description">포탈별로 관리됩니다. 방침을 선택하면 하단에 버전(개정 이력) 목록이 나타납니다.</p>
        </div>
        <button className="primary small" onClick={openCreateGroup} disabled={portals.length === 0}>
          + 방침 생성
        </button>
      </div>

      {portals.length === 0 && !loading && (
        <div className="error-box">
          등록된 포탈이 없습니다. 먼저 포탈 관리 화면에서 포탈을 만들어야 방침을 등록할 수 있습니다.
        </div>
      )}

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>방침 검색</label>
          <input
            placeholder="방침코드 또는 방침명"
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
              <th>방침코드</th>
              <th>방침명</th>
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
                <td>{portalName(group.portalId)}</td>
                <td>{group.policyCode}</td>
                <td>{group.policyName}</td>
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
                <td colSpan={7} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {selectedGroup && <PrivacyPolicyVersionSection group={selectedGroup} />}

      {groupModalOpen && (
        <Modal title={editingGroup ? '방침 수정' : '방침 생성'} onClose={() => setGroupModalOpen(false)}>
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
              <label>방침 코드</label>
              <input
                value={groupForm.policyCode}
                disabled={!!editingGroup}
                required
                onChange={(e) => setGroupForm({ ...groupForm, policyCode: e.target.value })}
              />
            </div>
            <div className="field">
              <label>방침명</label>
              <input
                value={groupForm.policyName}
                required
                onChange={(e) => setGroupForm({ ...groupForm, policyName: e.target.value })}
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

/** 선택된 방침의 버전(개정 이력) 목록 + 생성/수정/삭제. */
function PrivacyPolicyVersionSection({ group }) {
  const api = privacyPolicyVersionsApi(group.id)
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ versionNo: '', content: '', effectiveDate: '', description: '', useYn: 'Y' })
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
    setForm({ versionNo: '', content: '', effectiveDate: '', description: '', useYn: 'Y' })
    setModalOpen(true)
  }

  const openEdit = (item) => {
    setEditing(item)
    setFormError(null)
    setForm({
      versionNo: item.versionNo,
      content: item.content,
      effectiveDate: item.effectiveDate,
      description: item.description || '',
      useYn: item.useYn,
    })
    setModalOpen(true)
  }

  const handleDelete = async (item) => {
    if (!confirm(`버전 ${item.versionNo}을(를) 삭제하시겠습니까?`)) return
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
        versionNo: form.versionNo,
        content: form.content,
        effectiveDate: form.effectiveDate,
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
        <h3>버전 이력 — {group.policyName} ({group.policyCode})</h3>
        <button className="primary small" onClick={openCreate}>
          + 버전 추가
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
              <th>시행일자</th>
              <th>설명</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 120 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item) => (
              <tr key={item.id}>
                <td>{item.versionNo}</td>
                <td>{item.effectiveDate}</td>
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
                <td colSpan={6} className="empty-cell">
                  등록된 버전이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {modalOpen && (
        <Modal title={editing ? '버전 수정' : '버전 생성'} onClose={() => setModalOpen(false)} width={640}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            <div className="field">
              <label>버전 (예: 1.0)</label>
              <input
                value={form.versionNo}
                required
                onChange={(e) => setForm({ ...form, versionNo: e.target.value })}
              />
            </div>
            <div className="field">
              <label>시행일자</label>
              <input
                type="date"
                value={form.effectiveDate}
                required
                onChange={(e) => setForm({ ...form, effectiveDate: e.target.value })}
              />
            </div>
            <div className="field">
              <label>방침 내용</label>
              <textarea
                rows={10}
                value={form.content}
                required
                onChange={(e) => setForm({ ...form, content: e.target.value })}
                style={{ width: '100%', padding: '10px 12px', border: '1px solid #ddd', borderRadius: 6, fontSize: 14, fontFamily: 'inherit' }}
              />
            </div>
            <div className="field">
              <label>비고 (개정 사유 등)</label>
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
