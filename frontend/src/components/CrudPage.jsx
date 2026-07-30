import { useCallback, useEffect, useState } from 'react'
import Modal from './Modal'

/**
 * 대부분의 엔티티 관리 화면(검색조건 + 목록 + 생성/수정 모달 + 삭제)이 거의 같은 모양이라
 * 하나로 만들어서 재사용한다.
 *
 * columns 항목: { key, label, render?, resolveApi?, resolveLabel?, resolveKey? }
 * - render(row): 커스텀 렌더링. 있으면 resolveApi보다 우선한다.
 * - resolveApi/resolveLabel: 참조 ID를 사람이 읽을 이름으로 바꿔서 보여준다 (예: portalId를
 *   포탈명으로). resolveApi.list()로 참조 목록을 한 번 불러오고, row[key]와 resolveKey(기본
 *   'id')가 같은 항목을 찾아 resolveLabel(matched)를 표시한다. 못 찾으면 원래 값 그대로 보여준다.
 *
 * fields 항목: { name, label, required?, createOnly?, editOnly?, immutable?, type?, optionsApi?, optionsLabel?, optionsValue?, excludeSelf? }
 * - type: 'text'(기본) | 'number' | 'checkbox' | 'select'
 * - createOnly: 생성 시에만 보이고 수정 폼에는 안 나옴 (예: 코드 값)
 * - editOnly: 수정 시에만 보이고 생성 폼에는 안 나옴 (예: useYn — CreateRequest엔 없고
 *   UpdateRequest에만 있는 필드. 엔티티는 생성 시 항상 useYn="Y"로 시작하고, 활성화/비활성화는
 *   수정에서만 가능하다)
 * - immutable: 수정 폼에는 보이지만 편집 불가(읽기 전용으로 표시)
 * - type: 'select'일 때 — 정적 목록이면 options: [{value,label}]를 바로 준다(예: PC/MOBILE/ALL
 *   같은 고정된 값). API에서 불러와야 하면 optionsApi(목록을 불러올 api 객체, .list() 가능해야
 *   함)/optionsLabel(row => 화면에 보여줄 문자열)/optionsValue(row => 실제 저장할 값, 기본은
 *   row.id)를 준다. optionsApi 옵션은 모달이 열릴 때 한 번 불러온다. excludeSelf: true면 수정
 *   중인 자기 자신을 옵션에서 뺀다(예: 부서의 상위부서 선택 시 자기 자신을 상위로 고를 수 없게).
 *
 * search 프롭(선택) — 있으면 검색바가 생긴다:
 * { keywordFields: ['code','name'], keywordLabel?, filters?: [{ key, label, type:'select',
 *   options?: [{value,label}], optionsApi?, optionsLabel?, optionsValue? }] }
 * 검색은 지금은 서버 쿼리가 아니라 이미 불러온 목록을 클라이언트에서 필터링하는 방식이다.
 */
