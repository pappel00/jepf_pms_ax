package com.platform.module.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class RolePageDto {

    private RolePageDto() {
    }

    @Schema(name = "RolePageCreateRequest")
    public record CreateRequest(
            @NotBlank @Schema(example = "PGE_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String pageId
    ) {
    }
}
