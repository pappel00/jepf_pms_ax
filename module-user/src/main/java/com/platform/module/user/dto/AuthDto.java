package com.platform.module.user.dto;

import com.platform.module.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AuthDto {

    private AuthDto() {
    }

    @Schema(name = "AuthRegisterRequest")
    public record RegisterRequest(
            @NotBlank @Size(max = 30) @Schema(example = "DEMO") String companyCode,
            @NotBlank @Size(max = 100) @Schema(example = "bob") String loginId,
            @NotBlank @Size(min = 8, max = 100) @Schema(example = "bobpass1234") String password,
            @Size(max = 200) @Schema(example = "bob@example.com") String email,
            @NotBlank @Size(max = 100) @Schema(example = "Bob") String userName
    ) {
    }

    @Schema(name = "AuthLoginRequest")
    public record LoginRequest(
            @NotBlank @Size(max = 30) @Schema(example = "DEMO") String companyCode,
            @NotBlank @Size(max = 100) @Schema(example = "admin") String loginId,
            @NotBlank @Schema(example = "admin1234") String password
    ) {
    }

    @Schema(name = "AuthRefreshRequest")
    public record RefreshRequest(
            @NotBlank @Schema(example = "eyJhbGciOiJIUzI1NiJ9....") String refreshToken
    ) {
    }

    @Schema(name = "AuthLogoutRequest")
    public record LogoutRequest(
            @NotBlank @Schema(example = "eyJhbGciOiJIUzI1NiJ9....") String refreshToken
    ) {
    }

    @Schema(name = "AuthLoginResponse")
    public record LoginResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            String userId,
            String loginId,
            String tenantId,
            List<String> roles
    ) {
        public static LoginResponse of(String accessToken, String refreshToken, User user, String tenantId,
                                        List<String> roles) {
            return new LoginResponse(
                    accessToken,
                    refreshToken,
                    "Bearer",
                    user.getId(),
                    user.getLoginId(),
                    tenantId,
                    roles
            );
        }
    }
}