export default function CrudPage({ title, description, api, columns, fields, idField = 'id', disableCreate, search }) {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState({})
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState(null)
  const [selectOptions, setSelectOptions] = useState({})
  const [columnRefs, setColumnRefs] = useState({})

  const [searchKeyword, setSearchKeyword] = useState('')
  const [searchFilters, setSearchFilters] = useState({})
  const [appliedKeyword, setAppliedKeyword] = useState('')
  const [appliedFilters, setAppliedFilters] = useState({})
  const [filterOptions, setFilterOptions] = useState({})

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await api.list('?size=200')
      setRows(res.data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [api])

  useEffect(() => {
    load()
  }, [load])

  // 목록 컬럼에서 참조 ID를 이름으로 보여줘야 하는 컬럼들의 참조 데이터를 한 번 불러온다.
  useEffect(() => {
    const resolveColumns = columns.filter((c) => c.resolveApi)
    if (resolveColumns.length === 0) return
    Promise.all(
      resolveColumns.map(async (c) => {
        try {
          const res = await c.resolveApi.list('?size=200')
          return [c.key, res.data || []]
        } catch {
          return [c.key, []]
        }
      }),
    ).then((entries) => setColumnRefs(Object.fromEntries(entries)))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 검색 필터 중 select 타입에 optionsApi가 있으면 옵션 목록을 한 번 불러온다.
  useEffect(() => {
    const apiFilters = (search?.filters || []).filter((f) => f.optionsApi)
    if (apiFilters.length === 0) return
    Promise.all(
      apiFilters.map(async (f) => {
        try {
          const res = await f.optionsApi.list('?size=200')
          return [f.key, res.data || []]
        } catch {
          return [f.key, []]
        }
      }),
    ).then((entries) => setFilterOptions(Object.fromEntries(entries)))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const loadSelectOptions = useCallback(async (excludeId) => {
    const apiSelectFields = fields.filter((f) => f.type === 'select' && f.optionsApi)
    if (apiSelectFields.length === 0) return
    const entries = await Promise.all(
      apiSelectFields.map(async (f) => {
        try {
          const res = await f.optionsApi.list('?size=200')
          let options = res.data || []
          if (f.excludeSelf && excludeId) {
            options = options.filter((row) => row[idField] !== excludeId)
          }
          return [f.name, options]
        } catch {
          return [f.name, []]
        }
      }),
    )
    setSelectOptions(Object.fromEntries(entries))
  }, [fields, idField])

  const openCreate = () => {
    setEditing(null)
    setFormError(null)
    const initial = {}
    fields.filter((f) => !f.editOnly).forEach((f) => {
      initial[f.name] = f.default ?? ''
    })
    setForm(initial)
    setModalOpen(true)
    loadSelectOptions(null)
  }

  const openEdit = (row) => {
    setEditing(row)
    setFormError(null)
    const initial = {}
    fields.forEach((f) => {
      initial[f.name] = row[f.name] ?? ''
    })
    setForm(initial)
    setModalOpen(true)
    loadSelectOptions(row[idField])
  }

  const handleDelete = async (row) => {
    if (!confirm('삭제하시겠습니까?')) return
    try {
      await api.remove(row[idField])
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  /** 이미 삭제된 행은 "삭제" 대신 이 버튼을 쓴다 — 삭제된 행에서 "삭제"를 다시 누르면 404가 나던 문제 대응. */
  const handleRestore = async (row) => {
    if (!confirm('복구하시겠습니까?')) return
    try {
      await api.restore(row[idField])
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
        const body = {}
        fields.filter((f) => !f.createOnly).forEach((f) => {
          body[f.name] = form[f.name]
        })
        await api.update(editing[idField], body)
      } else {
        const body = {}
        fields.filter((f) => !f.editOnly).forEach((f) => {
          body[f.name] = form[f.name]
        })
        await api.create(body)
      }
      setModalOpen(false)
      load()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSaving(false)
    }
  }

  const handleSearch = (e) => {
    e.preventDefault()
    setAppliedKeyword(searchKeyword)
    setAppliedFilters(searchFilters)
  }

  const handleResetSearch = () => {
    setSearchKeyword('')
    setSearchFilters({})
    setAppliedKeyword('')
    setAppliedFilters({})
  }

  const filteredRows = rows.filter((row) => {
    const matchesKeyword =
      !appliedKeyword ||
      (search?.keywordFields || []).some((field) =>
        String(row[field] ?? '').toLowerCase().includes(appliedKeyword.toLowerCase()),
      )
    const matchesFilters = (search?.filters || []).every((f) => {
      const value = appliedFilters[f.key]
      if (!value || value === 'ALL') return true
      return row[f.key] === value
    })
    return matchesKeyword && matchesFilters
  })

  const renderColumnValue = (row, c) => {
    if (c.render) return c.render(row)
    if (c.resolveApi) {
      const match = (columnRefs[c.key] || []).find((r) => r[c.resolveKey || 'id'] === row[c.key])
      return match ? c.resolveLabel(match) : row[c.key]
    }
    return row[c.key]
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <h2>{title}</h2>
          {description && <p className="page-description">{description}</p>}
        </div>
        {!disableCreate && (
          <button className="primary small" onClick={openCreate}>
            + 새로 만들기
          </button>
        )}
      </div>

      {search && (
        <form className="search-bar" onSubmit={handleSearch}>
          {search.keywordFields && (
            <div className="search-field">
              <label>{search.keywordLabel || '검색'}</label>
              <input value={searchKeyword} onChange={(e) => setSearchKeyword(e.target.value)} />
            </div>
          )}
          {(search.filters || []).map((f) => (
            <div className="search-field" key={f.key}>
              <label>{f.label}</label>
              <select
                value={searchFilters[f.key] ?? 'ALL'}
                onChange={(e) => setSearchFilters({ ...searchFilters, [f.key]: e.target.value })}
              >
                <option value="ALL">전체</option>
                {f.optionsApi
                  ? (filterOptions[f.key] || []).map((opt) => (
                      <option
                        key={f.optionsValue ? f.optionsValue(opt) : opt.id}
                        value={f.optionsValue ? f.optionsValue(opt) : opt.id}
                      >
                        {f.optionsLabel ? f.optionsLabel(opt) : opt.id}
                      </option>
                    ))
                  : (f.options || []).map((opt) => (
                      <option key={opt.value} value={opt.value}>
                        {opt.label}
                      </option>
                    ))}
              </select>
            </div>
          ))}
          <div className="search-actions">
            <button type="button" className="secondary small" onClick={handleResetSearch}>
              초기화
            </button>
            <button type="submit" className="primary small">
              검색
            </button>
          </div>
        </form>
      )}

      {error && <div className="error-box">{error}</div>}

      {search && !loading && (
        <p className="muted" style={{ margin: '4px 0 8px' }}>
          총 {filteredRows.length}건
        </p>
      )}

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              {columns.map((c) => (
                <th key={c.key}>{c.label}</th>
              ))}
              <th style={{ width: 120 }}>액션</th>
            </tr>
          </thead>
          <tbody>
            {filteredRows.map((row) => (
              <tr key={row[idField]}>
                {columns.map((c) => (
                  <td key={c.key}>{renderColumnValue(row, c)}</td>
                ))}
                <td>
                  <button className="link-btn" onClick={() => openEdit(row)}>
                    수정
                  </button>
                  {row.deletedYn === 'Y' ? (
                    <button className="link-btn" onClick={() => handleRestore(row)}>
                      복구
                    </button>
                  ) : (
                    <button className="link-btn danger" onClick={() => handleDelete(row)}>
                      삭제
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {filteredRows.length === 0 && (
              <tr>
                <td colSpan={columns.length + 1} className="empty-cell">
                  데이터가 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      )}

      {modalOpen && (
        <Modal title={editing ? `${title} 수정` : `${title} 생성`} onClose={() => setModalOpen(false)}>
          <form onSubmit={handleSubmit}>
            {formError && <div className="error-box">{formError}</div>}
            {fields
              .filter((f) => (editing ? !f.createOnly : !f.editOnly))
              .map((f) => (
                <div className="field" key={f.name}>
                  <label>{f.label}</label>
                  {f.type === 'checkbox' ? (
                    <input
                      type="checkbox"
                      checked={form[f.name] === 'Y'}
                      disabled={editing && f.immutable}
                      onChange={(e) => setForm({ ...form, [f.name]: e.target.checked ? 'Y' : 'N' })}
                    />
                  ) : f.type === 'select' ? (
                    <select
                      value={form[f.name] ?? ''}
                      required={f.required}
                      disabled={editing && f.immutable}
                      onChange={(e) => setForm({ ...form, [f.name]: e.target.value })}
                    >
                      <option value="">선택하세요</option>
                      {f.options
                        ? f.options.map((opt) => (
                            <option key={opt.value} value={opt.value}>
                              {opt.label}
                            </option>
                          ))
                        : (selectOptions[f.name] || []).map((opt) => (
                            <option
                              key={f.optionsValue ? f.optionsValue(opt) : opt.id}
                              value={f.optionsValue ? f.optionsValue(opt) : opt.id}
                            >
                              {f.optionsLabel ? f.optionsLabel(opt) : opt.id}
                            </option>
                          ))}
                    </select>
                  ) : (
                    <input
                      type={f.type === 'number' ? 'number' : 'text'}
                      value={form[f.name] ?? ''}
                      required={f.required}
                      disabled={editing && f.immutable}
                      onChange={(e) => setForm({ ...form, [f.name]: e.target.value })}
                    />
                  )}
                </div>
              ))}
            <button className="primary" type="submit" disabled={saving}>
              {saving ? '저장 중...' : '저장'}
            </button>
          </form>
        </Modal>
      )}
    </div>
  )
}
