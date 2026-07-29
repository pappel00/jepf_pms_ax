package com.platform.module.tenant;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * module-tenant는 라이브러리 모듈이라 @SpringBootApplication 이 없다.
 * 통합 테스트에서 ApplicationContext를 띄우기 위한 테스트 전용 부트스트랩 클래스.
 * (framework-core/framework-security/module-tenant의 AutoConfiguration.imports 가
 *  classpath 스캔으로 자동 로드된다 — 패키지 위치와 무관)
 */
@SpringBootApplication
public class TenantModuleTestApplication {
}
