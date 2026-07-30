import { useCallback, useEffect, useMemo, useState } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { portalsApi, applicationsApi, pagesApi, menuTree } from '../api/entities'

/**
 * 운영 화면 셸 — `/workspace/*` 하위 모든 관리 화면의 유일한 레이아웃이다(예전에 있던 정적
 * `Layout.jsx`/`NAV_SECTIONS` 사이드바는 검증이 끝나서 제거했다).
 *
 * 구성:
 *  1) 상단: 로그인한 사용자가 실제로 접근 가능한 어플리케이션만(`ApplicationRole` 기준),
 *     sortOrder 순으로 보여주는 스위처(`GET /api/v1/applications?accessibleOnly=true`).
 *  2) 좌측: 선택된 어플리케이션의 메뉴 트리(`GET /api/v1/menus/tree?applicationId=`) —
 *     이미 백엔드에서 PageRole 기준으로 접근 불가능한 페이지는 걸러진 상태로 내려온다.
 *  3) 메인: 트리에서 페이지를 클릭하면 그 페이지의 `pageFile`(이 프론트 안의 실제 라우트
 *     경로, 예: `/workspace/companies`)로 `navigate()`한다 — 별도 렌더러 없이 기존 라우트가
 *     그대로 `<Outlet/>`에 뜬다.
 *
 * 포탈은 지금은 `BO`(BackOffice) 하나로 고정한다 — 여러 포탈을 넘나드는 UI는 다음 단계 과제.
 */
const FIXED_PORTAL_CODE = 'BO'

/** 트리에서 가장 먼저 나오는 페이지를 찾는다(폴더 우선순위 → 그 안의 pageIds 순서 그대로). */
function findFirstPage(nodes, pageById) {
  for (const node of nodes) {
    const firstPageId = (node.pageIds || [])[0]
    if (firstPageId && pageById[firstPageId]) return pageById[firstPageId]
    const fromChild = findFirstPage(node.children || [], pageById)
    if (fromChild) return fromChild
  }
  return null
}

