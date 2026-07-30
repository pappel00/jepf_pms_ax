import { useCallback, useEffect, useState } from 'react'
import Modal from '../components/Modal'
import Tabs from '../components/Tabs'
import MappingPanel from '../components/MappingPanel'
import { pagesApi, pageRolesApi, pageDataServicesApi, pageMessageResourcesApi, rolesApi, dataServicesApi, messageResourcesApi } from '../api/entities'
import { loadCodeOptions, codeLabel } from '../api/codeOptions'

// 용도(일반/팝업)와 디바이스유형(PC/MOBILE/ALL)의 선택지는 하드코딩하지 않고 공통코드
// (module-code)에서 불러온다 — 이 그룹코드들은 PageCodeBootstrapListener가 테넌트 생성 시
// 자동으로 만들어둔다. 코드 관리 화면에서 관리자가 옵션을 추가/변경할 수 있다.
// (공지 팝업 상세 설정이 NOTICE_* 코드그룹을 쓰는 것과 동일한 방식)
const PAGE_CODE_GROUPS = {
  pageType: 'PAGE_TYPE',
  deviceType: 'PAGE_DEVICE_TYPE',
}

/**
 * 페이지 관리: 검색조건 + 목록 + 선택한 페이지의 하단 탭(기본정보/데이터서비스/메시지자원/권한(Role)).
 * 사진 속 BackOffice "페이지 관리" 화면 구조(검색조건 → 목록 → 하단 탭 상세: 페이지/데이터서비스/
 * 메시지자원/컴포넌트/파라미터/매뉴얼/권한)를 최대한 그대로 따랐다 — 컴포넌트/파라미터/매뉴얼 탭은
 * 아직 백엔드에 대응 엔티티가 없어서 이번엔 제외했다.
 *
 * 용도/디바이스유형은 공통코드에서 불러온 값으로 셀렉트를 채우고, 목록/상세에는 저장된 코드값
 * (예: "MOBILE") 대신 코드명(예: "모바일")을 보여준다. 공통코드를 못 가져오면(코드그룹이 지워진
 * 경우 등) 텍스트 입력으로 대체해서 화면이 막히지 않게 한다.
 */
