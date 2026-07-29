plugins {
    java
}

allprojects {
    group = "com.platform"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // 필수: @PathVariable/@RequestParam 등에서 이름을 명시하지 않아도 Spring이 파라미터명을
    // 리플렉션으로 읽을 수 있도록 한다. 이게 없으면 "Name for argument of type [...] not specified"
    // 런타임 에러가 난다 (실제로 겪은 버그). Jackson의 record 역직렬화에도 도움이 된다.
    tasks.withType<JavaCompile> {
        options.compilerArgs.add("-parameters")

        // 필수: 소스 파일은 전부 UTF-8인데, 이걸 명시하지 않으면 javac가 JVM의 플랫폼 기본
        // 인코딩을 쓴다. 한국어 Windows에서는 그게 x-windows-949(CP949)라서 주석의 한글이
        // 깨져 읽히고, 운이 나쁘면 "unmappable character for encoding x-windows-949" 컴파일
        // 에러로 빌드 자체가 실패한다 (실제로 겪음 — PageCodeBootstrapListener 추가 시 발생).
        // 기존 파일들이 여태 통과한 건 UTF-8 바이트가 우연히 CP949로도 해석 가능했기 때문일
        // 뿐이라, 새 한글 주석을 추가할 때마다 재발할 수 있는 문제였다.
        options.encoding = "UTF-8"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
