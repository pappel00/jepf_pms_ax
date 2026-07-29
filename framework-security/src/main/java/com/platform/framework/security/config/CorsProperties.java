package com.platform.framework.security.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * framework.security.cors.allowed-origins 로 프론트엔드 오리진을 설정한다.
 * 기본값은 Vite 개발 서버 기본 포트(5173)와 CRA 기본 포트(3000) — 로컬 프론트엔드 개발을
 * 바로 시작할 수 있도록 하기 위함이다. 운영 배포 시에는 반드시 실제 프론트 도메인으로
 * application.yml에서 재정의해야 한다.
 */
@ConfigurationProperties(prefix = "framework.security.cors")
public class CorsProperties {

    private List<String> allowedOrigins = List.of("http://localhost:5173", "http://localhost:3000");

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }
}