export default function WorkspaceLayout() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [portalId, setPortalId] = useState(null)
  const [applications, setApplications] = useState([])
  const [appsLoading, setAppsLoading] = useState(true)
  const [appsError, setAppsError] = useState(null)

  const [selectedApplicationId, setSelectedApplicationId] = useState('')

  const [pages, setPages] = useState([])
  const [tree, setTree] = useState([])
  const [treeLoading, setTreeLoading] = useState(false)
  const [treeError, setTreeError] = useState(null)
  const [expandedMenus, setExpandedMenus] = useState(new Set())

  const loadApplications = useCallback(async () => {
    setAppsLoading(true)
    setAppsError(null)
    try {
      const portalsRes = await portalsApi.list('?size=200')
      const boPortal = (portalsRes.data || []).find((p) => p.portalCode === FIXED_PORTAL_CODE)
      if (!boPortal) {
        setAppsError(`포탈(${FIXED_PORTAL_CODE})을 찾을 수 없습니다.`)
        setApplications([])
        return
      }
      setPortalId(boPortal.id)
      const appsRes = await applicationsApi.list(`?portalId=${boPortal.id}&accessibleOnly=true`)
      setApplications(appsRes.data || [])
    } catch (err) {
      setAppsError(err.message)
    } finally {
      setAppsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadApplications()
  }, [loadApplications])

  // 접근 가능한 어플리케이션 목록이 오면(이미 sortOrder 순) 첫 번째를 기본 선택한다.
  useEffect(() => {
    if (applications.length > 0 && !applications.some((a) => a.id === selectedApplicationId)) {
      setSelectedApplicationId(applications[0].id)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [applications])

  const loadTree = useCallback(async () => {
    if (!selectedApplicationId) {
      setTree([])
      return
    }
    setTreeLoading(true)
    setTreeError(null)
    try {
      const [treeRes, pagesRes] = await Promise.all([
        menuTree(selectedApplicationId),
        pagesApi.list('?size=200'),
      ])
      setTree(treeRes.data || [])
      setPages(pagesRes.data || [])
      setExpandedMenus(new Set((treeRes.data || []).map((node) => node.id)))
    } catch (err) {
      setTreeError(err.message)
    } finally {
      setTreeLoading(false)
    }
  }, [selectedApplicationId])

  useEffect(() => {
    loadTree()
  }, [loadTree])

  const pageById = useMemo(() => Object.fromEntries(pages.map((p) => [p.id, p])), [pages])

  // `/workspace`(정확히 인덱스 경로)로 진입했을 때만 첫 페이지로 자동 이동한다 — 딥링크나
  // 새로고침으로 특정 페이지(예: /workspace/roles)에 바로 온 경우는 그대로 유지한다. HOME
  // 어플리케이션이 sortOrder=0이라 기본 선택되므로, 결과적으로 /workspace 방문 시 HOME의
  // 첫 페이지(/workspace/home, DashboardPage)로 자동 이동하는 랜딩 화면 역할을 한다.
  useEffect(() => {
    if (location.pathname !== '/workspace' || treeLoading || tree.length === 0) return
    const firstPage = findFirstPage(tree, pageById)
    if (firstPage) navigate(firstPage.pageFile, { replace: true })
  }, [location.pathname, treeLoading, tree, pageById, navigate])

  const toggleExpand = (menuId) => {
    setExpandedMenus((prev) => {
      const next = new Set(prev)
      if (next.has(menuId)) next.delete(menuId)
      else next.add(menuId)
      return next
    })
  }

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  const renderTreeNode = (node, depth) => {
    const isExpanded = expandedMenus.has(node.id)
    const nodePages = (node.pageIds || []).map((id) => pageById[id]).filter(Boolean)
    const hasContent = node.children.length > 0 || nodePages.length > 0

    return (
      <div key={node.id}>
        <div className="tree-row" style={{ paddingLeft: 8 + depth * 18 }}>
          <button
            className="tree-toggle"
            onClick={() => hasContent && toggleExpand(node.id)}
            style={{ visibility: hasContent ? 'visible' : 'hidden' }}
          >
            {isExpanded ? '▾' : '▸'}
          </button>
          <span className="tree-icon">📁</span>
          <span className="tree-label">{node.menuName}</span>
        </div>
        {isExpanded && (
          <div>
            {nodePages.map((page) => (
              <div
                key={page.id}
                className={'tree-row workspace-menu-link' + (location.pathname === page.pageFile ? ' active' : '')}
                style={{ paddingLeft: 8 + (depth + 1) * 18 }}
                onClick={() => navigate(page.pageFile)}
              >
                <span className="tree-toggle" style={{ visibility: 'hidden' }}>
                  ▸
                </span>
                <span className="tree-icon">📄</span>
                <span className="tree-label">{page.pageName}</span>
              </div>
            ))}
            {node.children.map((child) => renderTreeNode(child, depth + 1))}
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="app-shell">
      <header className="app-topbar">
        <div className="app-topbar-title">JEPF Workspace</div>
        <div className="app-topbar-user">
          <span>
            {auth?.loginId} ({auth?.roles?.join(', ') || '역할 없음'})
          </span>
          <button className="secondary small" onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      </header>

      <nav className="workspace-app-switcher">
        {appsLoading && <span style={{ color: '#cdd2e0', fontSize: 13, padding: '6px 4px' }}>불러오는 중...</span>}
        {appsError && <span style={{ color: '#f2a6a6', fontSize: 13, padding: '6px 4px' }}>{appsError}</span>}
        {!appsLoading && !appsError && applications.length === 0 && (
          <span style={{ color: '#cdd2e0', fontSize: 13, padding: '6px 4px' }}>
            접근 가능한 어플리케이션이 없습니다.
          </span>
        )}
        {applications.map((app) => (
          <button
            key={app.id}
            className={'workspace-app-tab' + (app.id === selectedApplicationId ? ' active' : '')}
            onClick={() => setSelectedApplicationId(app.id)}
          >
            {app.applicationName}
          </button>
        ))}
      </nav>

      <div className="app-body">
        <div className="workspace-rail">
          {treeLoading ? (
            <p className="muted" style={{ padding: 12 }}>
              불러오는 중...
            </p>
          ) : treeError ? (
            <p className="muted" style={{ padding: 12 }}>
              {treeError}
            </p>
          ) : tree.length === 0 ? (
            <p className="muted" style={{ padding: 12 }}>
              메뉴가 없습니다.
            </p>
          ) : (
            tree.map((node) => renderTreeNode(node, 0))
          )}
        </div>

        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
