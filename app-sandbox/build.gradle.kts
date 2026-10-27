plugins {
    id("org.springframework.boot") version "4.0.0"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.0")
    }
}

dependencies {
    implementation(project(":module-user"))
    implementation(project(":module-portal"))
    implementation(project(":module-code"))
    implementation(project(":module-file"))
    implementation(project(":module-terms"))
    implementation(project(":module-notice"))
    implementation(project(":module-privacy"))
    implementation(project(":module-form"))
    implementation(project(":module-pms"))
    runtimeOnly("org.postgresql:postgresql:42.7.4")
}
