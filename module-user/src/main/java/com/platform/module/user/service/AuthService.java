package com.platform.module.user.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.SecurityException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.security.jwt.JwtProperties;
import com.platform.framework.security.jwt.JwtTokenProvider;
import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.tenant.domain.Company;
import com.platform.module.tenant.repository.CompanyRepository;
import com.platform.module.user.domain.RefreshToken;
import com.platform.module.user.domain.User;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.domain.UserGroupUser;
import com.platform.module.user.dto.AuthDto;
import com.platform.module.user.dto.UserDto;
import com.platform.module.user.exception.UserModuleErrorCode;
import com.platform.module.user.repository.RefreshTokenRepository;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import com.platform.module.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 로그인/회원가입/refresh는 의도적으로 클래스/메서드 레벨 @Transactional 을 사용하지 않는다.
 *
 * 이유: Hibernate 의 CurrentTenantIdentifierResolver 는 "Session 이 열리는 시점"에 현재
 * 테넌트를 확정한다. 만약 이 메서드 전체를 하나의 @Transactional 로 묶으면, Company 조회
 * (테넌트 무관) 시점에 이미 Session 이 열려버려서 그 뒤에 TenantContext 를 설정해도 반영되지
 * 않을 수 있다. 대신 각 리포지토리 호출이 Spring Data JPA 의 기본 동작대로 자기만의 트랜잭션
 * (=새 Session) 을 열도록 두어, TenantContext 설정 이후에 실행되는 조회/저장 호출이
 * 항상 올바른 테넌트로 새 Session 을 여는 것을 보장한다.
 *
 * refresh() 는 tenantId를 refresh token의 서명된 claim에서 바로 얻으므로 login()/register()와
 * 달리 처음부터 tenantId를 알고 시작하지만, 일관성을 위해 같은 패턴(개별 트랜잭션)을 유지한다.
 *
 * ⚠️ 이 설계는 spring.jpa.open-in-view=false 를 전제로 한다 (framework-security의
 * OpenInViewDefaultEnvironmentPostProcessor 가 기본값을 강제한다).
 *
 * register() 는 사용자를 만든 뒤, 테넌트에 defaultYn="Y" 인 UserGroup(기본권한 그룹)이 있으면
 * 자동으로 그 그룹에 소속시킨다. 없으면 아무 그룹에도 안 속한 채로 가입되고, 관리자가 나중에
 * UserGroupUserController로 그룹에 넣어줘야 로그인 후 아무 API도 못 쓰는 상태를 벗어난다
 * (User가 더 이상 역할을 직접 갖지 않기 때문 — User.java 클래스 주석 참고).
 *
 * ⚠️ refresh()/logout() 이 같은 refreshToken으로 정확히 동시에 두 번 호출되면(레이스 컨디션 —
 * 실제로 겪음: 프론트에서 여러 API 요청이 동시에 401을 받아 각자 독립적으로 refresh를 시도하는
 * 경우), 둘 다 "아직 revoke 안 됨" 상태의 같은 RefreshToken 행을 읽어서 각자 저장을 시도한다.
 * 하나는 성공하고(@Version 증가), 다른 하나는 이미 바뀐 버전이라
 * ObjectOptimisticLockingFailureException 이 난다. 이건 버그가 아니라 낙관적 락이 의도대로
 * 동작한 것이지만, 그대로 두면 처리 안 된 500 에러로 튀어나온다 — "동시에 같은 토큰이 또
 * 쓰였다"는 건 의미상 REFRESH_TOKEN_NOT_FOUND(이미 사용됨)와 동일하므로, 잡아서 그 예외로
 * 변환한다 (아래 saveRevokedOrThrow 참고).
 */
