package com.platform.module.portal.dto;

import com.platform.module.portal.domain.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PageDto {

    private PageDto() {
    }

    @Schema(name = "PageCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "USR_LIST") String pageCode,
            @NotBlank @Size(max = 200) @Schema(example = "사용자 목록") String pageName,
            @NotBlank @Size(max = 20) @Schema(example = "일반") String pageType,
            @NotBlank @Size(max = 10) @Schema(example = "PC") String deviceType,
            @Size(max = 500) @Schema(example = "/ES/pages/user/UserList") String pageFile,
            @Size(max = 100) @Schema(example = "MSG_TXT_PGE_USR_LIST") String messageResourceCode
    ) {
    }

    @Schema(name = "PageUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "사용자 목록") String pageName,
            @NotBlank @Size(max = 20) @Schema(example = "일반") String pageType,
            @NotBlank @Size(max = 10) @Schema(example = "PC") String deviceType,
            @Size(max = 500) @Schema(example = "/ES/pages/user/UserList") String pageFile,
            @Size(max = 100) @Schema(example = "MSG_TXT_PGE_USR_LIST") String messageResourceCode,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "PageResponse")
    public record Response(
            String id,
            String tenantId,
            String pageCode,
            String pageName,
            String pageType,
            String deviceType,
            String pageFile,
            String messageResourceCode,
            String useYn,
            String deletedYn
    ) {
        public static Response from(Page page) {
            return new Response(
                    page.getId(),
                    page.getTenantId(),
                    page.getPageCode(),
                    page.getPageName(),
                    page.getPageType(),
                    page.getDeviceType(),
                    page.getPageFile(),
                    page.getMessageResourceCode(),
                    page.getUseYn(),
                    page.getDeletedYn()
            );
        }
    }
}
