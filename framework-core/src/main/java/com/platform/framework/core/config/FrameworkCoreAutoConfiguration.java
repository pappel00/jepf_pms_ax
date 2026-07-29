package com.platform.framework.core.config;

import com.platform.framework.core.common.config.JpaAuditingConfig;
import com.platform.framework.core.common.config.OpenApiConfig;
import com.platform.framework.core.common.exception.GlobalExceptionHandler;
import com.platform.framework.core.common.web.TraceIdFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * framework-core 모듈의 Spring Boot 자동 구성 진입점.
 * 이 모듈을 의존성에 추가하는 것만으로 공통 Response/Exception/Audit/TraceId/Swagger 기능이
 * 자동으로 활성화된다 (META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 참고).
 */
@AutoConfiguration
@Import({JpaAuditingConfig.class, GlobalExceptionHandler.class, OpenApiConfig.class})
public class FrameworkCoreAutoConfiguration {

    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilterRegistration() {
        FilterRegistrationBean<TraceIdFilter> registration = new FilterRegistrationBean<>(new TraceIdFilter());
        registration.setOrder(Integer.MIN_VALUE);
        registration.addUrlPatterns("/*");
        registration.setName("traceIdFilter");
        return registration;
    }
}
