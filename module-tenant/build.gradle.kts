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
    api(project(":framework-security"))

    // ModuleTenantAutoConfiguration의 @AutoConfiguration/@EntityScan 등에서 사용.
    // 멀티모듈 환경에서의 전이 의존성 해석 문제를 피하기 위해 명시적으로 선언한다.
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
