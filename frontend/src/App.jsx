import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
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
import MyDashboardPage from './pages/apps/PMS/MyDashboardPage'
import WbsPage from './pages/apps/PMS/WbsPage'
import ProjectsPage from './pages/apps/PMS/ProjectsPage'
import RfpPage from './pages/apps/PMS/RfpPage'
import RfpEvalPage from './pages/apps/PMS/RfpEvalPage'
import RfpEvalAdminPage from './pages/apps/PMS/RfpEvalAdminPage'
import ProposalPage from './pages/apps/PMS/ProposalPage'
import DocTailoringPage from './pages/apps/PMS/DocTailoringPage'
import IssuesPage from './pages/apps/PMS/IssuesPage'
import ReportPage from './pages/apps/PMS/ReportPage'
import TransitionPage from './pages/apps/PMS/TransitionPage'
import ExecProjectPage from './pages/apps/PMS/ExecProjectPage'
import StaffingPage from './pages/apps/PMS/StaffingPage'
import StaffManagePage from './pages/apps/PMS/StaffManagePage'
import NextOppsPage from './pages/apps/PMS/NextOppsPage'
import OpsTasksPage from './pages/apps/PMS/OpsTasksPage'
import OpsIssuesPage from './pages/apps/PMS/OpsIssuesPage'
import OpsSalesPage from './pages/apps/PMS/OpsSalesPage'
import SalesNotesPage from './pages/apps/PMS/SalesNotesPage'
import CustomersPage from './pages/apps/PMS/CustomersPage'
import CustomerContactsPage from './pages/apps/PMS/CustomerContactsPage'
import RatesPage from './pages/apps/PMS/RatesPage'
import OpsTransitionPage from './pages/apps/PMS/OpsTransitionPage'
import OpsProjectPage from './pages/apps/PMS/OpsProjectPage'

function ProtectedRoute({ children }) {
  const { auth } = useAuth()
  return auth ? children : <Navigate to="/login" replace />
}

function RootRedirect() {
  const { auth } = useAuth()
  return <Navigate to={auth ? '/workspace' : '/login'} replace />
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<RootRedirect />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/*
            운영 화면 셸 — Portal(BO)/Application(HOME/SYS_MGT/STD_MGT)/Menu/Page DB 데이터로
            내비게이션을 구성한다(PortalBootstrapListener 참고). 예전엔 이 라우트들이 정적
            Layout.jsx 하위에 하드코딩돼 있었는데, WorkspaceLayout 검증이 끝나서 이제 이
            /workspace/* 라우트 트리가 유일한 관리 화면 진입점이다(pageFile 값과 아래 path가
            반드시 일치해야 한다 — 새 관리 화면을 추가할 때 이 목록도 함께 갱신할 것).
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

            {/*
              PMS 목업(화면설계 검토용, 더미 데이터) — docs/pms/01-요구사항-초안.md 6절의 "화면설계 →
              ERD" 순서에 따라 API 연동 없이 화면부터 만든다. PortalBootstrapListener의 PMS
              AppSeed(Page.pageFile)와 아래 path가 일치해야 한다.
            */}
            <Route path="PMS/customers" element={<CustomersPage />} />
            <Route path="PMS/customer-contacts" element={<CustomerContactsPage />} />
            <Route path="PMS/sales-notes" element={<SalesNotesPage />} />
            <Route path="PMS/projects" element={<ProjectsPage />} />
            <Route path="PMS/rfp" element={<RfpPage />} />
            <Route path="PMS/rfp-eval" element={<RfpEvalPage />} />
            <Route path="PMS/rfp-eval-admin" element={<RfpEvalAdminPage />} />
            <Route path="PMS/proposals" element={<ProposalPage />} />
            <Route path="PMS/phase-transition" element={<TransitionPage />} />
            <Route path="PMS/ops-transition" element={<OpsTransitionPage />} />
            <Route path="PMS/ops-project" element={<OpsProjectPage />} />
            <Route path="PMS/ops-tasks" element={<OpsTasksPage />} />
            <Route path="PMS/ops-issues" element={<OpsIssuesPage />} />
            <Route path="PMS/ops-sales" element={<OpsSalesPage />} />
            <Route path="PMS/exec-project" element={<ExecProjectPage />} />
            <Route path="PMS/staffing" element={<StaffingPage />} />
            <Route path="PMS/staff" element={<StaffManagePage />} />
            <Route path="PMS/rates" element={<RatesPage />} />
            <Route path="PMS/doc-tailoring" element={<DocTailoringPage />} />
            <Route path="PMS/dashboard" element={<MyDashboardPage />} />
            <Route path="PMS/wbs" element={<WbsPage />} />
            <Route path="PMS/next-opps" element={<NextOppsPage />} />
            <Route path="PMS/issues" element={<IssuesPage />} />
            <Route path="PMS/report-weekly" element={<ReportPage mode="week" />} />
            <Route path="PMS/report-monthly" element={<ReportPage mode="month" />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
