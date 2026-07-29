import { codeGroupsApi, codeDetailsApi } from './entities'

/**
 * 공통코드(module-code)의 코드상세를 셀렉트 옵션 형태로 불러온다.
 *
 * 화면에서 하드코딩된 선택지 대신 공통코드를 쓰는 곳이 여럿이라(공지 팝업 상세 설정, 페이지
 * 용도/디바이스유형 등) 로딩 로직을 여기 한 곳에 모았다.
 *
 * @param {Record<string, string>} groupCodeByField
 *   {폼 필드명: 코드그룹코드} 매핑. 예: { pageType: 'PAGE_TYPE', deviceType: 'PAGE_DEVICE_TYPE' }
 * @returns {Promise<Record<string, {value: string, label: string}[]>>}
 *   같은 키에 옵션 배열이 담긴 객체. 해당 코드그룹이 없거나 호출이 실패하면 빈 배열이다 —
 *   공통코드를 못 가져와도 화면이 막히지 않아야 하므로 예외를 던지지 않는다(호출하는 화면은
 *   옵션이 비어있으면 텍스트 입력으로 대체한다).
 *
 * 옵션의 value 는 CodeDetail.code(실제 엔티티에 저장되는 값), label 은 codeName(화면 표시명)이다.
 * 사용여부(useYn)가 'N'인 코드상세는 제외하고, sortOrder 순으로 정렬한다.
 */
export async function loadCodeOptions(groupCodeByField) {
  const empty = Object.fromEntries(Object.keys(groupCodeByField).map((field) => [field, []]))
  try {
    const groupsRes = await codeGroupsApi.list('?size=200')
    const groups = groupsRes.data || []

    const entries = await Promise.all(
      Object.entries(groupCodeByField).map(async ([field, groupCode]) => {
        const group = groups.find((g) => g.groupCode === groupCode && g.deletedYn !== 'Y')
        if (!group) return [field, []]
        const detailsRes = await codeDetailsApi(group.id).list()
        const options = (detailsRes.data || [])
          .filter((d) => d.useYn === 'Y' && d.deletedYn !== 'Y')
          .sort((a, b) => a.sortOrder - b.sortOrder)
          .map((d) => ({ value: d.code, label: d.codeName || d.code }))
        return [field, options]
      }),
    )
    return Object.fromEntries(entries)
  } catch {
    return empty
  }
}

/** 저장된 코드값을 화면 표시명으로 바꾼다. 옵션에 없는 값(코드가 지워진 경우 등)은 그대로 보여준다. */
export function codeLabel(options, value) {
  if (!value) return '-'
  return options.find((o) => o.value === value)?.label || value
}
