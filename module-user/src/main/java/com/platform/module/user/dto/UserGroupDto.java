package com.platform.module.user.dto;

import com.platform.module.user.domain.UserGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserGroupDto {

    private UserGroupDto() {
    }

    @Schema(name = "UserGroupCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "CNTN_MGT") String groupCode,
            @NotBlank @Size(max = 200) @Schema(example = "콘텐츠 관리자") String groupName,
            @Size(max = 50) @Schema(example = "콘텐츠 관리") String category,
            @Schema(example = "PRT_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String portalId,
            @Schema(example = "N") String defaultYn
    ) {
    }

    @Schema(name = "UserGroupUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "콘텐츠 관리자") String groupName,
            @Size(max = 50) @Schema(example = "콘텐츠 관리") String category,
            @Schema(example = "PRT_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String portalId,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "UserGroupResponse")
    public record Response(
            String id,
            String tenantId,
            String groupCode,
            String groupName,
            String category,
            String portalId,
            String defaultYn,
            String useYn,
            String deletedYn
    ) {
        public static Response from(UserGroup userGroup) {
            return new Response(
                    userGroup.getId(),
                    userGroup.getTenantId(),
                    userGroup.getGroupCode(),
                    userGroup.getGroupName(),
                    userGroup.getCategory(),
                    userGroup.getPortalId(),
                    userGroup.getDefaultYn(),
                    userGroup.getUseYn(),
                    userGroup.getDeletedYn()
            );
        }
    }
}
