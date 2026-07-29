import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function DashboardPage() {
  const { auth } = useAuth()

  if (!auth) {
    return null
  }

  return (
    <div>
      <div className="page-header">
        <h2>대시보드</h2>
      </div>

      <div className="card" style={{ width: 'auto', maxWidth: 480 }}>
        <div className="kv-row">
          <span className="k">아이디</span>
          <span>{auth.loginId}</span>
        </div>
        <div className="kv-row">
          <span className="k">사용자 ID</span>
          <span>{auth.userId}</span>
        </div>
        <div className="kv-row">
          <span className="k">테넌트 ID</span>
          <span>{auth.tenantId}</span>
        </div>
        <div className="kv-row">
          <span className="k">역할</span>
          <div className="roles-list">
            {auth.roles?.length ? (
              auth.roles.map((role) => (
                <span className="role-chip" key={role}>
                  {role}
                </span>
              ))
            ) : (
              <span className="muted">부여된 역할 없음</span>
            )}
          </div>
        </div>
      </div>

      <p style={{ marginTop: 24 }}>
        왼쪽 메뉴에서 관리 화면으로 이동하거나, <Link to="/user-groups">사용자그룹 관리</Link>에서
        내 계정의 역할 매핑을 확인해보세요.
      </p>
    </div>
  )
}
