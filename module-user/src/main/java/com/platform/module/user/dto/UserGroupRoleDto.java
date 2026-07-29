package com.platform.module.user.dto;

import com.platform.module.user.domain.UserGroupRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserGroupRoleDto {

    private UserGroupRoleDto() {
    }

    @Schema(name = "UserGroupRoleCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 50) @Schema(example = "ROLE_ADMIN") String roleCode
    ) {
    }

    @Schema(name = "UserGroupRoleMappingResponse")
    public record Response(
            String id,
            String tenantId,
            String userGroupId,
            String roleCode
    ) {
        public static Response from(UserGroupRole mapping) {
            return new Response(mapping.getId(), mapping.getTenantId(), mapping.getUserGroupId(), mapping.getRoleCode());
        }
    }
}
