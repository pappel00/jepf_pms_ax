import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Layout from './components/Layout'
import WorkspaceLayout from './components/WorkspaceLayout'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import CompaniesPage from './pages/CompaniesPage'
import DepartmentsPage from './pages/DepartmentsPage'
import UsersPage from './pages/UsersPage'
import RolesPage from './pages/RolesPage'
import UserGroupsPage from './pages/UserGroupsPage'
import PortalsPage from './pages/PortalsPage'
import ApplicationsPage from './pages/ApplicationsPage'
import PagesPage from './pages/PagesPage'
import MenusPage from './pages/MenusPage'
import DataServicesPage from './pages/DataServicesPage'
import MessageResourcesPage from './pages/MessageResourcesPage'
import CodesPage from './pages/CodesPage'
import FilesPage from './pages/FilesPage'
import TermsPage from './pages/TermsPage'
import PrivacyPolicyPage from './pages/PrivacyPolicyPage'
import NoticesPage from './pages/NoticesPage'
import FormsPage from './pages/FormsPage'
import Sample1Page from './pages/apps/STD_MGT/Sample1Page'
import Sample2Page from './pages/apps/STD_MGT/Sample2Page'

function ProtectedRoute({ children }) {
  const { auth } = useAuth()
  return auth ? children : <Navigate to="/login" replace />
}

function RootRedirect() {
  const { auth } = useAuth()
  return <Navigate to={auth ? '/dashboard' : '/login'} replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          <Route
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/companies" element={<CompaniesPage />} />
            <Route path="/departments" element={<DepartmentsPage />} />
            <Route path="/users" element={<UsersPage />} />
            <Route path="/roles" element={<RolesPage />} />
            <Route path="/user-groups" element={<UserGroupsPage />} />
            <Route path="/portals" element={<PortalsPage />} />
            <Route path="/applications" element={<ApplicationsPage />} />
            <Route path="/pages" element={<PagesPage />} />
            <Route path="/menus" element={<MenusPage />} />
            <Route path="/data-services" element={<DataServicesPage />} />
            <Route path="/message-resources" element={<MessageResourcesPage />} />
            <Route path="/codes" element={<CodesPage />} />
            <Route path="/files" element={<FilesPage />} />
            <Route path="/terms" element={<TermsPage />} />
            <Route path="/privacy-policy" element={<PrivacyPolicyPage />} />
            <Route path="/notices" element={<NoticesPage />} />
            <Route path="/forms" element={<FormsPage />} />
          </Route>

          {/*
            신규 운영 화면 셸(검증용) — Portal(BO)/Application(HOME, SYS_MGT)/Menu/Page DB
            데이터로 내비게이션을 구성한다(PortalBootstrapListener 참고). 위 정적 라우트와
            완전히 분리해서 병행 운영하며, 같은 화면 컴포넌트를 재사용한다(pageFile 값과 아래
            path가 반드시 일치해야 한다 — 새 관리 화면을 추가할 때 이 목록도 함께 갱신할 것).
            "home"은 HOME 어플리케이션 전용(기존 DashboardPage 재사용) — ApplicationRole
            매핑이 없어서 로그인한 사용자 누구나 접근 가능하다.
          */}
          <Route
            path="/workspace"
            element={
              <ProtectedRoute>
                <WorkspaceLayout />
              </ProtectedRoute>
            }
          >
            <Route
              index
              element={<p className="muted">왼쪽 메뉴에서 페이지를 선택하세요.</p>}
            />
            <Route path="home" element={<DashboardPage />} />
            <Route path="companies" element={<CompaniesPage />} />
            <Route path="departments" element={<DepartmentsPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="roles" element={<RolesPage />} />
            <Route path="user-groups" element={<UserGroupsPage />} />
            <Route path="portals" element={<PortalsPage />} />
            <Route path="applications" element={<ApplicationsPage />} />
            <Route path="pages" element={<PagesPage />} />
            <Route path="menus" element={<MenusPage />} />
            <Route path="data-services" element={<DataServicesPage />} />
            <Route path="message-resources" element={<MessageResourcesPage />} />
            <Route path="codes" element={<CodesPage />} />
            <Route path="files" element={<FilesPage />} />
            <Route path="terms" element={<TermsPage />} />
            <Route path="privacy-policy" element={<PrivacyPolicyPage />} />
            <Route path="notices" element={<NoticesPage />} />
            <Route path="forms" element={<FormsPage />} />

            {/*
              STD_MGT(표준관리) 예제 — "시스템 관리 외 다른 어플리케이션" 추가 패턴의 실제
              샘플이다. 화면 파일은 src/pages/apps/STD_MGT/ 밑에 두고, 라우트는 여기에
              /workspace/STD_MGT/<화면> 형태로 등록한다. PortalBootstrapListener의 AppSeed
              목록에 STD_MGT 항목이 대응된다 — 그쪽의 Page.pageFile 값과 아래 path가 반드시
              일치해야 한다.
            */}
            <Route path="STD_MGT/sample1" element={<Sample1Page />} />
            <Route path="STD_MGT/sample2" element={<Sample2Page />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
