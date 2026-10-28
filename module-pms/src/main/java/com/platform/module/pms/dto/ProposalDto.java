package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public final class ProposalDto {

    private ProposalDto() {
    }

    @Schema(name = "PmsProposalRequest")
    public record Request(
            @NotBlank String rfpId,
            @NotBlank @Size(max = 300) String title,
            @NotBlank String statusCode,
            @NotBlank String ownerUserId,
            LocalDate dueDate,
            LocalDate submittedDate,
            Long price,
            Long costEstimate,
            @Schema(description = "등록 시에만 사용 — 처음 만들 제출 체크리스트 항목명") List<String> checklist
    ) {
    }

    @Schema(name = "PmsProposalVersionRequest")
    public record VersionRequest(
            @NotBlank @Size(max = 20) String ver,
            LocalDate versionDate,
            @Size(max = 1000) String summary,
            @Size(max = 300) String fileName,
            @Size(max = 30) String fileSize
    ) {
    }

    /** 항목 대응 — null 인 필드는 바꾸지 않는다. */
    @Schema(name = "PmsProposalMappingRequest")
    public record MappingRequest(String section, String responseCode, String summary, String ownerUserId) {
    }

    @Schema(name = "PmsProposalCheckRequest")
    public record CheckRequest(@NotBlank @Size(max = 200) String itemName) {
    }

    @Schema(name = "PmsProposalVersionResponse")
    public record VersionResponse(String id, String ver, LocalDate versionDate, String authorUserId, String authorName,
                                  String summary, String fileName, String fileSize, boolean submitted) {
    }

    @Schema(name = "PmsProposalMappingResponse")
    public record MappingResponse(String rfpItemId, String section, String responseCode, String summary, String ownerUserId,
                                  String ownerName) {
    }

    @Schema(name = "PmsProposalCheckResponse")
    public record CheckResponse(String id, String itemName, boolean done) {
    }

    @Schema(name = "PmsProposalResponse")
    public record Response(
            String id,
            String proposalCode,
            String projectId,
            String projectCode,
            String rfpId,
            String rfpCode,
            String title,
            String statusCode,
            String ownerUserId,
            String ownerName,
            LocalDate dueDate,
            LocalDate submittedDate,
            Long price,
            Long costEstimate,
            @NotNull List<VersionResponse> versions,
            List<MappingResponse> mappings,
            List<CheckResponse> checklist
    ) {
    }
}
