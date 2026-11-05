package com.platform.framework.core.crypto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 설정값 암호화 도구(명령줄). Gradle 로 실행한다:
 * <pre>
 *   ./gradlew -q :framework-core:secretTool --args="generate-key"   마스터 키 생성(키 파일에 저장, 이미 있으면 거부)
 *   ./gradlew -q :framework-core:secretTool --args="encrypt"        표준입력 첫 줄을 암호화해 ENC(...) 출력
 *   ./gradlew -q :framework-core:secretTool --args="decrypt"        표준입력 첫 줄의 ENC(...) 를 복호화(확인용)
 * </pre>
 * 비밀값을 명령줄 인자로 넘기지 않고 표준입력으로 받아 셸 기록에 남지 않게 한다.
 */
public final class SecretCipherTool {

    private SecretCipherTool() {
    }

    public static void main(String[] args) throws IOException {
        String cmd = args.length > 0 ? args[0] : "";
        switch (cmd) {
            case "generate-key" -> generateKey();
            case "encrypt" -> System.out.println(cipher().encrypt(readLine("암호화할 값")));
            case "decrypt" -> System.out.println(cipher().decrypt(readLine("ENC(...) 값").trim()));
            default -> {
                System.err.println("사용법: generate-key | encrypt | decrypt");
                System.exit(2);
            }
        }
    }

    private static void generateKey() throws IOException {
        Path file = MasterKey.defaultFile();
        if (Files.exists(file)) {
            System.err.println("이미 키 파일이 있어 덮어쓰지 않습니다: " + file);
            System.exit(1);
        }
        Files.createDirectories(file.getParent());
        Files.writeString(file, MasterKey.generate() + System.lineSeparator(), StandardCharsets.UTF_8);
        System.out.println("마스터 키를 만들었습니다: " + file + " — 이 파일은 저장소에 넣지 말고 서버마다 따로 보관하세요.");
    }

    private static SecretCipher cipher() {
        return new SecretCipher(MasterKey.find().orElseThrow(() -> new IllegalStateException(
                "마스터 키가 없습니다. generate-key 로 만들거나 환경변수 " + MasterKey.ENV_KEY + " 를 지정하세요.")));
    }

    private static String readLine(String what) throws IOException {
        System.err.println(what + "을(를) 한 줄로 입력하세요:");
        String line = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        if (line == null || line.isEmpty()) {
            throw new IllegalArgumentException("입력이 비어 있습니다.");
        }
        return line;
    }
}
