package com.platform.module.portal.dto;

import com.platform.module.portal.domain.Portal;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PortalDto {

    private PortalDto() {
    }

    @Schema(name = "PortalCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "ADMIN") String portalCode,
            @NotBlank @Size(max = 200) @Schema(example = "Admin Portal") String portalName,
            @NotBlank @Size(max = 10) @Schema(example = "PC") String deviceType,
            @Size(max = 500) @Schema(example = "관리자용 포탈") String description
    ) {
    }

    @Schema(name = "PortalUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "Admin Portal") String portalName,
            @NotBlank @Size(max = 10) @Schema(example = "PC") String deviceType,
            @Size(max = 500) @Schema(example = "관리자용 포탈") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "PortalResponse")
    public record Response(
            String id,
            String tenantId,
            String portalCode,
            String portalName,
            String deviceType,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(Portal portal) {
            return new Response(
                    portal.getId(),
                    portal.getTenantId(),
                    portal.getPortalCode(),
                    portal.getPortalName(),
                    portal.getDeviceType(),
                    portal.getDescription(),
                    portal.getUseYn(),
                    portal.getDeletedYn()
            );
        }
    }
}
