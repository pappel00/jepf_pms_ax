plugins {
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.0")
    }
}

dependencies {
    // 4500 Response / 4600 Exception 규격 구현에 필요한 최소 스타터
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-data-jpa")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-autoconfigure")

    // API 문서 자동화(Swagger UI). 이 모듈에 추가하는 것만으로 이 프레임워크를 쓰는 모든 앱에서
    // /swagger-ui/index.html, /v3/api-docs 가 별도 설정 없이 바로 열린다.
    // ⚠️ 버전 확인 필요: springdoc-openapi 2.x는 Spring Boot 3.x(Spring 6) 기준으로 검증되어 있다.
    //   Spring Boot 4.x(Spring 7) 환경에서는 springdoc의 Boot4 호환 버전으로 조정이 필요할 수 있다.
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
