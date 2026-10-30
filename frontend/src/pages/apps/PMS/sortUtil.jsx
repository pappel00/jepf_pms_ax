/**
 * 목록 표 정렬 공통 부품 — 열 머리를 누르면 오름차순 → 내림차순 → 원래 순서 로 바뀐다.
 * 정렬은 화면에서 한다(목록은 이미 전부 읽어 온 상태). 순서 자체가 의미인 화면(WBS 트리·평가 입력·이력·체크리스트 등)에는 쓰지 않는다.
 *
 * 사용:
 *   const { sort, onSort, sorted } = useSort(list, { name: (r) => r.name, amount: (r) => r.amount })
 *   <SortTh k="name" sort={sort} onSort={onSort}>이름</SortTh>
 *   {sorted.map(...)}
 */
import { useMemo, useState } from 'react'

const blank = (v) => v === null || v === undefined || v === ''

function compare(a, b) {
  if (blank(a) && blank(b)) return 0
  if (blank(a)) return 1 // 값이 없는 행은 항상 뒤로
  if (blank(b)) return -1
  if (typeof a === 'number' && typeof b === 'number') return a - b
  return String(a).localeCompare(String(b), 'ko', { numeric: true })
}

/** accessors: { 열키: (행) => 정렬값 }. 선택한 열이 없으면 원래 순서 그대로 돌려준다. */
export function useSort(list, accessors) {
  const [sort, setSort] = useState({ key: null, dir: 'asc' })
  const onSort = (key) =>
    setSort((s) => (s.key !== key ? { key, dir: 'asc' } : s.dir === 'asc' ? { key, dir: 'desc' } : { key: null, dir: 'asc' }))
  const sorted = useMemo(() => {
    const get = sort.key && accessors[sort.key]
    if (!get) return list
    const sign = sort.dir === 'asc' ? 1 : -1
    // 값이 없는 행을 뒤로 보내는 규칙은 방향과 무관해야 하므로 비어 있는지 먼저 본다.
    return [...list].sort((x, y) => {
      const a = get(x)
      const b = get(y)
      if (blank(a) !== blank(b)) return blank(a) ? 1 : -1
      return sign * compare(a, b)
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [list, sort.key, sort.dir])
  return { sort, onSort, sorted }
}

/** 정렬 가능한 열 머리. k 는 useSort 의 accessors 키. */
export function SortTh({ k, sort, onSort, children, style, ...rest }) {
  const active = sort.key === k
  const arrow = active ? (sort.dir === 'asc' ? '▲' : '▼') : '↕'
  return (
    <th
      {...rest}
      onClick={() => onSort(k)}
      aria-sort={active ? (sort.dir === 'asc' ? 'ascending' : 'descending') : 'none'}
      title="눌러서 정렬 (오름차순 → 내림차순 → 원래 순서)"
      style={{ cursor: 'pointer', userSelect: 'none', whiteSpace: 'nowrap', ...style }}
    >
      {children}
      <span style={{ marginLeft: 4, fontSize: 10, opacity: active ? 1 : 0.35 }}>{arrow}</span>
    </th>
  )
}
