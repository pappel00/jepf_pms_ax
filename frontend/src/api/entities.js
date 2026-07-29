import { apiFetch, downloadFile, uploadFile } from './http'
import { createCrudApi, createMappingApi } from './crud'

export const companiesApi = createCrudApi('/api/v1/companies')
export const departmentsApi = createCrudApi('/api/v1/departments')
export const usersApi = createCrudApi('/api/v1/users')
export const rolesApi = createCrudApi('/api/v1/roles')
export const userGroupsApi = createCrudApi('/api/v1/user-groups')
export const portalsApi = createCrudApi('/api/v1/portals')
export const applicationsApi = createCrudApi('/api/v1/applications')
export const pagesApi = createCrudApi('/api/v1/pages')
export const menusApi = createCrudApi('/api/v1/menus')
export const dataServicesApi = createCrudApi('/api/v1/data-services')
export const messageResourcesApi = createCrudApi('/api/v1/message-resources')
export const codeGroupsApi = createCrudApi('/api/v1/code-groups')
export const fileGroupsApi = createCrudApi('/api/v1/file-groups')
export const termsGroupsApi = createCrudApi('/api/v1/terms-groups')
export const noticesApi = createCrudApi('/api/v1/notices')
export const privacyPolicyGroupsApi = createCrudApi('/api/v1/privacy-policy-groups')
export const formGroupsApi = createCrudApi('/api/v1/form-groups')

// --- 매핑(N:M) 하위 리소스 ---
export const userGroupRolesApi = (userGroupId) => createMappingApi(`/api/v1/user-groups/${userGroupId}/roles`)
export const userGroupUsersApi = (userGroupId) => createMappingApi(`/api/v1/user-groups/${userGroupId}/users`)
export const pageRolesApi = (pageId) => createMappingApi(`/api/v1/pages/${pageId}/roles`)
export const pageDataServicesApi = (pageId) => createMappingApi(`/api/v1/pages/${pageId}/data-services`)
export const menuPagesApi = (menuId) => createMappingApi(`/api/v1/menus/${menuId}/pages`)
export const pageMessageResourcesApi = (pageId) => createMappingApi(`/api/v1/pages/${pageId}/message-resources`)

// 코드상세는 CodeGroup의 하위 리소스이자 CodeDetail 자체가 code/codeName/sortOrder/description을
// 갖는 진짜 엔티티라(순수 매핑이 아님) createCrudApi를 그대로 재사용한다.
export const codeDetailsApi = (groupId) => createCrudApi(`/api/v1/code-groups/${groupId}/details`)

// 첨부항목도 CodeDetail과 동일한 이유로 createCrudApi를 그대로 재사용한다 (순수 매핑이 아니라
// fileName/filePath/fileSize 등을 갖는 진짜 엔티티).
export const fileItemsApi = (groupId) => createCrudApi(`/api/v1/file-groups/${groupId}/items`)

/** 실제 파일을 업로드한다 — filePath를 직접 입력하는 fileItemsApi.create()와는 별개 경로. */
export function uploadFileItem(groupId, file, { sortOrder, description } = {}) {
  const formData = new FormData()
  formData.append('file', file)
  if (sortOrder !== undefined && sortOrder !== null && sortOrder !== '') {
    formData.append('sortOrder', sortOrder)
  }
  if (description) formData.append('description', description)
  return uploadFile(`/api/v1/file-groups/${groupId}/items/upload`, formData)
}

/** 첨부항목의 실제 파일을 다운로드한다 (브라우저 저장 다이얼로그를 띄운다). */
export function downloadFileItem(groupId, itemId, fileName) {
  return downloadFile(`/api/v1/file-groups/${groupId}/items/${itemId}/download`, fileName)
}

export function menuTree(applicationId) {
  return apiFetch(`/api/v1/menus/tree?applicationId=${applicationId}`)
}

// 약관 버전도 CodeDetail/FileItem과 동일한 이유로 createCrudApi를 그대로 재사용한다 (순수
// 매핑이 아니라 version/content/effectiveDate 등을 갖는 진짜 엔티티).
export const termsVersionsApi = (groupId) => createCrudApi(`/api/v1/terms-groups/${groupId}/versions`)

// 개인정보처리방침 버전도 동일한 이유로 createCrudApi를 재사용한다.
export const privacyPolicyVersionsApi = (groupId) => createCrudApi(`/api/v1/privacy-policy-groups/${groupId}/versions`)

// 표준양식 버전도 동일한 이유로 createCrudApi를 재사용한다.
export const formVersionsApi = (groupId) => createCrudApi(`/api/v1/form-groups/${groupId}/versions`)

/**
 * 표준양식 버전은 텍스트가 아니라 실제 엑셀/워드 파일로 관리한다 — 업로드는 fileItemsApi와
 * 달리 versionNo/effectiveDate가 필수로 같이 필요하다(양식 버전 정보이므로).
 */
export function uploadFormVersion(groupId, file, { versionNo, effectiveDate, description }) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('versionNo', versionNo)
  formData.append('effectiveDate', effectiveDate)
  if (description) formData.append('description', description)
  return uploadFile(`/api/v1/form-groups/${groupId}/versions/upload`, formData)
}

/** 표준양식 버전의 실제 파일을 다운로드한다 (브라우저 저장 다이얼로그를 띄운다). */
export function downloadFormVersion(groupId, versionId, fileName) {
  return downloadFile(`/api/v1/form-groups/${groupId}/versions/${versionId}/download`, fileName)
}
