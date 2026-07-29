package com.platform.framework.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.framework.core.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 인가 실패(권한 부족) 요청에 대해 MAD 4500 Response 규격을 따르는 403 응답을 반환한다. */
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.fail("ERR-AUTH-0004", "접근 권한이 없습니다.");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
