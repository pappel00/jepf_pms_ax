package com.platform.framework.security.config;

import com.platform.framework.security.filter.JwtAuthenticationFilter;
import com.platform.framework.security.jwt.JwtTokenProvider;
import com.platform.framework.security.web.RestAccessDeniedHandler;
import com.platform.framework.security.web.RestAuthenticationEntryPoint;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * MAD 4200 core/security 의 최소 구현.
 * - Stateless (세션 미사용, JWT 기반)
 * - /api/v1/auth/** 는 인증 없이 접근 허용 (로그인/토큰 발급용)
 * - 그 외 모든 요청은 인증 필요
 *
 * @EnableMethodSecurity 로 각 모듈의 컨트롤러/서비스에서 @PreAuthorize("hasRole('ADMIN')") 같은
 * 메서드 단위 권한 체크를 쓸 수 있다. 권한 거부(AccessDeniedException)는 별도 배선 없이
 * 아래 accessDeniedHandler(RestAccessDeniedHandler)가 그대로 처리해 ApiResponse 포맷 403을 반환한다.
 *
 * CORS: 프론트엔드(React/Vite 등)가 다른 오리진에서 이 API를 호출할 수 있도록 허용한다.
 * 허용 오리진은 CorsProperties(framework.security.cors.allowed-origins)로 설정한다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private static final String[] PERMIT_ALL = {
            "/api/v1/auth/**",
            "/actuator/health",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs",
            "/v3/api-docs/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider,
                                            CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                        .accessDeniedHandler(new RestAccessDeniedHandler()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // 프론트가 응답 헤더에서 traceId 등을 읽을 수 있게 노출한다 (필요시 확장).
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
