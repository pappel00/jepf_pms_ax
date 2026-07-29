package com.platform.framework.security.filter;

import com.platform.framework.security.jwt.JwtTokenProvider;
import com.platform.framework.security.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer {token} 헤더를 검사하여 SecurityContext 와 TenantContext 를 채운다.
 * 토큰이 없거나 유효하지 않으면 인증 없이 다음 필터로 넘기고, 이후 authorizeHttpRequests 설정에 따라
 * 인증이 필요한 API는 RestAuthenticationEntryPoint 로 401 응답된다.
 *
 * refresh token(roles claim이 없고 tokenType=refresh)이 Authorization 헤더로 들어오면 인증을
 * 시도하지 않는다 — refresh token은 오직 /api/v1/auth/refresh 로만 제출되어야 한다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
                Claims claims = jwtTokenProvider.parseClaims(token);
                if (jwtTokenProvider.isAccessToken(claims)) {
                    authenticate(claims);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(Claims claims) {
        String userId = jwtTokenProvider.getUserId(claims);
        String tenantId = jwtTokenProvider.getTenantId(claims);
        List<String> roles = jwtTokenProvider.getRoles(claims);

        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();

        var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        if (StringUtils.hasText(tenantId)) {
            TenantContext.setTenantId(tenantId);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(PREFIX)) {
            return bearer.substring(PREFIX.length());
        }
        return null;
    }
}
