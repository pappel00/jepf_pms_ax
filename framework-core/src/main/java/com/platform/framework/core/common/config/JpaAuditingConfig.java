package com.platform.framework.core.common.config;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * MAD 4800 Audit 규칙 구현을 위한 JPA Auditing 활성화.
 * 기본 AuditorAware는 "SYSTEM" 을 반환하며, framework-security 모듈이 클래스패스에 있으면
 * 인증된 사용자 정보를 사용하는 AuditorAware(SecurityAuditorAwareConfig)로 자동 교체된다.
 *
 * ※ @ConditionalOnMissingBean(AuditorAware.class) 대신 @ConditionalOnMissingClass 를 사용한다.
 *   Bean 존재 여부는 "어떤 AutoConfiguration이 먼저 처리되는가"라는 순서에 의존하게 되어
 *   FrameworkCoreAutoConfiguration이 FrameworkSecurityAutoConfiguration보다 먼저 처리되는 상황에서는
 *   오히려 core의 기본 Bean이 먼저 등록되어버리고, 뒤이어 security의 Bean이 같은 이름으로 등록을
 *   시도하며 충돌(BeanDefinitionOverrideException)한다. 클래스패스 검사는 처리 순서와 무관하게
 *   항상 같은 결과를 내므로 이 문제가 없다.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

    @Bean
    @ConditionalOnMissingClass("com.platform.framework.security.tenant.TenantContext")
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("SYSTEM");
    }
}

