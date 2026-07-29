import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { usersApi, departmentsApi, companiesApi } from '../api/entities'
import { useAuth } from '../context/AuthContext'

/**
 * 새 사용자는 회원가입(로그인 화면)으로도, 여기 관리 화면의 "생성" 버튼으로도 만들 수 있다
 * (UserController.create() — module-user 참고). 생성/수정 폼의 필드가 서로 달라서(비밀번호는
 * 생성 때만 입력) CrudPage를 그대로 쓰지 않고 커스텀 화면으로 만들었다.
 * 역할(roles)은 이 사용자가 속한 UserGroup들의 Role을 합친 값이라 여기서 직접 편집할 수
 * 없다 — 사용자그룹 관리 화면에서 그룹 매핑을 바꿔야 한다. 새로 생성된 사용자는 회원가입과
 * 동일하게 defaultYn=Y인 사용자그룹이 있으면 자동으로 배정된다.
 *
 * ⚠️ 회사 <-> 부서 <-> 사용자 연결고리: 회사는 모든 엔티티가 자동으로 갖는 tenant_id로 이미
 * 연결돼 있다(로그인한 관리자의 회사가 곧 tenant_id) — 화면엔 로그인한 관리자 자신의 회사
 * 하나만 항상 보인다(다른 회사 사용자는 테넌트 격리로 원래부터 안 보임). 부서는 이번에
 * departmentId 필드를 추가해서 연결했다(User.java 참고, 실제로 빠져있던 연결고리).
 */
const EMPTY_CREATE_FORM = { loginId: '', password: '', email: '', userName: '', departmentId: '' }
const EMPTY_EDIT_FORM = { email: '', userName: '', departmentId: '', useYn: 'Y' }

