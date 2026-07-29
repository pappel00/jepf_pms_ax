package com.platform.framework.security.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.framework.core.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 인증되지 않은 요청에 대해 MAD 4500 Response 규격을 따르는 401 응답을 반환한다. */
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.fail("ERR-AUTH-0003", "인증이 필요합니다.");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
