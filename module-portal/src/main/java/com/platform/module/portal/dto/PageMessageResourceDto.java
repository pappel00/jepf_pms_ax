package com.platform.module.portal.dto;

import com.platform.module.portal.domain.PageMessageResource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PageMessageResourceDto {

    private PageMessageResourceDto() {
    }

    @Schema(name = "PageMessageResourceCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 100) @Schema(example = "MSG_ALT_CHK_UNIT_DUP") String messageResourceCode
    ) {
    }

    @Schema(name = "PageMessageResourceMappingResponse")
    public record Response(
            String id,
            String tenantId,
            String pageId,
            String messageResourceCode
    ) {
        public static Response from(PageMessageResource mapping) {
            return new Response(mapping.getId(), mapping.getTenantId(), mapping.getPageId(), mapping.getMessageResourceCode());
        }
    }
}
