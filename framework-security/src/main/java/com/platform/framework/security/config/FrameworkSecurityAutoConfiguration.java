package com.platform.framework.security.config;

import com.platform.framework.core.config.FrameworkCoreAutoConfiguration;
import com.platform.framework.security.jwt.JwtProperties;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

/**
 * framework-security 모듈의 Spring Boot 자동 구성 진입점.
 * framework-core 이후에 적용되어야 auditorProvider 교체가 정상 동작한다.
 *
 * @ComponentScan 이 필요한 이유: JwtTokenProvider 등 @Component 로 선언된 클래스는
 * 이 모듈을 사용하는 애플리케이션의 기본 패키지가 com.platform.framework.security 하위가
 * 아닌 경우 컴포넌트 스캔 대상에서 누락되어 빈이 아예 등록되지 않는다. 명시적으로 이 모듈의
 * 패키지를 스캔 범위에 포함시켜, 어느 애플리케이션에 추가하든 항상 동작하도록 한다.
 */
@AutoConfiguration
@AutoConfigureAfter(FrameworkCoreAutoConfiguration.class)
@EnableConfigurationProperties(JwtProperties.class)
@ComponentScan(basePackages = "com.platform.framework.security")
@Import({SecurityConfig.class, SecurityAuditorAwareConfig.class, TenantHibernateConfig.class})
public class FrameworkSecurityAutoConfiguration {
}

