package com.platform.module.code.dto;

import com.platform.module.code.domain.CodeGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CodeGroupDto {

    private CodeGroupDto() {
    }

    @Schema(name = "CodeGroupCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 50) @Schema(example = "DEVICE_TYPE") String groupCode,
            @NotBlank @Size(max = 200) @Schema(example = "디바이스 유형") String groupName,
            @Size(max = 500) @Schema(example = "화면이 노출되는 디바이스 종류") String description
    ) {
    }

    @Schema(name = "CodeGroupUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "디바이스 유형") String groupName,
            @Size(max = 500) @Schema(example = "화면이 노출되는 디바이스 종류") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "CodeGroupResponse")
    public record Response(
            String id,
            String tenantId,
            String groupCode,
            String groupName,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(CodeGroup codeGroup) {
            return new Response(
                    codeGroup.getId(),
                    codeGroup.getTenantId(),
                    codeGroup.getGroupCode(),
                    codeGroup.getGroupName(),
                    codeGroup.getDescription(),
                    codeGroup.getUseYn(),
                    codeGroup.getDeletedYn()
            );
        }
    }
}
