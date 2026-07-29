package com.platform.module.user.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.user.dto.AuthDto;
import com.platform.module.user.dto.UserDto;
import com.platform.module.user.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * SecurityConfig 의 PERMIT_ALL 패턴("/api/v1/auth/**")에 걸리므로 인증 없이 호출 가능하다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "로그인/회원가입 (인증 불필요)")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto.Response> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return ApiResponse.success("201", "회원가입이 완료되었습니다.", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * refresh token으로 새 access/refresh token 쌍을 발급받는다. 기존 refresh token은 회전(revoke)된다 —
     * 응답으로 받은 새 refreshToken으로 갱신해서 저장해야 한다 (같은 refresh token을 재사용하면 실패한다).
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthDto.LoginResponse> refresh(@Valid @RequestBody AuthDto.RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    /** 제출한 refresh token 하나만 무효화한다 (그 기기/세션에서만 로그아웃). */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody AuthDto.LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success("200", "로그아웃되었습니다.", null);
    }
}
