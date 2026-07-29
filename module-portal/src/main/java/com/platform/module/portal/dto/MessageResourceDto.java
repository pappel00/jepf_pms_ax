package com.platform.module.portal.dto;

import com.platform.module.portal.domain.MessageResource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class MessageResourceDto {

    private MessageResourceDto() {
    }

    @Schema(name = "MessageResourceCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 100) @Schema(example = "MSG_ALT_CHK_UNIT_DUP") String messageResourceCode,
            @NotBlank @Size(max = 20) @Schema(example = "Alert") String resourceType,
            @NotBlank @Size(max = 10) @Schema(example = "ko") String languageId,
            @NotBlank @Size(max = 2000) @Schema(example = "이미 등록된 정보가 존재합니다.") String value,
            @Size(max = 2000) @Schema(example = "이미 등록된 정보가 존재합니다.") String defaultValue
    ) {
    }

    @Schema(name = "MessageResourceUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 2000) @Schema(example = "이미 등록된 정보가 존재합니다.") String value,
            @Size(max = 2000) @Schema(example = "이미 등록된 정보가 존재합니다.") String defaultValue,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "MessageResourceResponse")
    public record Response(
            String id,
            String tenantId,
            String messageResourceCode,
            String resourceType,
            String languageId,
            String value,
            String defaultValue,
            String useYn,
            String deletedYn
    ) {
        public static Response from(MessageResource messageResource) {
            return new Response(
                    messageResource.getId(),
                    messageResource.getTenantId(),
                    messageResource.getMessageResourceCode(),
                    messageResource.getResourceType(),
                    messageResource.getLanguageId(),
                    messageResource.getValue(),
                    messageResource.getDefaultValue(),
                    messageResource.getUseYn(),
                    messageResource.getDeletedYn()
            );
        }
    }
}
