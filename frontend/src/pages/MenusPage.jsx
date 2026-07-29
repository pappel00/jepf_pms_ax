import { useCallback, useEffect, useMemo, useState } from 'react'
import Modal from '../components/Modal'
import { menusApi, applicationsApi, pagesApi, portalsApi, menuPagesApi } from '../api/entities'

const EMPTY_CREATE_FORM = { menuCode: '', menuName: '', applicationId: '', parentMenuId: '', sortOrder: 0, linkUrl: '' }
const EMPTY_EDIT_FORM = { menuName: '', sortOrder: 0, linkUrl: '', useYn: 'Y' }

/**
 * 메뉴 관리: 사진 속 화면처럼 왼쪽에 페이지 검색/체크, 오른쪽에 포탈 > 어플리케이션을 고르면
 * 그 하위 폴더(=Menu) > 페이지 트리가 보인다. 오른쪽에서 폴더 하나를 선택하고 왼쪽에서
 * 페이지를 체크한 뒤 저장하면, 체크한 페이지들이 선택된 폴더 하위에 놓인다.
 *
 * ⚠️ 메뉴(폴더)와 페이지는 N:N이다 — 하나의 폴더에 여러 페이지를, 같은 페이지를 여러 폴더에
 * 동시에 놓을 수 있다. 예전엔 Menu.pageId로 1:N이었는데, 중간에 MenuPage 매핑 테이블을 두는
 * 구조로 바꿨다(module-portal 참고). 저장은 체크한 개수만큼 매핑 추가 API를 순차 호출한다 —
 * 별도 벌크 API는 안 만들고 다른 N:M 매핑들과 동일하게 건별 API로 통일했다.
 *
 * 폴더(Menu) 자체의 생성/수정/삭제는 트리의 인라인 액션으로 한다. applicationId/parentMenuId는
 * 생성 시에만 입력받고 수정 화면에는 없다(백엔드 UpdateRequest에 이 두 필드가 없음 — 구조를
 * 바꾸는 건 새 폴더를 만드는 것에 가깝다는 판단).
 */
