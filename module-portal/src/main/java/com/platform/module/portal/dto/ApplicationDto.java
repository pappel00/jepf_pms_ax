package com.platform.module.portal.dto;

import com.platform.module.portal.domain.Application;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ApplicationDto {

    private ApplicationDto() {
    }

    @Schema(name = "ApplicationCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "USR_MGT") String applicationCode,
            @NotBlank @Size(max = 200) @Schema(example = "사용자 관리") String applicationName,
            @NotBlank @Schema(example = "PRT_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String portalId
    ) {
    }

    @Schema(name = "ApplicationUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "사용자 관리") String applicationName,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "ApplicationResponse")
    public record Response(
            String id,
            String tenantId,
            String applicationCode,
            String applicationName,
            String portalId,
            String useYn,
            String deletedYn
    ) {
        public static Response from(Application application) {
            return new Response(
                    application.getId(),
                    application.getTenantId(),
                    application.getApplicationCode(),
                    application.getApplicationName(),
                    application.getPortalId(),
                    application.getUseYn(),
                    application.getDeletedYn()
            );
        }
    }
}
