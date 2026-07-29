package com.platform.framework.core.common.exception;

/**
 * MAD 4600 에러 코드 명명 규칙: ERR-[모듈]-[네자리숫자].
 * ERR-CORE-XXXX: 공통/시스템 에러 (DB 연결 실패, 널 포인터 등).
 * 모듈별 에러 코드는 각 모듈에서 별도 상수 클래스로 정의한다 (예: ERR-USER-XXXX).
 */
public final class CommonErrorCode {

    private CommonErrorCode() {
    }

    public static final String INTERNAL_ERROR = "ERR-CORE-0001";
    public static final String INVALID_REQUEST = "ERR-CORE-0002";
    public static final String NOT_FOUND = "ERR-CORE-0003";
}
