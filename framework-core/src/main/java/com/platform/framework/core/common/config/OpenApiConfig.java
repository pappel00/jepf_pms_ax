package com.platform.framework.core.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 기본 설정. 이 모듈을 의존성에 추가하는 것만으로
 * /swagger-ui/index.html, /v3/api-docs 가 별도 설정 없이 열린다
 * (SecurityConfig 에 이미 두 경로 모두 permitAll 로 등록되어 있다).
 *
 * Swagger UI 우측 상단 "Authorize" 버튼으로 JWT를 넣으면, 이후 모든 요청에
 * Authorization: Bearer {token} 헤더가 자동으로 붙는다.
 *
 * 앱에서 title/version 등을 커스터마이징하고 싶으면, 앱 쪽에 OpenAPI 타입 Bean을
 * 직접 정의하면 이 기본 Bean은 @ConditionalOnMissingBean 에 의해 자동으로 양보한다.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME_NAME = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI defaultOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java Enterprise Platform Framework API")
                        .version("v1")
                        .description("MAD(Master Architecture Document) 기준 구현. "
                                + "인증이 필요한 API는 우측 상단 Authorize 버튼으로 JWT를 입력하세요."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME_NAME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME_NAME));
    }
}
