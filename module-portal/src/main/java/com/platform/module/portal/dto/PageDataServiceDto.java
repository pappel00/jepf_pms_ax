package com.platform.module.portal.dto;

import com.platform.module.portal.domain.PageDataService;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public final class PageDataServiceDto {

    private PageDataServiceDto() {
    }

    @Schema(name = "PageDataServiceCreateRequest")
    public record CreateRequest(
            @NotBlank @Schema(example = "DTS_a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4") String dataServiceId
    ) {
    }

    @Schema(name = "PageDataServiceMappingResponse")
    public record Response(
            String id,
            String tenantId,
            String pageId,
            String dataServiceId
    ) {
        public static Response from(PageDataService mapping) {
            return new Response(mapping.getId(), mapping.getTenantId(), mapping.getPageId(), mapping.getDataServiceId());
        }
    }
}
