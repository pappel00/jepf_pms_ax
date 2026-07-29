plugins {
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.0")
    }
}

dependencies {
    // module-tenant 를 통해 framework-core/framework-security 도 함께 딸려온다 (Company 참조용).
    api(project(":module-tenant"))
    // 사용자그룹의 분류(category) 선택지를 공통코드로 관리하기 위해 의존한다 — 새 테넌트가 생기면
    // UserGroupCodeBootstrapListener가 이 코드그룹/코드상세를 자동으로 만든다. module-notice/
    // module-portal이 같은 이유로 module-code에 의존하는 것과 동일한 패턴이다.
    implementation(project(":module-code"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
