plugins {
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.0")
    }
}

dependencies {
    api(project(":framework-core"))
    api("org.springframework.boot:spring-boot-starter-security")

    // TenantHibernateConfig 의 HibernatePropertiesCustomizer 등에서 사용.
    // framework-core가 api로 내려주고 있지만, 멀티모듈 환경에서의 전이 의존성 해석 문제를
    // 피하기 위해 이 모듈에도 명시적으로 선언한다.
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // api로 선언: JwtTokenProvider.parseClaims()가 반환하는 io.jsonwebtoken.Claims 타입을
    // 하위 모듈(module-user의 AuthService 등)이 직접 참조해야 해서, implementation으로 숨기면
    // "package io.jsonwebtoken does not exist" 컴파일 에러가 난다 (실제로 겪은 버그).
    api("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // 필수: RestAuthenticationEntryPoint / RestAccessDeniedHandler 에서 ObjectMapper로
    // ApiResponse를 직접 JSON 직렬화하는 데 사용 (Spring MVC의 자동 구성 Jackson과 별개로 명시)
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")
    // 추천: Java 8 Date/Time API (LocalDateTime 등) 지원 모듈
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.1")
    // 추천: Java 8 Optional 등 JDK8 기능 지원 모듈
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.18.1")
    // 추천: 생성자 매개변수 이름 인식 지원 모듈 (ApiResponse/ErrorDetail 등 record/생성자 직렬화 시 유용)
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.18.1")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
