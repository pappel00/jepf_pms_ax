package com.platform.framework.core.common.web;

import com.platform.framework.core.common.response.TraceIdHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 traceId 를 발급/전파한다.
 * 클라이언트가 X-Trace-Id 헤더로 요청했다면 그 값을 그대로 사용하고, 없으면 새로 생성한다.
 * MAD 4500 Response 규격의 traceId 필드는 이 필터에서 채워진 MDC 값을 사용한다.
 */
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TraceIdHolder.MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceIdHolder.MDC_KEY);
        }
    }
}
