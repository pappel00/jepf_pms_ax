package com.platform.framework.core.common.response;

/**
 * MAD 4500 Response 규격의 검증 실패 상세 항목.
 * 예) { "field": "userId", "rejectedValue": "admin", "reason": "이미 존재하는 아이디입니다." }
 */
public record ErrorDetail(String field, Object rejectedValue, String reason) {
}
