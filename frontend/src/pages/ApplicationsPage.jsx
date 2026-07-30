import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import MappingPanel from '../components/MappingPanel'
import { applicationsApi, applicationRolesApi, portalsApi, rolesApi } from '../api/entities'

const roleLabel = (r) => `${r.roleName} (${r.roleCode})`

/**
 * 어플리케이션 관리: 검색조건 + 목록 + 선택한 어플리케이션의 권한(Role) 매핑.
 * `CrudPage`(검색조건 + 목록 + 모달)만으로는 매핑 탭을 표현할 수 없어서 `PagesPage.jsx`처럼
 * 커스텀 화면으로 만들었다.
 *
 * 어플리케이션 1 : Role N — 하나의 어플리케이션에 여러 Role을 매핑할 수 있다. `Page`의 "권한"
 * 탭(`PageRole`)과 동일한 패턴의 매핑 테이블(`ApplicationRole`)을 쓴다. 매핑이 하나도 없으면
 * 로그인한 사용자 누구나 접근 가능하다고 본다(매핑을 깜빡한 새 어플리케이션이 기본적으로
 * 잠기는 것을 방지).
 */
export default function ApplicationsPage() {
  const [applications, setApplications] = useState([])
  const [portals, setPortals] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchPortalId, setSearchPortalId] = useState('ALL')
  const [searchUseYn, setSearchUseYn] = useState('ALL')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedPortalId, setAppliedPortalId] = useState('ALL')
  const [appliedUseYn, setAppliedUseYn] = useState('ALL')

  const [selectedApplication, setSelectedApplication] = useState(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({ applicationCode: '', applicationName: '', portalId: '', sortOrder: 0 })
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await applicationsApi.list('?size=200')
      setApplications(res.data || [])
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

  useEffect(() => {
    load()
    loadPortals()
  }, [load, loadPortals])

  const portalName = (portalId) => portals.find((p) => p.id === portalId)?.portalName || portalId

  const filteredApplications = applications
    .filter((a) => {
      const matchesKeyword =
        !appliedKeyword ||
        a.applicationCode.toLowerCase().includes(appliedKeyword.toLowerCase()) ||
        a.applicationName.toLowerCase().includes(appliedKeyword.toLowerCase())
      const matchesPortal = appliedPortalId === 'ALL' || a.portalId === appliedPortalId
      const matchesUseYn = appliedUseYn === 'ALL' || a.useYn === appliedUseYn
      return matchesKeyword && matchesPortal && matchesUseYn
    })
    .sort((a, b) => a.sortOrder - b.sortOrder)

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedPortalId(searchPortalId)
    setAppliedUseYn(searchUseYn)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchPortalId('ALL')
    setSearchUseYn('ALL')
    setAppliedKeyword('')
    setAppliedPortalId('ALL')
    setAppliedUseYn('ALL')
  }

  const openCreate = () => {
    setEditing(null)
    setFormError(null)
    setForm({ applicationCode: '', applicationName: '', portalId: portals[0]?.id || '', sortOrder: 0 })
    setModalOpen(true)
  }

  const openEdit = (application) => {
    setEditing(application)
    setFormError(null)
    setForm({
      applicationCode: application.applicationCode,
      applicationName: application.applicationName,
      portalId: application.portalId,
      sortOrder: application.sortOrder,
      useYn: application.useYn,
    })
    setModalOpen(true)
  }

  const handleDelete = async (application) => {
    if (!confirm(`${application.applicationName} 어플리케이션을 삭제하시겠습니까?`)) return
    try {
      await applicationsApi.remove(application.id)
      if (selectedApplication?.id === application.id) setSelectedApplication(null)
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleRestore = async (application) => {
    if (!confirm(`${application.applicationName} 어플리케이션을 복구하시겠습니까?`)) return
    try {
      await applicationsApi.restore(application.id)
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
      const sortOrder = Number(form.sortOrder) || 0
      if (editing) {
        await applicationsApi.update(editing.id, { applicationName: form.applicationName, sortOrder, useYn: form.useYn })
      } else {
        await applicationsApi.create({ ...form, sortOrder })
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
          <h2>어플리케이션 관리</h2>
          <p className="page-description">
            어플리케이션을 선택하면 하단에서 접근 가능한 권한(Role)을 매핑할 수 있습니다.
          </p>
        </div>
        <button className="primary small" onClick={openCreate} disabled={portals.length === 0}>
          + 어플리케이션 생성
        </button>
      </div>

      {portals.length === 0 && !loading && (
        <div className="error-box">
          등록된 포탈이 없습니다. 먼저 포탈 관리 화면에서 포탈을 만들어야 등록할 수 있습니다.
        </div>
      )}

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>어플리케이션 검색</label>
          <input
            placeholder="어플리케이션 코드 또는 이름"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
        </div>
        <div className="search-field">
          <label>포탈</label>
          <select value={searchPortalId} onChange={(e) => setSearchPortalId(e.target.value)}>
            <option value="ALL">전체</option>
            {portals.map((p) => (
              <option key={p.id} value={p.id}>
                {p.portalName}
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
        총 {filteredApplications.length}건
      </p>

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>어플리케이션 코드</th>
              <th>어플리케이션명</th>
              <th>포탈</th>
              <th>정렬 순서</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 120 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {filteredApplications.map((application) => (
              <tr
                key={application.id}
                className={selectedApplication?.id === application.id ? 'row-selected' : ''}
                onClick={() => setSelectedApplication(application)}
                style={{ cursor: 'pointer' }}
              >
                <td>{application.applicationCode}</td>
                <td>{application.applicationName}</td>
                <td>{portalName(application.portalId)}</td>
                <td>{application.sortOrder}</td>
                <td>{application.useYn}</td>
                <td>{application.deletedYn}</td>
                <td>
                  <button
                    className="link-btn"
                    onClick={(e) => {
                      e.stopPropagation()
                      openEdit(application)
                    }}
                  >
                    수정
                  </button>
                  {application.deletedYn === 'Y' ? (
                    <button
                      className="link-btn"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleRestore(application)
                      }}
                    >
                      복구
                    </button>
                  ) : (
                    <button
                      className="link-btn danger"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleDelete(application)
                      }}
                    >
                      삭제
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filteredApplications.length === 0 && (
              <tr>
                <td colSpan={7} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {selectedApplication && (
        <div className="detail-section">
          <h3>
            {selectedApplication.applicationName} ({selectedApplication.applicationCode})
          </h3>
          <MappingPanel
            title="이 어플리케이션에 접근 가능한 권한(Role) — 하나도 없으면 로그인한 사용자 누구나 접근 가능"
            api={applicationRolesApi(selectedApplication.id)}
            addField={{
              name: 'roleCode',
              label: 'Role',
              optionsApi: rolesApi,
              optionsLabel: roleLabel,
              optionsValue: (r) => r.roleCode,
            }}
            renderItem={(item) => item.roleCode}
          />
        </div>
      )}

      {modalOpen && (
        <Modal title={editing ? '어플리케이션 수정' : '어플리케이션 생성'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            <div className="field">
              <label>어플리케이션 코드</label>
              <input
                value={form.applicationCode}
                disabled={!!editing}
                required
                onChange={(e) => setForm({ ...form, applicationCode: e.target.value })}
              />
            </div>
            <div className="field">
              <label>어플리케이션명</label>
              <input
                value={form.applicationName}
                required
                onChange={(e) => setForm({ ...form, applicationName: e.target.value })}
              />
            </div>
            <div className="field">
              <label>포탈</label>
              {editing ? (
                <input value={portalName(form.portalId)} disabled />
              ) : (
                <select
                  value={form.portalId}
                  required
                  onChange={(e) => setForm({ ...form, portalId: e.target.value })}
                >
                  {portals.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.portalName}
                    </option>
                  ))}
                </select>
              )}
            </div>
            <div className="field">
              <label>정렬 순서</label>
              <input
                type="number"
                value={form.sortOrder}
                onChange={(e) => setForm({ ...form, sortOrder: e.target.value })}
              />
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
