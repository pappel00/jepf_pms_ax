package com.platform.module.form.dto;

import com.platform.module.form.domain.FormVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public final class FormVersionDto {

    private FormVersionDto() {
    }

    /** filePath를 직접 입력하는 메타데이터 전용 등록 — 실제 파일 없이 경로만 아는 경우용. */
    @Schema(name = "FormVersionCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "1.0") String versionNo,
            @NotBlank @Size(max = 300) @Schema(example = "휴가신청서_v1.0.xlsx") String fileName,
            @NotBlank @Size(max = 1000) @Schema(example = "/forms/leave/v1.xlsx") String filePath,
            @Schema(example = "20480") Long fileSize,
            @NotNull @Schema(example = "2026-01-01") LocalDate effectiveDate,
            @Size(max = 500) @Schema(example = "최초 시행") String description
    ) {
    }

    /** 파일은 그대로 두고 메타데이터(버전번호/시행일자/비고/사용여부)만 수정한다. */
    @Schema(name = "FormVersionUpdateRequest")
    public record UpdateRequest(
            @NotBlank @Size(max = 30) @Schema(example = "1.1") String versionNo,
            @NotNull @Schema(example = "2026-03-01") LocalDate effectiveDate,
            @Size(max = 500) @Schema(example = "항목 추가") String description,
            @NotBlank @Schema(example = "Y") String useYn
    ) {
    }

    @Schema(name = "FormVersionResponse")
    public record Response(
            String id,
            String tenantId,
            String groupId,
            String versionNo,
            String fileName,
            String filePath,
            Long fileSize,
            LocalDate effectiveDate,
            String description,
            String useYn,
            String deletedYn
    ) {
        public static Response from(FormVersion version) {
            return new Response(
                    version.getId(),
                    version.getTenantId(),
                    version.getGroupId(),
                    version.getVersionNo(),
                    version.getFileName(),
                    version.getFilePath(),
                    version.getFileSize(),
                    version.getEffectiveDate(),
                    version.getDescription(),
                    version.getUseYn(),
                    version.getDeletedYn()
            );
        }
    }
}