export default function UsersPage() {
  const { auth } = useAuth()
  const [rows, setRows] = useState([])
  const [departments, setDepartments] = useState([])
  const [companyName, setCompanyName] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE_FORM)
  const [creating, setCreating] = useState(false)
  const [createError, setCreateError] = useState(null)

  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_EDIT_FORM)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await usersApi.list('?size=200')
      setRows(res.data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  const loadDepartments = useCallback(async () => {
    try {
      const res = await departmentsApi.list('?size=200')
      setDepartments(res.data || [])
    } catch {
      // 부서 목록을 못 가져와도 화면 자체는 계속 쓸 수 있게 조용히 넘어간다.
    }
  }, [])

  const loadCompanyName = useCallback(async () => {
    if (!auth?.tenantId) return
    try {
      const res = await companiesApi.get(auth.tenantId)
      setCompanyName(res.data?.companyName || auth.tenantId)
    } catch {
      setCompanyName(auth.tenantId)
    }
  }, [auth])

  useEffect(() => {
    load()
    loadDepartments()
    loadCompanyName()
  }, [load, loadDepartments, loadCompanyName])

  const departmentName = (id) => (id ? departments.find((d) => d.id === id)?.departmentName || id : '-')

  const openCreate = () => {
    setCreateError(null)
    setCreateForm(EMPTY_CREATE_FORM)
    setCreateModalOpen(true)
  }

  const handleCreateSubmit = async (e) => {
    e.preventDefault()
    setCreating(true)
    setCreateError(null)
    try {
      await usersApi.create(createForm)
      setCreateModalOpen(false)
      load()
    } catch (err) {
      setCreateError(err.message)
    } finally {
      setCreating(false)
    }
  }

  const openEdit = (row) => {
    setEditing(row)
    setFormError(null)
    setForm({
      email: row.email || '',
      userName: row.userName || '',
      departmentId: row.departmentId || '',
      useYn: row.useYn || 'Y',
    })
  }

  const handleDelete = async (row) => {
    if (!confirm(`${row.loginId} 사용자를 삭제하시겠습니까?`)) return
    try {
      await usersApi.remove(row.id)
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
      await usersApi.update(editing.id, form)
      setEditing(null)
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
          <h2>사용자 관리</h2>
          <p className="page-description">
            소속 회사: <strong>{companyName || '-'}</strong> (로그인한 관리자의 회사로 자동 범위 제한됩니다).
            로그인 화면의 회원가입 또는 아래 "새로 만들기"로 사용자를 생성할 수 있습니다. 역할은 사용자그룹 관리에서 배정하세요.
          </p>
        </div>
        <button className="primary small" onClick={openCreate}>
          + 새로 만들기
        </button>
      </div>

      {error && <div className="error-box">{error}</div>}

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>아이디</th>
              <th>이름</th>
              <th>이메일</th>
              <th>부서</th>
              <th>역할</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 120 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id}>
                <td>{row.loginId}</td>
                <td>{row.userName}</td>
                <td>{row.email}</td>
                <td>{departmentName(row.departmentId)}</td>
                <td>
                  <div className="roles-list">
                    {row.roles?.length ? (
                      row.roles.map((r) => (
                        <span className="role-chip" key={r}>
                          {r}
                        </span>
                      ))
                    ) : (
                      <span className="muted">없음</span>
                    )}
                  </div>
                </td>
                <td>{row.useYn}</td>
                <td>{row.deletedYn}</td>
                <td>
                  <button className="link-btn" onClick={() => openEdit(row)}>
                    수정
                  </button>
                  <button className="link-btn danger" onClick={() => handleDelete(row)}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={8} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {createModalOpen && (
        <Modal title="사용자 생성" onClose={() => setCreateModalOpen(false)}>
          <form onSubmit={handleCreateSubmit}>
            {createError && <div className="error-box">{createError}</div>}
            <div className="field">
              <label>소속 회사</label>
              <input value={companyName} disabled />
            </div>
            <div className="field">
              <label>아이디</label>
              <input
                value={createForm.loginId}
                required
                onChange={(e) => setCreateForm({ ...createForm, loginId: e.target.value })}
              />
            </div>
            <div className="field">
              <label>비밀번호 (8자 이상)</label>
              <input
                type="password"
                minLength={8}
                value={createForm.password}
                required
                onChange={(e) => setCreateForm({ ...createForm, password: e.target.value })}
              />
            </div>
            <div className="field">
              <label>이름</label>
              <input
                value={createForm.userName}
                required
                onChange={(e) => setCreateForm({ ...createForm, userName: e.target.value })}
              />
            </div>
            <div className="field">
              <label>이메일</label>
              <input
                type="email"
                value={createForm.email}
                onChange={(e) => setCreateForm({ ...createForm, email: e.target.value })}
              />
            </div>
            <div className="field">
              <label>부서 (선택)</label>
              <select
                value={createForm.departmentId}
                onChange={(e) => setCreateForm({ ...createForm, departmentId: e.target.value })}
              >
                <option value="">없음</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.departmentName}
                  </option>
                ))}
              </select>
            </div>
            <button className="primary" type="submit" disabled={creating}>
              {creating ? '생성 중...' : '생성'}
            </button>
          </form>
        </Modal>
      )}

      {editing && (
        <Modal title={`${editing.loginId} 수정`} onClose={() => setEditing(null)}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            <div className="field">
              <label>소속 회사</label>
              <input value={companyName} disabled />
            </div>
            <div className="field">
              <label>이름</label>
              <input
                value={form.userName}
                onChange={(e) => setForm({ ...form, userName: e.target.value })}
                required
              />
            </div>
            <div className="field">
              <label>이메일</label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
              />
            </div>
            <div className="field">
              <label>부서 (선택)</label>
              <select value={form.departmentId} onChange={(e) => setForm({ ...form, departmentId: e.target.value })}>
                <option value="">없음</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.departmentName}
                  </option>
                ))}
              </select>
            </div>
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
            <button className="primary" type="submit" disabled={saving}>
              {saving ? '저장 중...' : '저장'}
            </button>
          </form>
        </Modal>
      )}
    </div>
  )
}
