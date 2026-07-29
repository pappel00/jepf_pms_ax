import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import Tabs from '../components/Tabs'
import MappingPanel from '../components/MappingPanel'
import { userGroupsApi, userGroupRolesApi, userGroupUsersApi, portalsApi, rolesApi, usersApi } from '../api/entities'
import { loadCodeOptions, codeLabel } from '../api/codeOptions'

// 분류(컨텐츠관리/시스템관리/데이터입력관리)의 선택지는 하드코딩하지 않고 공통코드
// (module-code)에서 불러온다 — 이 그룹코드는 UserGroupCodeBootstrapListener가 테넌트 생성 시
// 자동으로 만들어둔다. 코드 관리 화면에서 관리자가 옵션을 추가/변경할 수 있다.
// (페이지 관리의 용도/디바이스유형이 PAGE_* 코드그룹을 쓰는 것과 동일한 방식)
const USER_GROUP_CODE_GROUPS = {
  category: 'USER_GROUP_CATEGORY',
}

/**
 * 사용자그룹 관리: 검색조건 + 목록 + 선택한 그룹의 하단 탭(기본정보/권한(Role)/사용자).
 * 사진 속 BackOffice 화면 구조(검색조건 → 목록 → 하단 탭 상세)를 그대로 따른다 —
 * Role ↔ UserGroup ↔ User 매핑을 별도 라우트로 빼지 않고 한 화면에서 바로 관리한다.
 *
 * 분류는 공통코드에서 불러온 값으로 셀렉트를 채우고, 목록/상세에는 저장된 코드값을 그대로
 * 보여준다(코드/코드명이 같은 값이라 codeLabel()을 거쳐도 사실상 동일하지만, 나중에 코드명을
 * 따로 바꾸는 경우에도 화면이 자동으로 맞게 반영되도록 통일했다). 공통코드를 못 가져오면
 * 텍스트 입력으로 대체해서 화면이 막히지 않게 한다.
 */
