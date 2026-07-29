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

    implementation("org.springframework.boot:spring-boot-autoconfigure")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
