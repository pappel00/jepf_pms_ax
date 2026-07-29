package com.platform.module.portal.dto;

import com.platform.module.portal.domain.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class MenuDto {

    private MenuDto() {
    }

    @Schema(name = "MenuCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "USR_MGT_MENU") String menuCode,
            @NotBlank @Size(max = 200) @Schema(example = "사용자 관리") String menuName,
            @NotBlank @Schema(example = "APP_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String applicationId,
            @Schema(example = "MNU_parent00000000000000000000") String parentMenuId,
            @Schema(example = "1", defaultValue = "0") Integer sortOrder,
            @Size(max = 500) @Schema(example = "https://example.com") String linkUrl
    ) {
        /** sortOrder를 안 보내면(null) 0으로 취급한다 — 필수값으로 강제하지 않는다. */
        public CreateRequest {
            if (sortOrder == null) {
                sortOrder = 0;
            }
        }
    }

    @Schema(name = "MenuUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "사용자 관리") String menuName,
            @Schema(example = "1", defaultValue = "0") Integer sortOrder,
            @Size(max = 500) @Schema(example = "https://example.com") String linkUrl,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
        public UpdateRequest {
            if (sortOrder == null) {
                sortOrder = 0;
            }
        }
    }

    @Schema(name = "MenuResponse")
    public record Response(
            String id,
            String tenantId,
            String menuCode,
            String menuName,
            String applicationId,
            String parentMenuId,
            int sortOrder,
            String linkUrl,
            String useYn,
            String deletedYn
    ) {
        public static Response from(Menu menu) {
            return new Response(
                    menu.getId(),
                    menu.getTenantId(),
                    menu.getMenuCode(),
                    menu.getMenuName(),
                    menu.getApplicationId(),
                    menu.getParentMenuId(),
                    menu.getSortOrder(),
                    menu.getLinkUrl(),
                    menu.getUseYn(),
                    menu.getDeletedYn()
            );
        }
    }

    /**
     * GET /api/v1/menus/tree 전용 응답. parentMenuId 를 이미 계층 구조로 조립해서 내려주므로
     * 프론트는 children 을 그대로 재귀 렌더링하면 된다 (평면 리스트를 직접 조립할 필요 없음).
     * pageIds 는 이 폴더에 매핑된 페이지 id 목록이다(MenuPage N:M 매핑, sortOrder 순).
     */
    @Schema(name = "MenuTreeNode")
    public record TreeNode(
            String id,
            String menuCode,
            String menuName,
            int sortOrder,
            List<String> pageIds,
            String linkUrl,
            String useYn,
            List<TreeNode> children
    ) {
        public static TreeNode of(Menu menu, List<String> pageIds, List<TreeNode> children) {
            return new TreeNode(
                    menu.getId(),
                    menu.getMenuCode(),
                    menu.getMenuName(),
                    menu.getSortOrder(),
                    pageIds,
                    menu.getLinkUrl(),
                    menu.getUseYn(),
                    children
            );
        }
    }
}
