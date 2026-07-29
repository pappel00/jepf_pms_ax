package com.platform.module.user.dto;

import com.platform.module.user.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class RoleDto {

    private RoleDto() {
    }

    @Schema(name = "RoleCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 50) @Schema(example = "CNTN_OWNR") String roleCode,
            @NotBlank @Size(max = 100) @Schema(example = "콘텐츠 담당") String roleName,
            @Size(max = 50) @Schema(example = "Content Owner") String roleAlias,
            @Size(max = 50) @Schema(example = "콘텐츠 관리") String category,
            @Size(max = 500) @Schema(example = "콘텐츠 담당자 역할") String description
    ) {
    }

    @Schema(name = "RoleUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 100) @Schema(example = "콘텐츠 담당") String roleName,
            @Size(max = 50) @Schema(example = "Content Owner") String roleAlias,
            @Size(max = 50) @Schema(example = "콘텐츠 관리") String category,
            @Size(max = 500) @Schema(example = "콘텐츠 담당자 역할") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "RoleResponse")
    public record Response(
            String id,
            String tenantId,
            String roleCode,
            String roleName,
            String roleAlias,
            String category,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(Role role) {
            return new Response(
                    role.getId(),
                    role.getTenantId(),
                    role.getRoleCode(),
                    role.getRoleName(),
                    role.getRoleAlias(),
                    role.getCategory(),
                    role.getDescription(),
                    role.getUseYn(),
                    role.getDeletedYn()
            );
        }
    }
}
