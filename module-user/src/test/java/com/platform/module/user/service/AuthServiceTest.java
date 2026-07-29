package com.platform.module.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.SecurityException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.security.jwt.JwtProperties;
import com.platform.framework.security.jwt.JwtTokenProvider;
import com.platform.module.tenant.domain.Company;
import com.platform.module.tenant.repository.CompanyRepository;
import com.platform.module.user.domain.User;
import com.platform.module.user.dto.AuthDto;
import com.platform.module.user.repository.RefreshTokenRepository;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import com.platform.module.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private UserGroupUserRepository userGroupUserRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private EffectiveRoleService effectiveRoleService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtProperties jwtProperties = new JwtProperties();

    private AuthService authService() {
        return new AuthService(companyRepository, userRepository, userGroupRepository, userGroupUserRepository,
                refreshTokenRepository, effectiveRoleService, passwordEncoder, jwtTokenProvider, jwtProperties);
    }

    @AfterEach
    void clearTenant() {
        com.platform.framework.security.tenant.TenantContext.clear();
    }

    @Test
    void loginFailsWhenCompanyNotFound() {
        when(companyRepository.findByCompanyCodeAndDeletedYn("NOPE", "N")).thenReturn(Optional.empty());

        var request = new AuthDto.LoginRequest("NOPE", "admin", "password1234");

        assertThatThrownBy(() -> authService().login(request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void loginFailsWhenUserNotFound() {
        Company company = Company.create("ACME", "에이씨엠이", null);
        when(companyRepository.findByCompanyCodeAndDeletedYn("ACME", "N")).thenReturn(Optional.of(company));
        when(userRepository.findByLoginIdAndDeletedYn("ghost", "N")).thenReturn(Optional.empty());

        var request = new AuthDto.LoginRequest("ACME", "ghost", "password1234");

        assertThatThrownBy(() -> authService().login(request))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void loginFailsWhenPasswordDoesNotMatch() {
        Company company = Company.create("ACME", "에이씨엠이", null);
        User user = User.create("admin", passwordEncoder.encode("correct-password"), "a@a.com", "관리자");
        when(companyRepository.findByCompanyCodeAndDeletedYn("ACME", "N")).thenReturn(Optional.of(company));
        when(userRepository.findByLoginIdAndDeletedYn("admin", "N")).thenReturn(Optional.of(user));

        var request = new AuthDto.LoginRequest("ACME", "admin", "wrong-password");

        assertThatThrownBy(() -> authService().login(request))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void loginSucceedsAndReturnsAccessAndRefreshToken() {
        Company company = Company.create("ACME", "에이씨엠이", null);
        User user = User.create("admin", passwordEncoder.encode("correct-password"), "a@a.com", "관리자");
        when(companyRepository.findByCompanyCodeAndDeletedYn("ACME", "N")).thenReturn(Optional.of(company));
        when(userRepository.findByLoginIdAndDeletedYn("admin", "N")).thenReturn(Optional.of(user));
        when(effectiveRoleService.resolveRoles(user.getId())).thenReturn(List.of("ROLE_USER"));
        when(jwtTokenProvider.createAccessToken(anyString(), anyString(), anyList())).thenReturn("dummy-access-jwt");
        when(jwtTokenProvider.createRefreshToken(anyString(), anyString())).thenReturn("dummy-refresh-jwt");

        var request = new AuthDto.LoginRequest("ACME", "admin", "correct-password");

        AuthDto.LoginResponse response = authService().login(request);

        assertThat(response.accessToken()).isEqualTo("dummy-access-jwt");
        assertThat(response.refreshToken()).isEqualTo("dummy-refresh-jwt");
        assertThat(response.tenantId()).isEqualTo(company.getId());
        assertThat(response.loginId()).isEqualTo("admin");
        assertThat(response.roles()).containsExactly("ROLE_USER");
    }

    @Test
    void registerFailsWhenLoginIdDuplicated() {
        Company company = Company.create("ACME", "에이씨엠이", null);
        when(companyRepository.findByCompanyCodeAndDeletedYn("ACME", "N")).thenReturn(Optional.of(company));
        when(userRepository.existsByLoginId("admin")).thenReturn(true);

        var request = new AuthDto.RegisterRequest("ACME", "admin", "password1234", "a@a.com", "관리자");

        assertThatThrownBy(() -> authService().register(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void registerAutoAssignsDefaultGroupWhenOneExists() {
        Company company = Company.create("ACME", "에이씨엠이", null);
        when(companyRepository.findByCompanyCodeAndDeletedYn("ACME", "N")).thenReturn(Optional.of(company));
        when(userRepository.existsByLoginId("bob")).thenReturn(false);
        when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var defaultGroup = com.platform.module.user.domain.UserGroup.create("DEFAULT_USER", "기본권한 사용자", null, null, "Y");
        when(userGroupRepository.findFirstByDefaultYnAndDeletedYn("Y", "N")).thenReturn(Optional.of(defaultGroup));
        when(effectiveRoleService.resolveRoles(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());

        var request = new AuthDto.RegisterRequest("ACME", "bob", "password1234", "bob@a.com", "Bob");
        authService().register(request);

        org.mockito.Mockito.verify(userGroupUserRepository)
                .saveAndFlush(org.mockito.ArgumentMatchers.any(com.platform.module.user.domain.UserGroupUser.class));
    }

    @Test
    void refreshFailsWhenTokenTypeIsNotRefresh() {
        var accessClaims = io.jsonwebtoken.Jwts.claims().build();
        when(jwtTokenProvider.parseClaims("access-token-used-as-refresh")).thenReturn(accessClaims);
        when(jwtTokenProvider.isRefreshToken(accessClaims)).thenReturn(false);

        var request = new AuthDto.RefreshRequest("access-token-used-as-refresh");

        assertThatThrownBy(() -> authService().refresh(request))
                .isInstanceOf(SecurityException.class);
    }

    /**
     * 동시에 같은 refreshToken으로 두 번 refresh 요청이 들어와서 낙관적 락 충돌
     * (ObjectOptimisticLockingFailureException)이 나는 상황을 재현한다 — 처리 안 된 500 대신
     * "이미 사용된 토큰"과 같은 SecurityException으로 변환돼야 한다 (실제로 겪은 버그).
     */
    @Test
    void refreshConvertsOptimisticLockFailureToSecurityException() {
        var refreshClaims = io.jsonwebtoken.Jwts.claims().subject("USR_1").build();
        when(jwtTokenProvider.parseClaims("racing-refresh-token")).thenReturn(refreshClaims);
        when(jwtTokenProvider.isRefreshToken(refreshClaims)).thenReturn(true);
        when(jwtTokenProvider.getTenantId(refreshClaims)).thenReturn("TNT_1");

        com.platform.module.user.domain.RefreshToken stored = com.platform.module.user.domain.RefreshToken.create(
                "USR_1", "dummy-hash", java.time.LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHashAndRevokedYn(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("N")))
                .thenReturn(Optional.of(stored));
        when(refreshTokenRepository.saveAndFlush(stored))
                .thenThrow(new org.springframework.orm.ObjectOptimisticLockingFailureException(
                        com.platform.module.user.domain.RefreshToken.class, stored.getId()));

        var request = new AuthDto.RefreshRequest("racing-refresh-token");

        assertThatThrownBy(() -> authService().refresh(request))
                .isInstanceOf(SecurityException.class);
    }
}
