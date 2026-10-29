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

// --- PMS (module-pms) ---
export const pmsCustomersApi = createCrudApi('/api/v1/pms/customers')
export const pmsContactsApi = createCrudApi('/api/v1/pms/customer-contacts')
export const pmsProjectsApi = {
  ...createCrudApi('/api/v1/pms/projects'),
  /** 상태만 변경 — 단계전환·수행 종료·운영 전환 등 */
  changeStatus: (id, body) => apiFetch(`/api/v1/pms/projects/${id}/status`, { method: 'POST', body }),
  history: (id) => apiFetch(`/api/v1/pms/projects/${id}/history`),
}
export const pmsUserProfilesApi = {
  list: () => apiFetch('/api/v1/pms/user-profiles'),
  get: (userId) => apiFetch(`/api/v1/pms/user-profiles/${userId}`),
  upsert: (userId, body) => apiFetch(`/api/v1/pms/user-profiles/${userId}`, { method: 'PUT', body }),
}
export const pmsTeamsApi = {
  list: (kind = 'EXEC') => apiFetch(`/api/v1/pms/teams?kind=${kind}`),
  get: (projectId, kind = 'EXEC') => apiFetch(`/api/v1/pms/projects/${projectId}/team?kind=${kind}`),
}
export const pmsTransitionsApi = {
  list: (query = '') => apiFetch(`/api/v1/pms/transitions${query}`),
  request: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/transitions`, { method: 'POST', body }),
  approve: (id) => apiFetch(`/api/v1/pms/transitions/${id}/approve`, { method: 'POST', body: {} }),
  reject: (id, comment) => apiFetch(`/api/v1/pms/transitions/${id}/reject`, { method: 'POST', body: { comment } }),
  cancel: (id) => apiFetch(`/api/v1/pms/transitions/${id}/cancel`, { method: 'POST', body: {} }),
  rollback: (id, reason) => apiFetch(`/api/v1/pms/transitions/${id}/rollback`, { method: 'POST', body: { reason } }),
}
export const pmsTeamChangesApi = {
  list: (query = '') => apiFetch(`/api/v1/pms/team-changes${query}`),
  request: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/team-changes`, { method: 'POST', body }),
  approve: (id, comment) => apiFetch(`/api/v1/pms/team-changes/${id}/approve`, { method: 'POST', body: { comment: comment || null } }),
  reject: (id, comment) => apiFetch(`/api/v1/pms/team-changes/${id}/reject`, { method: 'POST', body: { comment } }),
  cancel: (id) => apiFetch(`/api/v1/pms/team-changes/${id}/cancel`, { method: 'POST', body: {} }),
}
export const pmsWbsApi = {
  list: (projectId = '') => apiFetch(`/api/v1/pms/wbs-tasks${projectId ? `?projectId=${projectId}` : ''}`),
  createBulk: (projectId, tasks) => apiFetch(`/api/v1/pms/projects/${projectId}/wbs-tasks/bulk`, { method: 'POST', body: { tasks } }),
  updateProgress: (id, progressPct, note) =>
    apiFetch(`/api/v1/pms/wbs-tasks/${id}/progress`, { method: 'PUT', body: { progressPct, note: note || null } }),
  copy: (id, assigneeUserId) => apiFetch(`/api/v1/pms/wbs-tasks/${id}/copy`, { method: 'POST', body: { assigneeUserId } }),
}
export const pmsIssuesApi = {
  list: (projectId = '') => apiFetch(`/api/v1/pms/issues${projectId ? `?projectId=${projectId}` : ''}`),
  create: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/issues`, { method: 'POST', body }),
  update: (id, body) => apiFetch(`/api/v1/pms/issues/${id}`, { method: 'PUT', body }),
  remove: (id) => apiFetch(`/api/v1/pms/issues/${id}`, { method: 'DELETE' }),
  link: (id, taskId) => apiFetch(`/api/v1/pms/issues/${id}/tasks/${taskId}`, { method: 'POST', body: {} }),
  unlink: (id, taskId) => apiFetch(`/api/v1/pms/issues/${id}/tasks/${taskId}`, { method: 'DELETE' }),
  addNote: (id, text) => apiFetch(`/api/v1/pms/issues/${id}/notes`, { method: 'POST', body: { text } }),
}
export const pmsRfpsApi = {
  /** RFP 문서(hwpx) 자동 분해 미리보기 — 저장하지 않는다. */
  parse: (file, splitLines) => {
    const fd = new FormData()
    fd.append('file', file)
    fd.append('splitLines', splitLines ? 'true' : 'false')
    return uploadFile('/api/v1/pms/rfp-parse', fd)
  },
  list: (projectId = '') => apiFetch(`/api/v1/pms/rfps${projectId ? `?projectId=${projectId}` : ''}`),
  evals: () => apiFetch('/api/v1/pms/rfp-evals'),
  create: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/rfps`, { method: 'POST', body }),
  update: (id, body) => apiFetch(`/api/v1/pms/rfps/${id}`, { method: 'PUT', body }),
  remove: (id) => apiFetch(`/api/v1/pms/rfps/${id}`, { method: 'DELETE' }),
  setEvaluators: (id, userIds) => apiFetch(`/api/v1/pms/rfps/${id}/evaluators`, { method: 'PUT', body: { userIds } }),
  addItem: (id, body) => apiFetch(`/api/v1/pms/rfps/${id}/items`, { method: 'POST', body }),
  addItems: (id, items) => apiFetch(`/api/v1/pms/rfps/${id}/items/bulk`, { method: 'POST', body: { items } }),
  updateItem: (itemId, body) => apiFetch(`/api/v1/pms/rfp-items/${itemId}`, { method: 'PUT', body }),
  deleteItem: (itemId) => apiFetch(`/api/v1/pms/rfp-items/${itemId}`, { method: 'DELETE' }),
  saveMyEval: (itemId, body) => apiFetch(`/api/v1/pms/rfp-items/${itemId}/evals/me`, { method: 'PUT', body }),
  submit: (id) => apiFetch(`/api/v1/pms/rfps/${id}/evals/submit`, { method: 'POST', body: {} }),
  reopen: (id, userId) => apiFetch(`/api/v1/pms/rfps/${id}/evaluators/${userId}/reopen`, { method: 'POST', body: {} }),
  confirm: (itemId, body) => apiFetch(`/api/v1/pms/rfp-items/${itemId}/confirm`, { method: 'PUT', body }),
  confirmAll: (id, evaluatorUserId) => apiFetch(`/api/v1/pms/rfps/${id}/confirm-all`, { method: 'POST', body: { evaluatorUserId } }),
}
export const pmsProposalsApi = {
  list: () => apiFetch('/api/v1/pms/proposals'),
  create: (body) => apiFetch('/api/v1/pms/proposals', { method: 'POST', body }),
  update: (id, body) => apiFetch(`/api/v1/pms/proposals/${id}`, { method: 'PUT', body }),
  remove: (id) => apiFetch(`/api/v1/pms/proposals/${id}`, { method: 'DELETE' }),
  addVersion: (id, body) => apiFetch(`/api/v1/pms/proposals/${id}/versions`, { method: 'POST', body }),
  saveMapping: (id, rfpItemId, body) => apiFetch(`/api/v1/pms/proposals/${id}/mappings/${rfpItemId}`, { method: 'PUT', body }),
  deleteMapping: (id, rfpItemId) => apiFetch(`/api/v1/pms/proposals/${id}/mappings/${rfpItemId}`, { method: 'DELETE' }),
  addCheck: (id, itemName) => apiFetch(`/api/v1/pms/proposals/${id}/checklist`, { method: 'POST', body: { itemName } }),
  toggleCheck: (id, checkId) => apiFetch(`/api/v1/pms/proposals/${id}/checklist/${checkId}/toggle`, { method: 'POST', body: {} }),
  removeCheck: (id, checkId) => apiFetch(`/api/v1/pms/proposals/${id}/checklist/${checkId}`, { method: 'DELETE' }),
}
const crud = (path, projectPath) => ({
  list: () => apiFetch(`/api/v1/pms/${path}`),
  create: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/${projectPath ?? path}`, { method: 'POST', body }),
  update: (id, body) => apiFetch(`/api/v1/pms/${path}/${id}`, { method: 'PUT', body }),
  remove: (id) => apiFetch(`/api/v1/pms/${path}/${id}`, { method: 'DELETE' }),
})
export const pmsSalesNotesApi = {
  list: () => apiFetch('/api/v1/pms/sales-notes'),
  create: (body) => apiFetch('/api/v1/pms/sales-notes', { method: 'POST', body }),
  update: (id, body) => apiFetch(`/api/v1/pms/sales-notes/${id}`, { method: 'PUT', body }),
  remove: (id) => apiFetch(`/api/v1/pms/sales-notes/${id}`, { method: 'DELETE' }),
  toggleDone: (id) => apiFetch(`/api/v1/pms/sales-notes/${id}/toggle-done`, { method: 'POST', body: {} }),
}
export const pmsNextOppsApi = crud('next-opps')
export const pmsOpsTasksApi = {
  ...crud('ops-tasks'),
  setStatus: (id, statusCode) => apiFetch(`/api/v1/pms/ops-tasks/${id}/status`, { method: 'POST', body: { statusCode } }),
}
export const pmsOpsIncidentsApi = {
  ...crud('ops-incidents'),
  linkOpportunity: (id, opportunityId) => apiFetch(`/api/v1/pms/ops-incidents/${id}/opportunity`, { method: 'PUT', body: { opportunityId } }),
}
export const pmsOpsOppsApi = crud('ops-opps')
export const pmsOperationApi = {
  list: () => apiFetch('/api/v1/pms/operations'),
  features: () => apiFetch('/api/v1/pms/project-features'),
  endExecution: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/end-execution`, { method: 'POST', body }),
  transition: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/operation-transition`, { method: 'POST', body }),
  update: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/operation`, { method: 'PUT', body }),
}
export const pmsCostApi = {
  rates: () => apiFetch('/api/v1/pms/rates'),
  createRate: (body) => apiFetch('/api/v1/pms/rates', { method: 'POST', body }),
  updateRate: (id, body) => apiFetch(`/api/v1/pms/rates/${id}`, { method: 'PUT', body }),
  deleteRate: (id) => apiFetch(`/api/v1/pms/rates/${id}`, { method: 'DELETE' }),
  actuals: () => apiFetch('/api/v1/pms/actual-mm'),
  saveActual: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/actual-mm`, { method: 'PUT', body }),
  settings: () => apiFetch('/api/v1/pms/settings'),
  saveSettings: (values) => apiFetch('/api/v1/pms/settings', { method: 'PUT', body: { values } }),
}
export const pmsReportsApi = {
  list: () => apiFetch('/api/v1/pms/reports'),
  saveDraft: (projectId, type, periodStart, memo) =>
    apiFetch(`/api/v1/pms/projects/${projectId}/reports/${type}/${periodStart}/draft`, { method: 'PUT', body: { memo } }),
  submit: (projectId, type, periodStart, body) =>
    apiFetch(`/api/v1/pms/projects/${projectId}/reports/${type}/${periodStart}/submit`, { method: 'POST', body }),
  reopen: (projectId, type, periodStart, reason) =>
    apiFetch(`/api/v1/pms/projects/${projectId}/reports/${type}/${periodStart}/reopen`, { method: 'POST', body: { reason } }),
  cancelEdit: (projectId, type, periodStart) =>
    apiFetch(`/api/v1/pms/projects/${projectId}/reports/${type}/${periodStart}/cancel-edit`, { method: 'POST', body: {} }),
  snapshots: () => apiFetch('/api/v1/pms/progress-snapshots'),
  saveSnapshot: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/progress-snapshots`, { method: 'PUT', body }),
}
export const pmsDocsApi = {
  list: () => apiFetch('/api/v1/pms/doc-tailoring'),
  init: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/doc-tailoring/init`, { method: 'POST', body }),
  reset: (projectId) => apiFetch(`/api/v1/pms/projects/${projectId}/doc-tailoring/reset`, { method: 'POST', body: {} }),
  addCustom: (projectId, body) => apiFetch(`/api/v1/pms/projects/${projectId}/doc-tailoring`, { method: 'POST', body }),
  patch: (id, body) => apiFetch(`/api/v1/pms/doc-tailoring/${id}`, { method: 'PATCH', body }),
  remove: (id) => apiFetch(`/api/v1/pms/doc-tailoring/${id}`, { method: 'DELETE' }),
  addVersion: (id, body) => apiFetch(`/api/v1/pms/doc-tailoring/${id}/versions`, { method: 'POST', body }),
  updateLatest: (id, body) => apiFetch(`/api/v1/pms/doc-tailoring/${id}/versions/latest`, { method: 'PUT', body }),
  deleteLatest: (id) => apiFetch(`/api/v1/pms/doc-tailoring/${id}/versions/latest`, { method: 'DELETE' }),
  setReviewed: (id, reviewed) => apiFetch(`/api/v1/pms/doc-tailoring/${id}/reviewed`, { method: 'PUT', body: { reviewed } }),
}
export const pmsScheduleProposalsApi = {
  list: (projectId = '') => apiFetch(`/api/v1/pms/schedule-proposals${projectId ? `?projectId=${projectId}` : ''}`),
  approve: (id) => apiFetch(`/api/v1/pms/schedule-proposals/${id}/approve`, { method: 'POST', body: {} }),
  reject: (id, comment) => apiFetch(`/api/v1/pms/schedule-proposals/${id}/reject`, { method: 'POST', body: { comment } }),
  ignore: (id, comment) => apiFetch(`/api/v1/pms/schedule-proposals/${id}/ignore`, { method: 'POST', body: { comment: comment || null } }),
}
// --- 매핑(N:M) 하위 리소스 ---
export const userGroupRolesApi = (userGroupId) => createMappingApi(`/api/v1/user-groups/${userGroupId}/roles`)
export const userGroupUsersApi = (userGroupId) => createMappingApi(`/api/v1/user-groups/${userGroupId}/users`)
export const pageRolesApi = (pageId) => createMappingApi(`/api/v1/pages/${pageId}/roles`)
export const pageDataServicesApi = (pageId) => createMappingApi(`/api/v1/pages/${pageId}/data-services`)
export const menuPagesApi = (menuId) => createMappingApi(`/api/v1/menus/${menuId}/pages`)
export const pageMessageResourcesApi = (pageId) => createMappingApi(`/api/v1/pages/${pageId}/message-resources`)
export const applicationRolesApi = (applicationId) => createMappingApi(`/api/v1/applications/${applicationId}/roles`)

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
