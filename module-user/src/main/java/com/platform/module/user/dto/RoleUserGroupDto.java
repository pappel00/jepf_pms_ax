package com.platform.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class RoleUserGroupDto {

    private RoleUserGroupDto() {
    }

    @Schema(name = "RoleUserGroupCreateRequest")
    public record CreateRequest(
            @NotBlank @Schema(example = "UGR_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String userGroupId
    ) {
    }
}
