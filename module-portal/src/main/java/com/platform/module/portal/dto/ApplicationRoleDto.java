package com.platform.module.portal.dto;

import com.platform.module.portal.domain.ApplicationRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ApplicationRoleDto {

    private ApplicationRoleDto() {
    }

    @Schema(name = "ApplicationRoleCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 50) @Schema(example = "ROLE_ADMIN") String roleCode
    ) {
    }

    @Schema(name = "ApplicationRoleMappingResponse")
    public record Response(
            String id,
            String tenantId,
            String applicationId,
            String roleCode
    ) {
        public static Response from(ApplicationRole mapping) {
            return new Response(mapping.getId(), mapping.getTenantId(), mapping.getApplicationId(), mapping.getRoleCode());
        }
    }
}
