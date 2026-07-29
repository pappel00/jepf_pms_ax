plugins {
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.0")
    }
}

dependencies {
    // module-tenant 를 통해 framework-core/framework-security 도 함께 딸려온다.
    api(project(":module-tenant"))
    // 페이지의 용도(일반/팝업)/디바이스유형(PC/MOBILE/ALL) 선택지를 공통코드로 관리하기 위해
    // 의존한다 — 새 테넌트가 생기면 PageCodeBootstrapListener가 이 코드그룹/코드상세를 자동으로
    // 만든다. module-notice가 팝업 상세 설정을 공통코드로 관리하는 것과 동일한 패턴이다.
    // (module-code는 module-portal을 모르므로 순환 의존이 생기지 않는다.)
    implementation(project(":module-code"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
