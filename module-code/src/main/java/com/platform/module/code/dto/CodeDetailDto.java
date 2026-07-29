package com.platform.module.code.dto;

import com.platform.module.code.domain.CodeDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CodeDetailDto {

    private CodeDetailDto() {
    }

    @Schema(name = "CodeDetailCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 50) @Schema(example = "PC") String code,
            @NotBlank @Size(max = 200) @Schema(example = "PC") String codeName,
            @Schema(example = "1", defaultValue = "0") Integer sortOrder,
            @Size(max = 500) @Schema(example = "데스크톱/노트북 환경") String description
    ) {
        /** sortOrder를 안 보내면(null) 0으로 취급한다 — Menu.CreateRequest와 동일한 이유. */
        public CreateRequest {
            if (sortOrder == null) {
                sortOrder = 0;
            }
        }
    }

    @Schema(name = "CodeDetailUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 200) @Schema(example = "PC") String codeName,
            @Schema(example = "1", defaultValue = "0") Integer sortOrder,
            @Size(max = 500) @Schema(example = "데스크톱/노트북 환경") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
        public UpdateRequest {
            if (sortOrder == null) {
                sortOrder = 0;
            }
        }
    }

    @Schema(name = "CodeDetailResponse")
    public record Response(
            String id,
            String tenantId,
            String groupId,
            String code,
            String codeName,
            int sortOrder,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(CodeDetail codeDetail) {
            return new Response(
                    codeDetail.getId(),
                    codeDetail.getTenantId(),
                    codeDetail.getGroupId(),
                    codeDetail.getCode(),
                    codeDetail.getCodeName(),
                    codeDetail.getSortOrder(),
                    codeDetail.getDescription(),
                    codeDetail.getUseYn(),
                    codeDetail.getDeletedYn()
            );
        }
    }
}
