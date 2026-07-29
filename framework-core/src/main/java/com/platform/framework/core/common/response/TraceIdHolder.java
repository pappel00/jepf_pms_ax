package com.platform.framework.core.common.response;

import org.slf4j.MDC;

/**
 * 요청 단위 traceId를 MDC에서 읽어오기 위한 헬퍼.
 * traceId는 {@link com.platform.framework.core.common.web.TraceIdFilter} 에서 채워진다.
 */
public final class TraceIdHolder {

    public static final String MDC_KEY = "traceId";

    private TraceIdHolder() {
    }

    public static String get() {
        String traceId = MDC.get(MDC_KEY);
        return traceId != null ? traceId : "-";
    }
}