export default function UserGroupsPage() {
  const [groups, setGroups] = useState([])
  const [portals, setPortals] = useState([])
  const [codeOptions, setCodeOptions] = useState({ category: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchCategory, setSearchCategory] = useState('ALL')
  const [searchUseYn, setSearchUseYn] = useState('ALL')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedCategory, setAppliedCategory] = useState('ALL')
  const [appliedUseYn, setAppliedUseYn] = useState('ALL')

  const [selectedGroup, setSelectedGroup] = useState(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ groupCode: '', groupName: '', category: '', portalId: '', defaultYn: 'N' })
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await userGroupsApi.list('?size=200')
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

  const loadCodes = useCallback(async () => {
    setCodeOptions(await loadCodeOptions(USER_GROUP_CODE_GROUPS))
  }, [])

  useEffect(() => {
    load()
    loadPortals()
    loadCodes()
  }, [load, loadPortals, loadCodes])

  const filteredGroups = groups.filter((g) => {
    const matchesKeyword =
      !appliedKeyword ||
      g.groupCode.toLowerCase().includes(appliedKeyword.toLowerCase()) ||
      g.groupName.toLowerCase().includes(appliedKeyword.toLowerCase())
    const matchesCategory = appliedCategory === 'ALL' || g.category === appliedCategory
    const matchesUseYn = appliedUseYn === 'ALL' || g.useYn === appliedUseYn
    return matchesKeyword && matchesCategory && matchesUseYn
  })

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedCategory(searchCategory)
    setAppliedUseYn(searchUseYn)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchCategory('ALL')
    setSearchUseYn('ALL')
    setAppliedKeyword('')
    setAppliedCategory('ALL')
    setAppliedUseYn('ALL')
  }

  const openCreate = () => {
    setEditing(null)
    setFormError(null)
    setForm({
      groupCode: '',
      groupName: '',
      // 공통코드의 첫 번째 값(sortOrder 순)을 기본 선택값으로 쓴다 — 하드코딩하지 않는다.
      category: codeOptions.category[0]?.value || '',
      portalId: '',
      defaultYn: 'N',
    })
    setModalOpen(true)
  }

  const openEdit = (group) => {
    setEditing(group)
    setFormError(null)
    setForm({
      groupCode: group.groupCode,
      groupName: group.groupName,
      category: group.category || '',
      portalId: group.portalId || '',
      defaultYn: group.defaultYn,
      useYn: group.useYn,
    })
    setModalOpen(true)
  }

  const handleDelete = async (group) => {
    if (!confirm(`${group.groupName} 그룹을 삭제하시겠습니까?`)) return
    try {
      await userGroupsApi.remove(group.id)
      if (selectedGroup?.id === group.id) setSelectedGroup(null)
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
        await userGroupsApi.update(editing.id, {
          groupName: form.groupName,
          category: form.category,
          portalId: form.portalId,
          useYn: form.useYn,
        })
      } else {
        await userGroupsApi.create(form)
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
    <div>
      <div className="page-header">
        <div>
          <h2>사용자그룹 관리</h2>
          <p className="page-description">
            Role ↔ 사용자그룹 ↔ 사용자 구조입니다. 그룹을 선택하면 하단 탭에서 권한/사용자 매핑을 바로 편집할 수 있습니다.
          </p>
        </div>
        <button className="primary small" onClick={openCreate}>
          + 사용자그룹 생성
        </button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>사용자그룹 검색</label>
          <input
            placeholder="그룹코드 또는 그룹명"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
        </div>
        <div className="search-field">
          <label>분류</label>
          <select value={searchCategory} onChange={(e) => setSearchCategory(e.target.value)}>
            <option value="ALL">전체</option>
            {codeOptions.category.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
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
              <th>그룹 코드</th>
              <th>그룹명</th>
              <th>포탈</th>
              <th>분류</th>
              <th>기본권한</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 120 }}>액션</th>
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
                <td>{portals.find((p) => p.id === group.portalId)?.portalName || '-'}</td>
                <td>{codeLabel(codeOptions.category, group.category)}</td>
                <td>{group.defaultYn}</td>
                <td>{group.useYn}</td>
                <td>{group.deletedYn}</td>
                <td>
                  <button
                    className="link-btn"
                    onClick={(e) => {
                      e.stopPropagation()
                      openEdit(group)
                    }}
                  >
                    수정
                  </button>
                  <button
                    className="link-btn danger"
                    onClick={(e) => {
                      e.stopPropagation()
                      handleDelete(group)
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

      {selectedGroup && (
        <UserGroupDetailSection
          group={selectedGroup}
          portals={portals}
          codeOptions={codeOptions}
          onEditBasic={() => openEdit(selectedGroup)}
        />
      )}

      {modalOpen && (
        <Modal title={editing ? '사용자그룹 수정' : '사용자그룹 생성'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            <div className="field">
              <label>그룹 코드</label>
              <input
                value={form.groupCode}
                disabled={!!editing}
                required
                onChange={(e) => setForm({ ...form, groupCode: e.target.value })}
              />
            </div>
            <div className="field">
              <label>그룹명</label>
              <input
                value={form.groupName}
                required
                onChange={(e) => setForm({ ...form, groupName: e.target.value })}
              />
            </div>
            <div className="field">
              <label>분류</label>
              {codeOptions.category.length === 0 ? (
                <input value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
              ) : (
                <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
                  <option value="">선택 안 함</option>
                  {/* 저장된 값이 코드에서 지워진 경우에도 셀렉트가 멋대로 바뀌지 않도록 남겨둔다. */}
                  {form.category && !codeOptions.category.some((o) => o.value === form.category) && (
                    <option value={form.category}>{form.category} (코드 없음)</option>
                  )}
                  {codeOptions.category.map((opt) => (
                    <option key={opt.value} value={opt.value}>
                      {opt.label}
                    </option>
                  ))}
                </select>
              )}
            </div>
            <div className="field">
              <label>포탈 (선택)</label>
              <select value={form.portalId} onChange={(e) => setForm({ ...form, portalId: e.target.value })}>
                <option value="">없음</option>
                {portals.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.portalName}
                  </option>
                ))}
              </select>
            </div>
            {!editing && (
              <div className="field">
                <label>
                  <input
                    type="checkbox"
                    checked={form.defaultYn === 'Y'}
                    onChange={(e) => setForm({ ...form, defaultYn: e.target.checked ? 'Y' : 'N' })}
                    style={{ marginRight: 6 }}
                  />
                  기본권한 그룹 (신규 가입자 자동 배정)
                </label>
              </div>
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

function UserGroupDetailSection({ group, portals, codeOptions, onEditBasic }) {
  const [activeTab, setActiveTab] = useState('roles')
  const portalName = (portalId) => portals.find((p) => p.id === portalId)?.portalName

  return (
    <div className="detail-section">
      <h3>
        {group.groupName} ({group.groupCode})
      </h3>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        tabs={[
          { key: 'info', label: '기본정보' },
          { key: 'roles', label: '권한 (Role)' },
          { key: 'users', label: '사용자' },
        ]}
      />

      <div className="tab-content">
        {activeTab === 'info' && (
          <div className="card" style={{ width: 'auto' }}>
            <div className="kv-row">
              <span className="k">그룹 코드</span>
              <span>{group.groupCode}</span>
            </div>
            <div className="kv-row">
              <span className="k">그룹명</span>
              <span>{group.groupName}</span>
            </div>
            <div className="kv-row">
              <span className="k">분류</span>
              <span>{codeLabel(codeOptions.category, group.category)}</span>
            </div>
            <div className="kv-row">
              <span className="k">포탈</span>
              <span>{portalName(group.portalId) || '-'}</span>
            </div>
            <div className="kv-row">
              <span className="k">기본권한</span>
              <span>{group.defaultYn}</span>
            </div>
            <div className="kv-row">
              <span className="k">사용여부</span>
              <span>{group.useYn}</span>
            </div>
            <button className="secondary" onClick={onEditBasic}>
              기본정보 수정
            </button>
          </div>
        )}

        {activeTab === 'roles' && (
          <MappingPanel
            title="이 그룹에 매핑된 Role"
            api={userGroupRolesApi(group.id)}
            addField={{
              name: 'roleCode',
              label: 'Role',
              optionsApi: rolesApi,
              optionsLabel: (r) => `${r.roleName} (${r.roleCode})`,
              optionsValue: (r) => r.roleCode,
            }}
            renderItem={(item) => item.roleCode}
          />
        )}

        {activeTab === 'users' && (
          <MappingPanel
            title="이 그룹에 속한 사용자"
            api={userGroupUsersApi(group.id)}
            addField={{
              name: 'userId',
              label: '사용자',
              optionsApi: usersApi,
              optionsLabel: (u) => `${u.loginId} (${u.userName})`,
            }}
            renderItem={(item) => `${item.loginId} (${item.userName})`}
            idField="id"
          />
        )}
      </div>
    </div>
  )
}
