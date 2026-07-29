package com.platform.module.portal.dto;

import com.platform.module.portal.domain.PageRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PageRoleDto {

    private PageRoleDto() {
    }

    @Schema(name = "PageRoleCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 50) @Schema(example = "ROLE_ADMIN") String roleCode
    ) {
    }

    @Schema(name = "PageRoleMappingResponse")
    public record Response(
            String id,
            String tenantId,
            String pageId,
            String roleCode
    ) {
        public static Response from(PageRole mapping) {
            return new Response(mapping.getId(), mapping.getTenantId(), mapping.getPageId(), mapping.getRoleCode());
        }
    }
}
