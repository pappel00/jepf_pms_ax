package com.platform.module.user.dto;

import com.platform.module.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class UserDto {

    private UserDto() {
    }

    /**
     * 관리자가 사용자 관리 화면에서 직접 사용자를 만들 때 쓴다. 회원가입(AuthService.register)과
     * 달리 companyCode가 없다 — 이미 인증된 ADMIN의 TenantContext로 자동 범위 제한되기 때문이다
     * (UserController가 다른 모든 API와 동일하게 로그인한 관리자의 테넌트 안에서만 동작한다).
     * departmentId는 선택이다 — 아직 부서를 정하지 않고 사용자만 먼저 만들 수도 있다.
     */
    @Schema(name = "UserCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 100) @Schema(example = "bob") String loginId,
            @NotBlank @Size(min = 8, max = 100) @Schema(example = "bobpass1234") String password,
            @Size(max = 200) @Schema(example = "bob@example.com") String email,
            @NotBlank @Size(max = 100) @Schema(example = "Bob") String userName,
            @Schema(example = "DEPT_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String departmentId
    ) {
    }

    @Schema(name = "UserUpdateRequest")
    public record UpdateRequest(
            @Size(max = 200) @Schema(example = "bob@example.com") String email,
            @NotBlank @Size(max = 100) @Schema(example = "Bob") String userName,
            @Schema(example = "DEPT_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String departmentId,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    /**
     * roles 는 User가 직접 가진 값이 아니라 EffectiveRoleService가 UserGroup을 통해 계산한 값이다
     * (Role -> UserGroup -> User 구조, User.java 클래스 주석 참고). 그래서 Response.from(User)
     * 대신 roles를 별도로 받는 오버로드만 제공한다 — 호출부가 직접 계산해서 넘겨야 한다는 뜻을
     * 명시적으로 드러내기 위함이다.
     *
     * tenantId(회사)는 모든 TenantAwareEntity가 자동으로 갖는 값이라 여기 그대로 노출한다 —
     * 회사<->부서<->사용자 연결고리 중 "회사" 쪽은 원래부터 있었고, departmentId가 이번에
     * 추가되면서 마지막 단계(부서<->사용자)도 채워졌다(User.java 클래스 주석 참고).
     */
    @Schema(name = "UserResponse")
    public record Response(
            String id,
            String tenantId,
            String loginId,
            String email,
            String userName,
            String departmentId,
            String useYn,
            String deletedYn,
            List<String> roles
    ) {
        public static Response from(User user, List<String> roles) {
            return new Response(
                    user.getId(),
                    user.getTenantId(),
                    user.getLoginId(),
                    user.getEmail(),
                    user.getUserName(),
                    user.getDepartmentId(),
                    user.getUseYn(),
                    user.getDeletedYn(),
                    roles
            );
        }
    }
}