export default function PagesPage() {
  const [pages, setPages] = useState([])
  const [codeOptions, setCodeOptions] = useState({ pageType: [], deviceType: [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchPageType, setSearchPageType] = useState('ALL')
  const [searchDeviceType, setSearchDeviceType] = useState('ALL')
  const [searchUseYn, setSearchUseYn] = useState('ALL')
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedPageType, setAppliedPageType] = useState('ALL')
  const [appliedDeviceType, setAppliedDeviceType] = useState('ALL')
  const [appliedUseYn, setAppliedUseYn] = useState('ALL')

  const [selectedPage, setSelectedPage] = useState(null)

  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({
    pageCode: '',
    pageName: '',
    pageType: '',
    deviceType: '',
    pageFile: '',
    messageResourceCode: '',
  })
  const [formError, setFormError] = useState(null)
  const [saving, setSaving] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await pagesApi.list('?size=200')
      setPages(res.data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [])

  const loadCodes = useCallback(async () => {
    setCodeOptions(await loadCodeOptions(PAGE_CODE_GROUPS))
  }, [])

  useEffect(() => {
    load()
    loadCodes()
  }, [load, loadCodes])

  const filteredPages = pages.filter((p) => {
    const matchesKeyword =
      !appliedKeyword ||
      p.pageCode.toLowerCase().includes(appliedKeyword.toLowerCase()) ||
      p.pageName.toLowerCase().includes(appliedKeyword.toLowerCase())
    const matchesPageType = appliedPageType === 'ALL' || p.pageType === appliedPageType
    const matchesDeviceType = appliedDeviceType === 'ALL' || p.deviceType === appliedDeviceType
    const matchesUseYn = appliedUseYn === 'ALL' || p.useYn === appliedUseYn
    return matchesKeyword && matchesPageType && matchesDeviceType && matchesUseYn
  })

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedPageType(searchPageType)
    setAppliedDeviceType(searchDeviceType)
    setAppliedUseYn(searchUseYn)
  }

  const handleReset = () => {
    setSearchKeyword('')
    setSearchPageType('ALL')
    setSearchDeviceType('ALL')
    setSearchUseYn('ALL')
    setAppliedKeyword('')
    setAppliedPageType('ALL')
    setAppliedDeviceType('ALL')
    setAppliedUseYn('ALL')
  }

  const openCreate = () => {
    setEditing(null)
    setFormError(null)
    setForm({
      pageCode: '',
      pageName: '',
      // 공통코드의 첫 번째 값(sortOrder 순)을 기본 선택값으로 쓴다 — 하드코딩하지 않는다.
      pageType: codeOptions.pageType[0]?.value || '',
      deviceType: codeOptions.deviceType[0]?.value || '',
      pageFile: '',
      messageResourceCode: '',
    })
    setModalOpen(true)
  }

  const openEdit = (page) => {
    setEditing(page)
    setFormError(null)
    setForm({
      pageCode: page.pageCode,
      pageName: page.pageName,
      pageType: page.pageType,
      deviceType: page.deviceType,
      pageFile: page.pageFile || '',
      messageResourceCode: page.messageResourceCode || '',
      useYn: page.useYn,
    })
    setModalOpen(true)
  }

  const handleDelete = async (page) => {
    if (!confirm(`${page.pageName} 페이지를 삭제하시겠습니까?`)) return
    try {
      await pagesApi.remove(page.id)
      if (selectedPage?.id === page.id) setSelectedPage(null)
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  const handleRestore = async (page) => {
    if (!confirm(`${page.pageName} 페이지를 복구하시겠습니까?`)) return
    try {
      await pagesApi.restore(page.id)
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
        await pagesApi.update(editing.id, {
          pageName: form.pageName,
          pageType: form.pageType,
          deviceType: form.deviceType,
          pageFile: form.pageFile,
          messageResourceCode: form.messageResourceCode,
          useYn: form.useYn,
        })
      } else {
        await pagesApi.create(form)
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
          <h2>페이지 관리</h2>
          <p className="page-description">
            페이지를 선택하면 하단 탭에서 데이터서비스/메시지자원/권한 매핑을 바로 편집할 수 있습니다.
          </p>
        </div>
        <button className="primary small" onClick={openCreate}>
          + 페이지 생성
        </button>
      </div>

      <form className="search-bar" onSubmit={handleSearch}>
        <div className="search-field">
          <label>페이지 검색</label>
          <input
            placeholder="페이지코드 또는 페이지명"
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
          />
        </div>
        <div className="search-field">
          <label>용도</label>
          <select value={searchPageType} onChange={(e) => setSearchPageType(e.target.value)}>
            <option value="ALL">전체</option>
            {codeOptions.pageType.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
        <div className="search-field">
          <label>디바이스유형</label>
          <select value={searchDeviceType} onChange={(e) => setSearchDeviceType(e.target.value)}>
            <option value="ALL">전체</option>
            {codeOptions.deviceType.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
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
        총 {filteredPages.length}건
      </p>

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>페이지 코드</th>
              <th>페이지명</th>
              <th>용도</th>
              <th>디바이스유형</th>
              <th>사용여부</th>
              <th>삭제여부</th>
              <th style={{ width: 120 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {filteredPages.map((page) => (
              <tr
                key={page.id}
                className={selectedPage?.id === page.id ? 'row-selected' : ''}
                onClick={() => setSelectedPage(page)}
                style={{ cursor: 'pointer' }}
              >
                <td>{page.pageCode}</td>
                <td>{page.pageName}</td>
                <td>{codeLabel(codeOptions.pageType, page.pageType)}</td>
                <td>{codeLabel(codeOptions.deviceType, page.deviceType)}</td>
                <td>{page.useYn}</td>
                <td>{page.deletedYn}</td>
                <td>
                  <button
                    className="link-btn"
                    onClick={(e) => {
                      e.stopPropagation()
                      openEdit(page)
                    }}
                  >
                    수정
                  </button>
                  {page.deletedYn === 'Y' ? (
                    <button
                      className="link-btn"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleRestore(page)
                      }}
                    >
                      복구
                    </button>
                  ) : (
                    <button
                      className="link-btn danger"
                      onClick={(e) => {
                        e.stopPropagation()
                        handleDelete(page)
                      }}
                    >
                      삭제
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filteredPages.length === 0 && (
              <tr>
                <td colSpan={7} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {selectedPage && (
        <PageDetailSection
          page={selectedPage}
          codeOptions={codeOptions}
          onEditBasic={() => openEdit(selectedPage)}
        />
      )}

      {modalOpen && (
        <Modal title={editing ? '페이지 수정' : '페이지 생성'} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            <div className="field">
              <label>페이지 코드</label>
              <input
                value={form.pageCode}
                disabled={!!editing}
                required
                onChange={(e) => setForm({ ...form, pageCode: e.target.value })}
              />
            </div>
            <div className="field">
              <label>페이지명</label>
              <input
                value={form.pageName}
                required
                onChange={(e) => setForm({ ...form, pageName: e.target.value })}
              />
            </div>
            <CodeSelectField
              label="용도"
              options={codeOptions.pageType}
              value={form.pageType}
              onChange={(value) => setForm({ ...form, pageType: value })}
            />
            <CodeSelectField
              label="디바이스유형"
              options={codeOptions.deviceType}
              value={form.deviceType}
              onChange={(value) => setForm({ ...form, deviceType: value })}
            />
            <p className="muted" style={{ margin: '-4px 0 12px', fontSize: 12 }}>
              용도/디바이스유형 선택지는 코드 관리 화면의 <code>PAGE_TYPE</code> /{' '}
              <code>PAGE_DEVICE_TYPE</code> 코드그룹에서 추가·변경할 수 있습니다.
            </p>
            <div className="field">
              <label>페이지 파일 경로</label>
              <input value={form.pageFile} onChange={(e) => setForm({ ...form, pageFile: e.target.value })} />
            </div>
            <div className="field">
              <label>메시지자원 코드 (표시명 다국어용)</label>
              <input
                value={form.messageResourceCode}
                onChange={(e) => setForm({ ...form, messageResourceCode: e.target.value })}
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

/**
 * 공통코드로 채워지는 셀렉트 하나. 코드그룹이 비어있거나 못 불러온 경우엔 텍스트 입력으로
 * 대체한다 — 공통코드를 못 가져왔다고 페이지 생성 자체가 막히면 안 되기 때문이다
 * (NoticesPage의 팝업 설정 필드와 동일한 방어 패턴).
 */
function CodeSelectField({ label, options, value, onChange }) {
  // 코드가 폼보다 늦게 도착한 경우(모달을 아주 빨리 연 경우) 셀렉트에는 첫 옵션이 보이는데
  // form 값은 ''인 불일치가 생긴다 — 보이는 값과 저장되는 값을 맞춰준다.
  useEffect(() => {
    if (!value && options.length > 0) onChange(options[0].value)
  }, [value, options, onChange])

  return (
    <div className="field">
      <label>{label}</label>
      {options.length === 0 ? (
        <input value={value} required onChange={(e) => onChange(e.target.value)} />
      ) : (
        <select value={value} required onChange={(e) => onChange(e.target.value)}>
          {/* 저장된 값이 코드에서 지워진 경우에도 셀렉트가 멋대로 첫 옵션으로 바뀌지 않도록 남겨둔다. */}
          {value && !options.some((o) => o.value === value) && <option value={value}>{value} (코드 없음)</option>}
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      )}
    </div>
  )
}

function PageDetailSection({ page, codeOptions, onEditBasic }) {
  const [activeTab, setActiveTab] = useState('dataServices')
  const [roles, setRoles] = useState([])
  const [dataServices, setDataServices] = useState([])
  const [messageResources, setMessageResources] = useState([])

  useEffect(() => {
    rolesApi.list('?size=200').then((res) => setRoles(res.data || [])).catch(() => {})
    dataServicesApi.list('?size=200').then((res) => setDataServices(res.data || [])).catch(() => {})
    messageResourcesApi.list('?size=200').then((res) => setMessageResources(res.data || [])).catch(() => {})
  }, [])

  const dataServiceName = (id) => dataServices.find((d) => d.id === id)?.dataServiceName || id
  const messageResourceValue = (code) => messageResources.find((m) => m.messageResourceCode === code)?.value || code

  return (
    <div className="detail-section">
      <h3>
        {page.pageName} ({page.pageCode})
      </h3>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        tabs={[
          { key: 'info', label: '기본정보' },
          { key: 'dataServices', label: '데이터서비스' },
          { key: 'messageResources', label: '메시지자원' },
          { key: 'roles', label: '권한 (Role)' },
        ]}
      />

      <div className="tab-content">
        {activeTab === 'info' && (
          <div className="card" style={{ width: 'auto' }}>
            <div className="kv-row">
              <span className="k">페이지 코드</span>
              <span>{page.pageCode}</span>
            </div>
            <div className="kv-row">
              <span className="k">페이지명</span>
              <span>{page.pageName}</span>
            </div>
            <div className="kv-row">
              <span className="k">용도</span>
              <span>{codeLabel(codeOptions.pageType, page.pageType)}</span>
            </div>
            <div className="kv-row">
              <span className="k">디바이스유형</span>
              <span>{codeLabel(codeOptions.deviceType, page.deviceType)}</span>
            </div>
            <div className="kv-row">
              <span className="k">페이지 파일</span>
              <span>{page.pageFile || '-'}</span>
            </div>
            <div className="kv-row">
              <span className="k">메시지자원 코드</span>
              <span>{page.messageResourceCode || '-'}</span>
            </div>
            <div className="kv-row">
              <span className="k">사용여부</span>
              <span>{page.useYn}</span>
            </div>
            <button className="secondary" onClick={onEditBasic}>
              기본정보 수정
            </button>
          </div>
        )}

        {activeTab === 'dataServices' && (
          <MappingPanel
            title="이 페이지가 쓰는 데이터서비스"
            api={pageDataServicesApi(page.id)}
            addField={{
              name: 'dataServiceId',
              label: '데이터서비스',
              optionsApi: dataServicesApi,
              optionsLabel: (d) => d.dataServiceName,
            }}
            renderItem={(item) => dataServiceName(item.dataServiceId)}
          />
        )}

        {activeTab === 'messageResources' && (
          <MappingPanel
            title="이 페이지가 쓰는 메시지자원"
            api={pageMessageResourcesApi(page.id)}
            addField={{
              name: 'messageResourceCode',
              label: '메시지자원',
              optionsApi: messageResourcesApi,
              optionsLabel: (m) => `${m.messageResourceCode} (${m.value})`,
              optionsValue: (m) => m.messageResourceCode,
            }}
            renderItem={(item) => `${item.messageResourceCode} (${messageResourceValue(item.messageResourceCode)})`}
          />
        )}

        {activeTab === 'roles' && (
          <MappingPanel
            title="이 페이지에 접근 가능한 Role"
            api={pageRolesApi(page.id)}
            addField={{
              name: 'roleCode',
              label: 'Role',
              optionsApi: rolesApi,
              optionsLabel: (r) => `${r.roleName} (${r.roleCode})`,
              optionsValue: (r) => r.roleCode,
            }}
            renderItem={(item) => {
              const role = roles.find((r) => r.roleCode === item.roleCode)
              return role ? `${role.roleName} (${item.roleCode})` : item.roleCode
            }}
          />
        )}
      </div>
    </div>
  )
}
