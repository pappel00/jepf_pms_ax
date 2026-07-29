package com.platform.framework.core.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void successResponseHasExpectedDefaults() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("200");
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getErrors()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void failResponseCarriesErrorCodeAndErrors() {
        List<ErrorDetail> errors = List.of(new ErrorDetail("password", "123", "비밀번호는 최소 8자리 이상이어야 합니다."));

        ApiResponse<Void> response = ApiResponse.fail("ERR-USER-4001", "입력값 검증에 실패했습니다.", errors);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("ERR-USER-4001");
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().get(0).field()).isEqualTo("password");
    }
}
