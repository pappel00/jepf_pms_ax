package com.platform.framework.security.config;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * spring.jpa.open-in-view 기본값을 false로 강제한다 (앱이 명시적으로 재정의하면 그 값이 우선한다).
 *
 * 배경: open-in-view(Spring Boot 기본값 true)가 켜져 있으면 HTTP 요청 하나당 Hibernate Session
 * 하나가 요청 끝까지 재사용된다. TenantAwareEntity의 @TenantId는 "Session이 열리는 시점"에
 * 현재 테넌트를 확정하므로, 로그인/회원가입처럼 요청 초반엔 TenantContext가 비어있다가 중간에
 * 채워지는 흐름에서는 "첫 DB 접근 시점(테넌트 미확정)에 캐싱된 값"이 이후 TenantContext를
 * 설정해도 반영되지 않는 문제가 생긴다. 실제로 module-user 로그인 플로우에서 이 버그가 발생했다
 * (SELECT에 의도한 tenantId 대신 기본값 "SYSTEM"이 바인딩됨).
 *
 * EnvironmentPostProcessor는 AutoConfiguration.imports보다 훨씬 이른 시점(ApplicationContext
 * 생성 전)에 동작해야 하므로, 이 클래스만 예외적으로 META-INF/spring.factories 로 등록한다.
 */
public class OpenInViewDefaultEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // addLast: 이미 로드된 application.yml/properties 등 다른 모든 설정보다 우선순위가 낮다.
        // 즉 앱이 spring.jpa.open-in-view 를 명시적으로 지정하면 그 값이 이 기본값을 덮어쓴다.
        environment.getPropertySources().addLast(
                new MapPropertySource("framework-security-defaults",
                        Map.of("spring.jpa.open-in-view", false)));
    }
}
