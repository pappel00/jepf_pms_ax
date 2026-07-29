package com.platform.module.portal.dto;

import com.platform.module.portal.domain.MenuPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class MenuPageDto {

    private MenuPageDto() {
    }

    @Schema(name = "MenuPageCreateRequest")
    public record CreateRequest(
            @NotBlank @Schema(example = "PGE_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String pageId,
            @Schema(example = "1", defaultValue = "0") Integer sortOrder
    ) {
        /** sortOrder를 안 보내면(null) 0으로 취급한다 — Menu/CodeDetail의 CreateRequest와 동일한 이유. */
        public CreateRequest {
            if (sortOrder == null) {
                sortOrder = 0;
            }
        }
    }

    @Schema(name = "MenuPageMappingResponse")
    public record Response(
            String id,
            String tenantId,
            String menuId,
            String pageId,
            int sortOrder
    ) {
        public static Response from(MenuPage mapping) {
            return new Response(
                    mapping.getId(),
                    mapping.getTenantId(),
                    mapping.getMenuId(),
                    mapping.getPageId(),
                    mapping.getSortOrder()
            );
        }
    }
}
