package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReportDto {

    private ReportDto() {
    }

    @Schema(name = "PmsReportDraftRequest")
    public record DraftRequest(String memo) {
    }

    /** 제출 — 제출 시점의 전체 진척·목표(화면이 WBS로 계산한 값)와 특이사항을 버전으로 남긴다. */
    @Schema(name = "PmsReportSubmitRequest")
    public record SubmitRequest(
            String memo,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal overallPct,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal targetPct
    ) {
    }

    @Schema(name = "PmsReportReopenRequest")
    public record ReopenRequest(@NotBlank @Size(max = 500) String reason) {
    }

    @Schema(name = "PmsReportHistoryResponse")
    public record HistoryEntry(String actionCode, String actorName, LocalDateTime actedAt, String text) {
    }

    @Schema(name = "PmsReportSubmittedResponse")
    public record Submitted(LocalDateTime submittedAt, String authorName, BigDecimal overallPct, BigDecimal targetPct, String memo) {
    }

    @Schema(name = "PmsReportResponse")
    public record Response(
            String id,
            String projectId,
            String projectCode,
            String reportTypeCode,
            LocalDate periodStart,
            LocalDate periodEnd,
            String statusCode,
            String memo,
            Integer submitVersion,
            Submitted submitted,
            List<HistoryEntry> history
    ) {
    }

    @Schema(name = "PmsSnapshotRequest")
    public record SnapshotRequest(
            @NotNull LocalDate snapshotDate,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal overallPct,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal targetPct
    ) {
    }

    @Schema(name = "PmsSnapshotResponse")
    public record SnapshotResponse(String id, String projectId, String projectCode, LocalDate snapshotDate,
                                   BigDecimal overallPct, BigDecimal targetPct) {
    }
}
