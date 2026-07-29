import { useCallback, useEffect, useState } from 'react'

/**
 * N:M 매핑 하나를 관리하는 공통 패널 — "추가할 값 입력 + 추가 버튼" + "현재 매핑 목록 + 제거 버튼".
 * api는 createMappingApi()로 만든 { list, add, remove } 객체.
 * addField: { name, label, placeholder, optionsApi?, optionsLabel?, optionsValue? } —
 * add(body) 호출 시 { [name]: 선택/입력값 } 형태로 보낸다. optionsApi가 있으면 텍스트 입력
 * 대신 드롭다운으로 고른다(optionsApi.list()로 옵션을 불러와서, optionsLabel(row)로 화면에
 * 보여줄 문자열을, optionsValue(row)로 실제 전송할 값을 뽑는다 — optionsValue 기본값은 row.id).
 * renderItem(item): 목록의 각 행에 보여줄 내용.
 */
export default function MappingPanel({ title, api, addField, renderItem, idField = 'id' }) {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [inputValue, setInputValue] = useState('')
  const [adding, setAdding] = useState(false)
  const [options, setOptions] = useState([])

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await api.list()
      setItems(res.data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }, [api])

  const loadOptions = useCallback(async () => {
    if (!addField.optionsApi) return
    try {
      const res = await addField.optionsApi.list('?size=200')
      setOptions(res.data || [])
    } catch {
      // 옵션을 못 가져와도 패널 자체는 계속 쓸 수 있게 조용히 넘어간다.
    }
  }, [addField.optionsApi])

  useEffect(() => {
    load()
    loadOptions()
  }, [load, loadOptions])

  const handleAdd = async (e) => {
    e.preventDefault()
    if (!inputValue.trim()) return
    setAdding(true)
    setError(null)
    try {
      await api.add({ [addField.name]: inputValue.trim() })
      setInputValue('')
      load()
    } catch (err) {
      setError(err.message)
    } finally {
      setAdding(false)
    }
  }

  const handleRemove = async (item) => {
    if (!confirm('매핑을 삭제하시겠습니까?')) return
    try {
      await api.remove(item[idField])
      load()
    } catch (err) {
      alert(err.message)
    }
  }

  return (
    <div className="mapping-panel">
      <h4>{title}</h4>
      {error && <div className="error-box">{error}</div>}

      <form className="mapping-add-row" onSubmit={handleAdd}>
        {addField.optionsApi ? (
          <select value={inputValue} onChange={(e) => setInputValue(e.target.value)}>
            <option value="">{addField.placeholder || addField.label}</option>
            {options.map((opt) => (
              <option
                key={addField.optionsValue ? addField.optionsValue(opt) : opt.id}
                value={addField.optionsValue ? addField.optionsValue(opt) : opt.id}
              >
                {addField.optionsLabel ? addField.optionsLabel(opt) : opt.id}
              </option>
            ))}
          </select>
        ) : (
          <input
            placeholder={addField.placeholder || addField.label}
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
          />
        )}
        <button className="primary small" type="submit" disabled={adding}>
          추가
        </button>
      </form>

      {loading ? (
        <p className="muted">불러오는 중...</p>
      ) : items.length === 0 ? (
        <p className="muted">매핑된 항목이 없습니다.</p>
      ) : (
        <ul className="mapping-list">
          {items.map((item) => (
            <li key={item[idField]}>
              <span>{renderItem(item)}</span>
              <button className="link-btn danger" onClick={() => handleRemove(item)}>
                제거
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
