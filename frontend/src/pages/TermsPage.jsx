import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { termsGroupsApi, termsVersionsApi, portalsApi } from '../api/entities'

/**
 * 이용약관 관리: 약관기본 목록 + 검색조건 + 선택한 약관의 버전(개정 이력)을 같은 화면 하단에서
 * 관리한다 (사진 속 BackOffice "이용약관 관리" 화면 구조 그대로 — 검색조건 → 약관 목록 →
 * 버전 이력). module-code/module-file과 동일한 마스터-디테일 패턴이다.
 *
 * ⚠️ 이용약관은 포탈별로 관리된다 — 같은 서비스라도 웹/모바일 등 포탈(채널)마다 서로 다른
 * 약관 세트를 가질 수 있어야 한다는 요구사항에 따라, TermsGroup은 portalId를 필수로 갖는다
 * (module-terms 참고). 같은 약관코드라도 포탈이 다르면 별개의 약관으로 등록할 수 있다.
 * 생성 폼에서 포탈을 드롭다운으로 고르고(직접 포탈 관리 화면에서 만든 포탈 목록을 불러온다),
 * 한 번 만든 뒤에는 포탈을 바꿀 수 없다 — 다른 포탈로 옮기는 건 "새 약관을 만드는 것"에
 * 가깝고, 이미 쌓인 버전 이력의 맥락이 바뀌어버리기 때문(TermsGroupDto 주석 참고).
 *
 * 검색은 지금은 백엔드 쿼리 파라미터(portalId는 백엔드가 지원하지만)가 아니라 이미 불러온
 * 목록을 클라이언트에서 필터링하는 방식이다 — 코드/첨부파일 관리 화면과 동일한 이유(약관
 * 항목 개수가 많지 않을 거라는 전제), 나머지 검색 조건들과 필터링 방식을 통일하기 위함이다.
 */
export default function TermsPage() {
  const [groups, setGroups] = useState([])
  const [portals, setPortals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchPortalId, setSearchPortalId] = useState('ALL')
  const [searchTermsType, setSearchTermsType] = useState('ALL')
  const [searchUseYn, setSearchUseYn] = useState('ALL')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedPortalId, setAppliedPortalId] = useState('ALL')
  const [appliedTermsType, setAppliedTermsType] = useState('ALL')
  const [appliedUseYn, setAppliedUseYn] = useState('ALL')

  const [selectedGroup, setSelectedGroup] = useState(null)

  const [groupModalOpen, setGroupModalOpen] = useState(false)
  const [editingGroup, setEditingGroup] = useState(null)
  const [groupForm, setGroupForm] = useState({ portalId: '', termsCode: '', termsName: '', termsType: '필수', description: '', useYn: 'Y' })
  const [groupFormError, setGroupFormError] = useState(null)
  const [savingGroup, setSavingGroup] = useState(false)

  const loadGroups = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await termsGroupsApi.list('?size=200')
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
      // 포탈 목록을 못 가져와도 약관 화면 자체는 계속 쓸 수 있게 조용히 넘어간다.
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
      g.termsCode.toLowerCase().includes(appliedKeyword.toLowerCase()) ||
      g.termsName.toLowerCase().includes(appliedKeyword.toLowerCase())
    const matchesPortal = appliedPortalId === 'ALL' || g.portalId === appliedPortalId
    const matchesType = appliedTermsType === 'ALL' || g.termsType === appliedTermsType
    const matchesUseYn = appliedUseYn === 'ALL' || g.useYn === appliedUseYn
    return matchesKeyword && matchesPortal && matchesType && matchesUseYn
  })

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedPortalId(searchPortalId)
    setAppliedTermsType(searchTermsType)
    setAppliedUseYn(searchUseYn)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchPortalId('ALL')
    setSearchTermsType('ALL')
    setSearchUseYn('ALL')
    setAppliedKeyword('')
    setAppliedPortalId('ALL')
    setAppliedTermsType('ALL')
    setAppliedUseYn('ALL')
  }

  const openCreateGroup = () => {
    setEditingGroup(null)
    setGroupFormError(null)
    setGroupForm({ portalId: portals[0]?.id || '', termsCode: '', termsName: '', termsType: '필수', description: '', useYn: 'Y' })
    setGroupModalOpen(true)
  }

  const openEditGroup = (group) => {
    setEditingGroup(group)
    setGroupFormError(null)
    setGroupForm({
      portalId: group.portalId,
      termsCode: group.termsCode,
      termsName: group.termsName,
      termsType: group.termsType,
      description: group.description || '',
      useYn: group.useYn,
    })
    setGroupModalOpen(true)
  }

  const handleDeleteGroup = async (group) => {
    if (!confirm(`${group.termsName} 약관을 삭제하시겠습니까? (하위 버전 이력도 함께 무의미해집니다)`)) return
    try {
      await termsGroupsApi.remove(group.id)
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
        await termsGroupsApi.update(editingGroup.id, {
          termsName: groupForm.termsName,
          termsType: groupForm.termsType,
          description: groupForm.description,
          useYn: groupForm.useYn,
        })
      } else {
        await termsGroupsApi.create(groupForm)
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
          <h2>이용약관 관리</h2>
          <p className="page-description">포탈별로 관리됩니다. 약관을 선택하면 하단에 버전(개정 이력) 목록이 나타납니다.</p>
        </div>
        <button className="primary small" onClick={openCreateGroup} disabled={portals.length === 0}>
          + 약관 생성
        </button>
      </div>

      {portals.length === 0 && !loading && (
        <div className="error-box">
          등록된 포탈이 없습니다. 먼저 포탈 관리 화면에서 포탈을 만들어야 약관을 등록할 수 있습니다.
        </div>
      )}

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>약관 검색</label>
          <input
            placeholder="약관코드 또는 약관명"
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
          <label>약관유형</label>
          <select value={searchTermsType} onChange={(e) => setSearchTermsType(e.target.value)}>
            <option value="ALL">전체</option>
            <option value="필수">필수</option>
            <option value="선택">선택</option>
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
              <th>약관코드</th>
              <th>약관명</th>
              <th>약관유형</th>
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
                <td>{group.termsCode}</td>
                <td>{group.termsName}</td>
                <td>{group.termsType}</td>
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
                <td colSpan={8} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {selectedGroup && <TermsVersionSection group={selectedGroup} />}

      {groupModalOpen && (
        <Modal title={editingGroup ? '약관 수정' : '약관 생성'} onClose={() => setGroupModalOpen(false)}>
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
              <label>약관 코드</label>
              <input
                value={groupForm.termsCode}
                disabled={!!editingGroup}
                required
                onChange={(e) => setGroupForm({ ...groupForm, termsCode: e.target.value })}
              />
            </div>
            <div className="field">
              <label>약관명</label>
              <input
                value={groupForm.termsName}
                required
                onChange={(e) => setGroupForm({ ...groupForm, termsName: e.target.value })}
              />
            </div>
            <div className="field">
              <label>약관유형</label>
              <select
                value={groupForm.termsType}
                onChange={(e) => setGroupForm({ ...groupForm, termsType: e.target.value })}
              >
                <option value="필수">필수</option>
                <option value="선택">선택</option>
              </select>
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

/** 선택된 약관의 버전(개정 이력) 목록 + 생성/수정/삭제. */
function TermsVersionSection({ group }) {
  const api = termsVersionsApi(group.id)
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
        <h3>버전 이력 — {group.termsName} ({group.termsCode})</h3>
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
              <label>약관 내용</label>
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
