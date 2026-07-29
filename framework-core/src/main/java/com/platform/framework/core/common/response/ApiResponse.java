package com.platform.framework.core.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * MAD 4500. Response 규격에 정의된 공통 Response Wrapper.
 * 모든 Controller는 이 클래스를 통해 응답을 생성한다 (Framework Response Builder).
 *
 * 필드: success, code, message, data, pagination, errors, timestamp, traceId
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final PageInfo pagination;
    private final List<ErrorDetail> errors;
    private final Instant timestamp;
    private final String traceId;

    private ApiResponse(boolean success, String code, String message, T data,
                         PageInfo pagination, List<ErrorDetail> errors) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        this.pagination = pagination;
        this.errors = errors;
        this.timestamp = Instant.now();
        this.traceId = TraceIdHolder.get();
    }

    // ---------- 성공 응답 ----------

    public static <T> ApiResponse<T> success(T data) {
        return success("200", "요청이 성공적으로 처리되었습니다.", data);
    }

    public static <T> ApiResponse<T> success(String code, String message, T data) {
        return new ApiResponse<>(true, code, message, data, null, null);
    }

    public static <T> ApiResponse<List<T>> successList(List<T> data, PageInfo pagination) {
        return new ApiResponse<>(true, "200", "성공", data, pagination, null);
    }

    // ---------- 실패 응답 ----------

    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null, null, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message, List<ErrorDetail> errors) {
        return new ApiResponse<>(false, code, message, null, null, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public PageInfo getPagination() {
        return pagination;
    }

    public List<ErrorDetail> getErrors() {
        return errors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getTraceId() {
        return traceId;
    }
}
