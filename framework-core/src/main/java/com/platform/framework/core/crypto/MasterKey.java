package com.platform.framework.core.crypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

/**
 * 암호화 마스터 키 찾기 — 서버마다 한 번만 정해 두는 값이며 소스·저장소에는 두지 않는다.
 * 1) 환경변수 JEPF_SECRET_KEY  2) 키 파일(환경변수 JEPF_SECRET_KEY_FILE, 없으면 사용자 홈의 .jepf/secret.key 첫 줄) 순으로 찾는다.
 */
public final class MasterKey {

    public static final String ENV_KEY = "JEPF_SECRET_KEY";
    public static final String ENV_FILE = "JEPF_SECRET_KEY_FILE";

    private MasterKey() {
    }

    /** 환경변수/시스템 속성 → 키 파일 순서로 마스터 키를 찾는다. 없으면 빈 값. */
    public static Optional<String> find() {
        String fromEnv = firstNonBlank(System.getenv(ENV_KEY), System.getProperty(ENV_KEY));
        if (fromEnv != null) {
            return Optional.of(fromEnv.trim());
        }
        try {
            Path file = defaultFile();
            if (Files.isRegularFile(file)) {
                String line = Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(l -> !l.isBlank()).findFirst().orElse("");
                if (!line.isBlank()) {
                    return Optional.of(line.trim());
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // 읽을 수 없으면 키가 없는 것으로 본다(호출한 쪽이 안내 메시지를 낸다).
        }
        return Optional.empty();
    }

    /** 키 파일 위치(JEPF_SECRET_KEY_FILE 또는 ~/.jepf/secret.key). */
    public static Path defaultFile() {
        String custom = firstNonBlank(System.getenv(ENV_FILE), System.getProperty(ENV_FILE));
        return custom != null ? Path.of(custom.trim()) : Path.of(System.getProperty("user.home"), ".jepf", "secret.key");
    }

    /** 무작위 마스터 키(32바이트, base64) 생성. */
    public static String generate() {
        byte[] b = new byte[32];
        new SecureRandom().nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }
}
