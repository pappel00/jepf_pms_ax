package com.platform.framework.core.crypto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * 설정값 자동 복호화 — application.yml 등에 적은 {@code ENC(...)} 값(플레이스홀더 기본값 {@code ${ENV:ENC(...)} 포함)을 서버 시작 때
 * 복호화해 같은 이름의 설정으로 덮어쓴다. 코드는 평소처럼 {@code @Value}·{@code environment.getProperty} 로 읽으면 평문을 받는다.
 * 환경변수 등으로 평문이 주어지면 그 값이 우선하므로 그대로 쓴다. ENC 값이 있는데 마스터 키가 없거나 틀리면 시작을 막고 안내한다.
 * 등록: META-INF/spring.factories. 설정 파일을 읽은 뒤에 동작하도록 우선순위를 가장 낮게 둔다.
 */
public class EncryptedPropertyPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String SOURCE_NAME = "decryptedSecrets";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Map<String, String> targets = new LinkedHashMap<>(); // 설정 이름 → ENC(...) 값
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (!(source instanceof EnumerablePropertySource<?> eps) || SOURCE_NAME.equals(source.getName())) {
                continue;
            }
            for (String name : eps.getPropertyNames()) {
                Object raw = eps.getProperty(name);
                if (!(raw instanceof String s) || !s.contains(SecretCipher.PREFIX) || targets.containsKey(name)) {
                    continue;
                }
                String resolved;
                try {
                    resolved = environment.getProperty(name); // 플레이스홀더를 풀어, 환경변수가 우선하는 최종 값을 본다
                } catch (RuntimeException e) {
                    continue;
                }
                if (SecretCipher.isEncrypted(resolved)) {
                    targets.put(name, resolved);
                }
            }
        }
        if (targets.isEmpty()) {
            return;
        }
        String key = MasterKey.find().orElseThrow(() -> new IllegalStateException(
                "암호화된 설정값(ENC(...))이 있는데 마스터 키가 없습니다. 환경변수 " + MasterKey.ENV_KEY + " 를 지정하거나 키 파일("
                        + MasterKey.defaultFile() + ")을 두세요. 대상 설정: " + targets.keySet()));
        SecretCipher cipher = new SecretCipher(key);
        Map<String, Object> decrypted = new LinkedHashMap<>();
        List<String> failed = new ArrayList<>();
        targets.forEach((name, enc) -> {
            try {
                decrypted.put(name, cipher.decrypt(enc));
            } catch (IllegalArgumentException e) {
                failed.add(name);
            }
        });
        if (!failed.isEmpty()) {
            throw new IllegalStateException("설정값 복호화에 실패했습니다(마스터 키가 다르거나 값이 손상됨). 대상 설정: " + failed);
        }
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, decrypted));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
