import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Layout from './components/Layout'
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
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
