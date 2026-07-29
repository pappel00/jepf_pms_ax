import { apiFetch } from './http'

/**
 * 대부분의 엔티티가 list(GET)/get(GET)/create(POST)/update(PUT)/remove(DELETE) 형태의
 * 동일한 REST 패턴이라, 매번 반복해서 쓰지 않고 이 팩토리로 만든다.
 */
export function createCrudApi(basePath) {
  return {
    list: (query = '') => apiFetch(`${basePath}${query}`),
    get: (id) => apiFetch(`${basePath}/${id}`),
    create: (body) => apiFetch(basePath, { method: 'POST', body }),
    update: (id, body) => apiFetch(`${basePath}/${id}`, { method: 'PUT', body }),
    remove: (id) => apiFetch(`${basePath}/${id}`, { method: 'DELETE' }),
  }
}

/** 매핑(N:M) 하위 리소스용 — update가 없고 add(=create)/remove만 있다. */
export function createMappingApi(basePath) {
  return {
    list: () => apiFetch(basePath),
    add: (body) => apiFetch(basePath, { method: 'POST', body }),
    remove: (mappingId) => apiFetch(`${basePath}/${mappingId}`, { method: 'DELETE' }),
  }
}
