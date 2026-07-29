package com.platform.module.user.dto;

import com.platform.module.user.domain.UserGroupUser;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class UserGroupUserDto {

    private UserGroupUserDto() {
    }

    @Schema(name = "UserGroupUserAssignRequest")
    public record AssignRequest(
            @NotBlank @Schema(example = "USR_2826be9e4bbb445db2e05729fcbd02b5") String userId
    ) {
    }

    @Schema(name = "UserGroupUserMappingResponse")
    public record Response(
            String id,
            String tenantId,
            String userGroupId,
            String userId
    ) {
        public static Response from(UserGroupUser mapping) {
            return new Response(mapping.getId(), mapping.getTenantId(), mapping.getUserGroupId(), mapping.getUserId());
        }
    }
}
