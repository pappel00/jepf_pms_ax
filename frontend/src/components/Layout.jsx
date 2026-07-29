import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const NAV_SECTIONS = [
  {
    title: '조직 관리',
    items: [
      { to: '/companies', label: '회사 관리' },
      { to: '/departments', label: '부서 관리' },
    ],
  },
  {
    title: 'User Management',
    items: [
      { to: '/users', label: '사용자 관리' },
      { to: '/user-groups', label: '사용자그룹 관리' },
      { to: '/roles', label: 'Role 관리' },
    ],
  },
  {
    title: 'Portal Management',
    items: [
      { to: '/pages', label: '페이지 관리' },
      { to: '/portals', label: '포탈 관리' },
      { to: '/applications', label: '어플리케이션 관리' },
      { to: '/menus', label: '메뉴 관리' },
      { to: '/data-services', label: '데이터서비스 관리' },
      { to: '/message-resources', label: '메시지자원 관리' },
    ],
  },
  {
    title: 'Data Management',
    items: [
      { to: '/codes', label: '코드 관리' },
      { to: '/files', label: '첨부파일 그룹 관리' },
      { to: '/terms', label: '이용약관 관리' },
      { to: '/privacy-policy', label: '개인정보처리방침 관리' },
      { to: '/notices', label: '공지/동의서 관리' },
      { to: '/forms', label: '표준양식 관리' },
    ],
  },
]

export default function Layout() {
  const { auth, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="app-shell">
      <header className="app-topbar">
        <div className="app-topbar-title">JEPF Admin</div>
        <div className="app-topbar-user">
          <span>
            {auth?.loginId} ({auth?.roles?.join(', ') || '역할 없음'})
          </span>
          <button className="secondary small" onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      </header>

      <div className="app-body">
        <nav className="app-sidebar">
          {NAV_SECTIONS.map((section) => (
            <div className="sidebar-section" key={section.title}>
              <div className="sidebar-section-title">{section.title}</div>
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => 'sidebar-link' + (isActive ? ' active' : '')}
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
