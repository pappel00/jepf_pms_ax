package com.platform.framework.security.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * framework-core 의 기본 AuditorAware("SYSTEM" 고정)를 인증된 사용자 정보 기반으로 교체한다.
 * (framework-core.JpaAuditingConfig 가 @ConditionalOnMissingClass 로 이 모듈의 존재를 감지해
 *  스스로 양보하므로, 이 Bean이 항상 우선 적용된다 — 등록 순서와 무관하게 결정적으로 동작)
 */
@Configuration
public class SecurityAuditorAwareConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("SYSTEM");
            }
            return Optional.of(authentication.getName());
        };
    }
}
