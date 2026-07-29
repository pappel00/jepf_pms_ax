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
    // 표준양식은 텍스트가 아니라 실제 엑셀/워드 파일로 관리한다 — 실제 디스크 저장/조회는
    // module-file의 FileStorageService를 그대로 재사용한다(같은 저장 방식을 두 번 만들
    // 이유가 없다).
    implementation(project(":module-file"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
