package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class DocDto {

    private DocDto() {
    }

    /** 단계전환 승인 직후 문서 테일러링 생성 — 선택한 단계의 표준 산출물을 복사한다. */
    @Schema(name = "PmsDocInitRequest")
    public record InitRequest(
            @NotEmpty List<String> phases,
            @Schema(description = "단계 → 담당자 사용자 ID") Map<String, String> owners,
            @Schema(description = "선택한 RFP 항목의 분류 코드(권장 문서 자동 적용 기준)") List<String> categories
    ) {
    }

    /** null 인 필드는 바꾸지 않는다. 미적용으로 바꿀 때는 사유가 필요하다(서버 검증). */
    @Schema(name = "PmsDocPatchRequest")
    public record PatchRequest(String appliedCode, @Size(max = 500) String notAppliedReason, String ownerUserId, @Size(max = 500) String note) {
    }

    @Schema(name = "PmsDocCustomRequest")
    public record CustomRequest(
            @NotBlank String phaseCode,
            @NotBlank @Size(max = 200) String docName,
            @NotNull Boolean required,
            String ownerUserId
    ) {
    }

    @Schema(name = "PmsDocVersionRequest")
    public record VersionRequest(@Size(max = 300) String fileName, @Size(max = 30) String fileSize, @Size(max = 500) String note) {
    }

    @Schema(name = "PmsDocReviewedRequest")
    public record ReviewedRequest(@NotNull Boolean reviewed) {
    }

    @Schema(name = "PmsDocVersionResponse")
    public record VersionResponse(String id, String ver, LocalDate versionDate, String authorName, String fileName, String fileSize, String note) {
    }

    @Schema(name = "PmsDocTailoringResponse")
    public record Response(
            String id,
            String projectId,
            String projectCode,
            String phaseCode,
            String docName,
            boolean required,
            String appliedCode,
            String notAppliedReason,
            String ownerUserId,
            String ownerName,
            boolean custom,
            String note,
            boolean reviewed,
            List<VersionResponse> versions
    ) {
    }
}
