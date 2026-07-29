package com.platform.sandbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 로컬 수동 테스트(Postman 등)를 위한 실행 가능한 샌드박스 애플리케이션.
 * module-tenant 및 그 하위 framework-core/framework-security를 실제 서버로 띄운다.
 *
 * 실행: IDE에서 이 클래스 실행, 또는 `./gradlew :app-sandbox:bootRun`
 * 기본 포트: 8080
 */
@SpringBootApplication
public class SandboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(SandboxApplication.class, args);
    }
}