@Service
public class AuthService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupUserRepository userGroupUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EffectiveRoleService effectiveRoleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthService(CompanyRepository companyRepository,
                        UserRepository userRepository,
                        UserGroupRepository userGroupRepository,
                        UserGroupUserRepository userGroupUserRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        EffectiveRoleService effectiveRoleService,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider,
                        JwtProperties jwtProperties) {
        this.companyRepository = companyRepository;
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupUserRepository = userGroupUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.effectiveRoleService = effectiveRoleService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    public UserDto.Response register(AuthDto.RegisterRequest request) {
        Company company = findCompanyOrThrow(request.companyCode());

        TenantContext.setTenantId(company.getId());
        try {
            if (userRepository.existsByLoginId(request.loginId())) {
                throw new BusinessException(UserModuleErrorCode.LOGIN_ID_DUPLICATED,
                        "이미 존재하는 로그인 아이디입니다: " + request.loginId());
            }
            User user = User.create(
                    request.loginId(),
                    passwordEncoder.encode(request.password()),
                    request.email(),
                    request.userName());
            User saved = userRepository.saveAndFlush(user);

            userGroupRepository.findFirstByDefaultYnAndDeletedYn("Y", "N")
                    .ifPresent(defaultGroup -> assignToGroup(saved.getId(), defaultGroup));

            return UserDto.Response.from(saved, effectiveRoleService.resolveRoles(saved.getId()));
        } finally {
            TenantContext.clear();
        }
    }

    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        Company company = findCompanyOrThrow(request.companyCode());

        TenantContext.setTenantId(company.getId());
        try {
            User user = userRepository.findByLoginIdAndDeletedYn(request.loginId(), "N")
                    .orElseThrow(() -> new SecurityException(
                            UserModuleErrorCode.INVALID_CREDENTIALS, "아이디 또는 비밀번호가 올바르지 않습니다."));

            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new SecurityException(
                        UserModuleErrorCode.INVALID_CREDENTIALS, "아이디 또는 비밀번호가 올바르지 않습니다.");
            }
            if (!"Y".equals(user.getUseYn())) {
                throw new SecurityException(
                        UserModuleErrorCode.USER_DISABLED, "비활성화된 사용자입니다.");
            }

            return issueTokens(user, company.getId());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * refresh token을 검증하고, 기존 토큰은 회전(revoke)한 뒤 새 access/refresh token 쌍을 발급한다.
     * 최초 로그인 이후 사용자그룹/역할 구성이 바뀌었을 수 있으므로, 매번 EffectiveRoleService로
     * 최신 권한을 다시 계산한다 — refresh는 단순 토큰 연장이 아니라 권한 재확인 시점이기도 하다.
     */
    public AuthDto.LoginResponse refresh(AuthDto.RefreshRequest request) {
        Claims claims = jwtTokenProvider.parseClaims(request.refreshToken());
        if (!jwtTokenProvider.isRefreshToken(claims)) {
            throw new SecurityException(UserModuleErrorCode.INVALID_TOKEN_TYPE,
                    "refresh token이 아닙니다.");
        }
        String tenantId = jwtTokenProvider.getTenantId(claims);

        TenantContext.setTenantId(tenantId);
        try {
            String tokenHash = sha256(request.refreshToken());
            RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedYn(tokenHash, "N")
                    .orElseThrow(() -> new SecurityException(UserModuleErrorCode.REFRESH_TOKEN_NOT_FOUND,
                            "이미 사용되었거나 로그아웃된 refresh token입니다. 다시 로그인해주세요."));

            stored.revoke();
            saveRevokedOrThrow(stored);

            if (stored.isExpired()) {
                throw new SecurityException(UserModuleErrorCode.REFRESH_TOKEN_NOT_FOUND,
                        "만료된 refresh token입니다. 다시 로그인해주세요.");
            }

            String userId = jwtTokenProvider.getUserId(claims);
            User user = userRepository.findById(userId)
                    .filter(u -> "N".equals(u.getDeletedYn()))
                    .orElseThrow(() -> new SecurityException(UserModuleErrorCode.INVALID_CREDENTIALS,
                            "존재하지 않는 사용자입니다."));
            if (!"Y".equals(user.getUseYn())) {
                throw new SecurityException(UserModuleErrorCode.USER_DISABLED, "비활성화된 사용자입니다.");
            }

            return issueTokens(user, tenantId);
        } finally {
            TenantContext.clear();
        }
    }

    /** 제출된 refresh token 하나만 무효화한다 (그 기기/세션에서만 로그아웃). */
    public void logout(AuthDto.LogoutRequest request) {
        Claims claims = jwtTokenProvider.parseClaims(request.refreshToken());
        String tenantId = jwtTokenProvider.getTenantId(claims);

        TenantContext.setTenantId(tenantId);
        try {
            String tokenHash = sha256(request.refreshToken());
            refreshTokenRepository.findByTokenHashAndRevokedYn(tokenHash, "N")
                    .ifPresent(stored -> {
                        stored.revoke();
                        try {
                            refreshTokenRepository.saveAndFlush(stored);
                        } catch (ObjectOptimisticLockingFailureException e) {
                            // 동시에 다른 요청(같은 토큰의 refresh 등)이 먼저 revoke했다는 뜻이다.
                            // logout의 목적(이 토큰을 못 쓰게 만드는 것)은 이미 달성된 상태이므로
                            // 에러로 취급하지 않고 조용히 넘어간다.
                        }
                    });
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * revoke() 후 저장한다. 동시에 같은 토큰으로 다른 요청이 먼저 revoke해서 낙관적 락 충돌이
     * 나면(클래스 주석 참고), "이미 사용된 토큰"과 의미상 동일하므로 SecurityException으로
     * 변환해서 던진다 — 처리 안 된 500 대신 정상적인 401 응답이 나가게 하기 위함이다.
     */
    private void saveRevokedOrThrow(RefreshToken stored) {
        try {
            refreshTokenRepository.saveAndFlush(stored);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SecurityException(UserModuleErrorCode.REFRESH_TOKEN_NOT_FOUND,
                    "이미 사용되었거나 로그아웃된 refresh token입니다. 다시 로그인해주세요.");
        }
    }

    private AuthDto.LoginResponse issueTokens(User user, String tenantId) {
        List<String> roles = effectiveRoleService.resolveRoles(user.getId());
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), tenantId, roles);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), tenantId);

        RefreshToken tokenRecord = RefreshToken.create(
                user.getId(), sha256(refreshToken),
                LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenValiditySeconds()));
        refreshTokenRepository.saveAndFlush(tokenRecord);

        return AuthDto.LoginResponse.of(accessToken, refreshToken, user, tenantId, roles);
    }

    private void assignToGroup(String userId, UserGroup group) {
        if (!userGroupUserRepository.existsByUserGroupIdAndUserId(group.getId(), userId)) {
            userGroupUserRepository.saveAndFlush(UserGroupUser.create(group.getId(), userId));
        }
    }

    private Company findCompanyOrThrow(String companyCode) {
        return companyRepository.findByCompanyCodeAndDeletedYn(companyCode, "N")
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.COMPANY_NOT_FOUND,
                        "존재하지 않는 회사 코드입니다: " + companyCode));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
