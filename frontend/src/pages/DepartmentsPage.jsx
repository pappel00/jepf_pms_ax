import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import { departmentsApi, companiesApi } from '../api/entities'
import { useAuth } from '../context/AuthContext'

const EMPTY_FORM = { departmentCode: '', departmentName: '', parentDepartmentId: '', useYn: 'Y' }

/**
 * 부서 관리: 검색조건 + 목록. 다른 Data Management 화면들과 달리 마스터-디테일이 아니라
 * 단일 목록이지만, 검색조건 + 회사 정보 표시가 필요해서 CrudPage 대신 커스텀 화면으로 만들었다.
 *
 * ⚠️ 회사 <-> 부서 연결고리: 부서는 모든 엔티티가 자동으로 갖는 tenant_id로 이미 회사와
 * 연결돼 있다(로그인한 관리자의 회사 = tenant_id) — 수정은 못 하지만(테넌트 격리 구조상
 * 다른 회사로 옮기는 게 불가능하므로) 목록/폼에 항상 보이게 했다.
 *
 * ⚠️ "여러 회사가 동시에 보이면 관리하기 힘들 것 같다"는 우려가 있었는데, 이 시스템은
 * Hibernate 레벨에서 테넌트(회사) 격리를 강제하기 때문에 로그인한 관리자는 애초에 자기
 * 회사 데이터만 볼 수 있다(SYSTEM_ADMIN도 로그인 시점의 회사로 고정) — 여러 회사가 동시에
 * 섞여 보이는 상황 자체가 구조상 발생하지 않는다. 그래서 회사 필터는 따로 안 만들고, 항상
 * 고정된 값으로 표시만 한다.
 */
export default function DepartmentsPage() {
  const { auth } = useAuth()
  const [departments, setDepartments] = useState([])
  const [companyName, setCompanyName] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchUseYn, setSearchUseYn] = useState('ALL')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedUseYn, setAppliedUseYn] = useState('ALL')

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await departmentsApi.list('?size=200')
      setDepartments(res.data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!auth?.tenantId) return
    companiesApi
      .get(auth.tenantId)
      .then((res) => setCompanyName(res.data?.companyName || auth.tenantId))
      .catch(() => setCompanyName(auth.tenantId))
  }, [auth])

  const departmentName = (id) => (id ? departments.find((d) => d.id === id)?.departmentName || id : '-')

  const filteredDepartments = departments.filter((d) => {
    const matchesKeyword =
      !appliedKeyword ||
      d.departmentCode.toLowerCase().includes(appliedKeyword.toLowerCase()) ||
      d.departmentName.toLowerCase().includes(appliedKeyword.toLowerCase())
    const matchesUseYn = appliedUseYn === 'ALL' || d.useYn === appliedUseYn
    return matchesKeyword && matchesUseYn
  })

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedUseYn(searchUseYn)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchUseYn('ALL')
    setAppliedKeyword('')
    setAppliedUseYn('ALL')
  }

  const openCreate = () => {
    setEditing(null)
    setFormError(null)
    setForm(EMPTY_FORM)
    setModalOpen(true)
  }

  const openEdit = (department) => {
    setEditing(department)
    setFormError(null)
    setForm({
      departmentCode: department.departmentCode,
      departmentName: department.departmentName,
      parentDepartmentId: department.parentDepartmentId || '',
      useYn: department.useYn,
    })
    setModalOpen(true)
  }

  const handleDelete = async (department) => {
    if (!confirm(`${department.departmentName} 부서를 삭제하시겠습니까?`)) return
    try {
      await departmentsApi.remove(department.id)
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
        await departmentsApi.update(editing.id, {
          departmentName: form.departmentName,
          parentDepartmentId: form.parentDepartmentId,
          useYn: form.useYn,
        })
      } else {
        await departmentsApi.create({
          departmentCode: form.departmentCode,
          departmentName: form.departmentName,
          parentDepartmentId: form.parentDepartmentId,
        })
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
          <h2>부서 관리</h2>
          <p className="page-description">
            소속 회사: <strong>{companyName || '-'}</strong> (로그인한 관리자의 회사로 자동 범위 제한되며, 수정할 수 없습니다).
          </p>
        </div>
        <button className="primary small" onClick={openCreate}>
          + 새로 만들기
        </button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>부서 검색</label>
          <input
            placeholder="부서코드 또는 부서명"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
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
        총 {filteredDepartments.length}건
      </p>

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>부서 코드</th>
              <th>부서명</th>
              <th>회사</th>
              <th>상위 부서</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 120 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {filteredDepartments.map((department) => (
              <tr key={department.id}>
                <td>{department.departmentCode}</td>
                <td>{department.departmentName}</td>
                <td>{companyName}</td>
                <td>{departmentName(department.parentDepartmentId)}</td>
                <td>{department.useYn}</td>
                <td>{department.deletedYn}</td>
                <td>
                  <button className="link-btn" onClick={() => openEdit(department)}>
                    수정
                  </button>
                  <button className="link-btn danger" onClick={() => handleDelete(department)}>
                    삭제
                  </button>
                </td>
              </tr>
            ))}
            {filteredDepartments.length === 0 && (
              <tr>
                <td colSpan={7} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {modalOpen && (
        <Modal title={editing ? '부서 수정' : '부서 생성'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            <div className="field">
              <label>소속 회사</label>
              <input value={companyName} disabled />
            </div>
            <div className="field">
              <label>부서 코드</label>
              <input
                value={form.departmentCode}
                disabled={!!editing}
                required
                onChange={(e) => setForm({ ...form, departmentCode: e.target.value })}
              />
            </div>
            <div className="field">
              <label>부서명</label>
              <input
                value={form.departmentName}
                required
                onChange={(e) => setForm({ ...form, departmentName: e.target.value })}
              />
            </div>
            <div className="field">
              <label>상위 부서 (선택)</label>
              <select
                value={form.parentDepartmentId}
                onChange={(e) => setForm({ ...form, parentDepartmentId: e.target.value })}
              >
                <option value="">없음</option>
                {departments
                  .filter((d) => d.id !== editing?.id)
                  .map((d) => (
                    <option key={d.id} value={d.id}>
                      {d.departmentName} ({d.departmentCode})
                    </option>
                  ))}
              </select>
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
