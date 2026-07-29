package com.platform.framework.core.common.exception;

import com.platform.framework.core.common.response.ErrorDetail;
import java.util.List;

/** 값 검증 실패 예외: 유효성 검사 실패 등. Response 의 errors 배열과 함께 사용된다. */
public class InvalidRequestException extends BaseException {

    private final List<ErrorDetail> errors;

    public InvalidRequestException(String errorCode, String message) {
        this(errorCode, message, List.of());
    }

    public InvalidRequestException(String errorCode, String message, List<ErrorDetail> errors) {
        super(errorCode, message);
        this.errors = errors;
    }

    public List<ErrorDetail> getErrors() {
        return errors;
    }
}