export default function MenusPage() {
  const [menus, setMenus] = useState([])
  const [applications, setApplications] = useState([])
  const [pages, setPages] = useState([])
  const [portals, setPortals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [selectedPortalId, setSelectedPortalId] = useState('')
  const [selectedApplicationId, setSelectedApplicationId] = useState('')
  const [expandedMenus, setExpandedMenus] = useState(new Set())
  const [selectedMenuId, setSelectedMenuId] = useState('')
  const [menuPageMappings, setMenuPageMappings] = useState({})
  const [loadingMappings, setLoadingMappings] = useState(false)

  const [leftKeyword, setLeftKeyword] = useState('')
  const [checkedPageIds, setCheckedPageIds] = useState(new Set())
  const [savingMapping, setSavingMapping] = useState(false)
  const [mappingError, setMappingError] = useState(null)

  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE_FORM)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState(null)

  const [editing, setEditing] = useState(null)
  const [editForm, setEditForm] = useState(EMPTY_EDIT_FORM)
  const [saving, setSaving] = useState(false)
  const [editError, setEditError] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const [menusRes, appsRes, pagesRes, portalsRes] = await Promise.all([
        menusApi.list('?size=200'),
        applicationsApi.list('?size=200'),
        pagesApi.list('?size=200'),
        portalsApi.list('?size=200'),
      ])
      setMenus(menusRes.data || [])
      setApplications(appsRes.data || [])
      setPages(pagesRes.data || [])
      setPortals(portalsRes.data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  // 포탈 목록이 로드되면 첫 포탈을 기본 선택한다.
  useEffect(() => {
    if (portals.length > 0 && !selectedPortalId) {
      setSelectedPortalId(portals[0].id)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [portals])

  const applicationsOfSelectedPortal = useMemo(
    () => applications.filter((a) => a.portalId === selectedPortalId),
    [applications, selectedPortalId],
  )

  // 포탈이 바뀌면 그 포탈의 첫 어플리케이션을 기본 선택한다.
  useEffect(() => {
    if (applicationsOfSelectedPortal.length > 0) {
      setSelectedApplicationId(applicationsOfSelectedPortal[0].id)
    } else {
      setSelectedApplicationId('')
    }
    setSelectedMenuId('')
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedPortalId, applicationsOfSelectedPortal.length])

  const menusOfSelectedApp = useMemo(
    () => menus.filter((m) => m.applicationId === selectedApplicationId),
    [menus, selectedApplicationId],
  )

  const loadMappings = useCallback(async () => {
    if (menusOfSelectedApp.length === 0) {
      setMenuPageMappings({})
      return
    }
    setLoadingMappings(true)
    try {
      const entries = await Promise.all(
        menusOfSelectedApp.map(async (menu) => {
          try {
            const res = await menuPagesApi(menu.id).list()
            return [menu.id, res.data || []]
          } catch {
            return [menu.id, []]
          }
        }),
      )
      setMenuPageMappings(Object.fromEntries(entries))
    } finally {
      setLoadingMappings(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedApplicationId, menus])

  useEffect(() => {
    loadMappings()
  }, [loadMappings])

  // 어플리케이션이 바뀌면 포탈 노드를 기본으로 펼쳐둔다(트리가 한눈에 들어오게).
  useEffect(() => {
    setExpandedMenus(new Set(menusOfSelectedApp.filter((m) => !m.parentMenuId).map((m) => m.id)))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedApplicationId])

  const toggleExpand = (menuId) => {
    setExpandedMenus((prev) => {
      const next = new Set(prev)
      if (next.has(menuId)) next.delete(menuId)
      else next.add(menuId)
      return next
    })
  }

  const togglePageCheck = (pageId) => {
    setCheckedPageIds((prev) => {
      const next = new Set(prev)
      if (next.has(pageId)) next.delete(pageId)
      else next.add(pageId)
      return next
    })
  }

  const filteredLeftPages = pages.filter((p) => {
    if (!leftKeyword) return true
    return (
      p.pageCode.toLowerCase().includes(leftKeyword.toLowerCase()) ||
      p.pageName.toLowerCase().includes(leftKeyword.toLowerCase())
    )
  })

  const selectedMenu = menus.find((m) => m.id === selectedMenuId)

  const handleSaveMapping = async () => {
    if (!selectedMenuId || checkedPageIds.size === 0) return
    setSavingMapping(true)
    setMappingError(null)
    try {
      const existing = menuPageMappings[selectedMenuId] || []
      const existingPageIds = new Set(existing.map((m) => m.pageId))
      const toAdd = [...checkedPageIds].filter((pageId) => !existingPageIds.has(pageId))
      let nextSortOrder = existing.length
      for (const pageId of toAdd) {
        await menuPagesApi(selectedMenuId).add({ pageId, sortOrder: nextSortOrder })
        nextSortOrder += 1
      }
      setCheckedPageIds(new Set())
      await loadMappings()
    } catch (err) {
      setMappingError(err.message)
    } finally {
      setSavingMapping(false)
    }
  }

  const handleRemovePageFromMenu = async (menuId, mappingId) => {
    if (!confirm('이 페이지를 폴더에서 빼시겠습니까?')) return
    try {
      await menuPagesApi(menuId).remove(mappingId)
      loadMappings()
    } catch (err) {
      alert(err.message)
    }
  }

  const applicationName = (id) => applications.find((a) => a.id === id)?.applicationName || id
  const menuName = (id) => (id ? menus.find((m) => m.id === id)?.menuName || id : '-')
  const parentMenuOptions = menus.filter((m) => m.applicationId === createForm.applicationId)

  const openCreate = (presetApplicationId, presetParentMenuId) => {
    setCreateError(null)
    setCreateForm({
      ...EMPTY_CREATE_FORM,
      applicationId: presetApplicationId || selectedApplicationId || '',
      parentMenuId: presetParentMenuId || '',
    })
    setCreateModalOpen(true)
  }

  const handleCreateSubmit = async (e) => {
    e.preventDefault()
    setCreating(true)
    setCreateError(null)
    try {
      await menusApi.create({ ...createForm, sortOrder: Number(createForm.sortOrder) || 0 })
      setCreateModalOpen(false)
      load()
    } catch (err) {
      setCreateError(err.message)
    } finally {
      setCreating(false)
    }
  }

  const openEdit = (menu) => {
    setEditing(menu)
    setEditError(null)
    setEditForm({
      menuName: menu.menuName,
      sortOrder: menu.sortOrder,
      linkUrl: menu.linkUrl || '',
      useYn: menu.useYn,
    })
  }

  const handleDeleteFolder = async (menu) => {
    if (!confirm(`${menu.menuName} 폴더를 삭제하시겠습니까?`)) return
    try {
      await menusApi.remove(menu.id)
      if (selectedMenuId === menu.id) setSelectedMenuId('')
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleEditSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    setEditError(null)
    try {
      await menusApi.update(editing.id, { ...editForm, sortOrder: Number(editForm.sortOrder) || 0 })
      setEditing(null)
      load()
    } catch (err) {
      setEditError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const rootMenus = menusOfSelectedApp.filter((m) => !m.parentMenuId).sort((a, b) => a.sortOrder - b.sortOrder)
  const childMenusOf = (menuId) =>
    menusOfSelectedApp.filter((m) => m.parentMenuId === menuId).sort((a, b) => a.sortOrder - b.sortOrder)

  const renderMenuNode = (menu, depth) => {
    const children = childMenusOf(menu.id)
    const mappedPages = (menuPageMappings[menu.id] || [])
      .map((mapping) => ({ ...mapping, page: pages.find((p) => p.id === mapping.pageId) }))
      .filter((m) => m.page)
    const hasContent = children.length > 0 || mappedPages.length > 0
    const isExpanded = expandedMenus.has(menu.id)
    const isSelected = selectedMenuId === menu.id

    return (
      <div key={menu.id}>
        <div
          className={'tree-row' + (isSelected ? ' selected' : '')}
          style={{ paddingLeft: 8 + depth * 22 }}
          onClick={() => setSelectedMenuId(menu.id)}
        >
          <button
            className="tree-toggle"
            onClick={(e) => {
              e.stopPropagation()
              if (hasContent) toggleExpand(menu.id)
            }}
            style={{ visibility: hasContent ? 'visible' : 'hidden' }}
          >
            {isExpanded ? '▾' : '▸'}
          </button>
          <span className="tree-icon">📁</span>
          <span className="tree-label">{menu.menuName}</span>
          <span className="tree-meta">{menu.menuCode}</span>
          {menu.useYn === 'N' && <span className="tree-badge">미사용</span>}
          <span className="tree-actions">
            <button
              className="link-btn"
              onClick={(e) => {
                e.stopPropagation()
                openCreate(menu.applicationId, menu.id)
              }}
            >
              + 하위폴더
            </button>
            <button
              className="link-btn"
              onClick={(e) => {
                e.stopPropagation()
                openEdit(menu)
              }}
            >
              수정
            </button>
            <button
              className="link-btn danger"
              onClick={(e) => {
                e.stopPropagation()
                handleDeleteFolder(menu)
              }}
            >
              삭제
            </button>
          </span>
        </div>
        {isExpanded && (
          <div>
            {mappedPages.map((mapping) => (
              <div className="tree-row tree-page-row" key={mapping.id} style={{ paddingLeft: 8 + (depth + 1) * 22 }}>
                <span className="tree-toggle" style={{ visibility: 'hidden' }}>
                  ▸
                </span>
                <span className="tree-icon">📄</span>
                <span className="tree-label">{mapping.page.pageName}</span>
                <span className="tree-meta">{mapping.page.pageCode}</span>
                <span className="tree-actions">
                  <button className="link-btn danger" onClick={() => handleRemovePageFromMenu(menu.id, mapping.id)}>
                    제거
                  </button>
                </span>
              </div>
            ))}
            {children.map((child) => renderMenuNode(child, depth + 1))}
          </div>
        )}
      </div>
    )
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>메뉴 관리</h2>
          <p className="page-description">
            포탈 &gt; 어플리케이션을 고르면 하위 폴더(메뉴) &gt; 페이지 트리가 보입니다. 오른쪽에서 폴더를 선택하고
            왼쪽에서 페이지를 체크한 뒤 저장하면 폴더 하위에 페이지가 추가됩니다.
          </p>
        </div>
        <button className="primary small" onClick={() => openCreate()} disabled={!selectedApplicationId}>
          + 새 폴더
        </button>
      </div>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <div className="menu-split">
          <div className="menu-split-left">
            <h3>페이지 검색</h3>
            <div className="field">
              <input
                placeholder="페이지코드 또는 페이지명"
                value={leftKeyword}
                onChange={(e) => setLeftKeyword(e.target.value)}
              />
            </div>
            <div className="menu-page-picklist">
              {filteredLeftPages.map((page) => (
                <label className="menu-page-pick-row" key={page.id}>
                  <input
                    type="checkbox"
                    checked={checkedPageIds.has(page.id)}
                    onChange={() => togglePageCheck(page.id)}
                  />
                  <span className="tree-icon">📄</span>
                  <span className="tree-label">{page.pageName}</span>
                  <span className="tree-meta">{page.pageCode}</span>
                </label>
              ))}
              {filteredLeftPages.length === 0 && <p className="muted" style={{ padding: '8px 4px' }}>페이지가 없습니다.</p>}
            </div>
          </div>

          <div className="menu-split-right">
            <div className="menu-selectors">
              <div className="field" style={{ flex: 1, marginBottom: 0 }}>
                <label>포탈</label>
                <select value={selectedPortalId} onChange={(e) => setSelectedPortalId(e.target.value)}>
                  {portals.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.portalName}
                    </option>
                  ))}
                </select>
              </div>
              <div className="field" style={{ flex: 1, marginBottom: 0 }}>
                <label>어플리케이션</label>
                <select value={selectedApplicationId} onChange={(e) => setSelectedApplicationId(e.target.value)}>
                  {applicationsOfSelectedPortal.length === 0 && <option value="">없음</option>}
                  {applicationsOfSelectedPortal.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.applicationName}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {mappingError && <div className="error-box">{mappingError}</div>}

            <div className="menu-save-row">
              <span className="muted">
                {!selectedMenu
                  ? '오른쪽에서 폴더를 선택하세요'
                  : checkedPageIds.size === 0
                    ? `선택된 폴더: ${selectedMenu.menuName} · 왼쪽에서 페이지를 선택하세요`
                    : `선택된 폴더: ${selectedMenu.menuName} · 체크한 페이지 ${checkedPageIds.size}개`}
              </span>
              <button
                className="primary small"
                disabled={!selectedMenuId || checkedPageIds.size === 0 || savingMapping}
                onClick={handleSaveMapping}
              >
                {savingMapping ? '저장 중...' : '체크한 페이지를 폴더에 추가'}
              </button>
            </div>

            <div className="tree-panel">
              {loadingMappings ? (
                <p className="muted" style={{ padding: 12 }}>
                  불러오는 중...
                </p>
              ) : rootMenus.length === 0 ? (
                <p className="muted" style={{ padding: 12 }}>
                  등록된 폴더가 없습니다.
                </p>
              ) : (
                rootMenus.map((menu) => renderMenuNode(menu, 0))
              )}
            </div>
          </div>
        </div>
      )}

      {createModalOpen && (
        <Modal title="폴더 생성" onClose={() => setCreateModalOpen(false)}>
          <form onSubmit={handleCreateSubmit}>
            {createError && <div className="error-box">{createError}</div>}
            <div className="field">
              <label>폴더 코드</label>
              <input
                value={createForm.menuCode}
                required
                onChange={(e) => setCreateForm({ ...createForm, menuCode: e.target.value })}
              />
            </div>
            <div className="field">
              <label>폴더명</label>
              <input
                value={createForm.menuName}
                required
                onChange={(e) => setCreateForm({ ...createForm, menuName: e.target.value })}
              />
            </div>
            <div className="field">
              <label>어플리케이션</label>
              <select
                value={createForm.applicationId}
                required
                onChange={(e) => setCreateForm({ ...createForm, applicationId: e.target.value, parentMenuId: '' })}
              >
                <option value="">선택하세요</option>
                {applications.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.applicationName}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>상위 폴더 (선택)</label>
              <select
                value={createForm.parentMenuId}
                disabled={!createForm.applicationId}
                onChange={(e) => setCreateForm({ ...createForm, parentMenuId: e.target.value })}
              >
                <option value="">최상위 폴더</option>
                {parentMenuOptions.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.menuName}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>정렬 순서</label>
              <input
                type="number"
                value={createForm.sortOrder}
                onChange={(e) => setCreateForm({ ...createForm, sortOrder: e.target.value })}
              />
            </div>
            <div className="field">
              <label>외부 링크 URL (선택)</label>
              <input
                value={createForm.linkUrl}
                onChange={(e) => setCreateForm({ ...createForm, linkUrl: e.target.value })}
              />
            </div>
            <button className="primary" type="submit" disabled={creating}>
              {creating ? '생성 중...' : '생성'}
            </button>
          </form>
        </Modal>
      )}

      {editing && (
        <Modal title={`${editing.menuName} 수정`} onClose={() => setEditing(null)}>
          <form onSubmit={handleEditSubmit}>
            {editError && <div className="error-box">{editError}</div>}
            <p className="muted" style={{ marginTop: 0 }}>
              어플리케이션: {applicationName(editing.applicationId)} / 상위 폴더: {menuName(editing.parentMenuId)}
              (구조를 바꾸려면 폴더를 새로 만드세요)
            </p>
            <div className="field">
              <label>폴더명</label>
              <input
                value={editForm.menuName}
                required
                onChange={(e) => setEditForm({ ...editForm, menuName: e.target.value })}
              />
            </div>
            <div className="field">
              <label>정렬 순서</label>
              <input
                type="number"
                value={editForm.sortOrder}
                onChange={(e) => setEditForm({ ...editForm, sortOrder: e.target.value })}
              />
            </div>
            <div className="field">
              <label>외부 링크 URL (선택)</label>
              <input value={editForm.linkUrl} onChange={(e) => setEditForm({ ...editForm, linkUrl: e.target.value })} />
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
            <button className="primary" type="submit" disabled={saving}>
              {saving ? '저장 중...' : '저장'}
            </button>
          </form>
        </Modal>
      )}
    </div>
  )
}
