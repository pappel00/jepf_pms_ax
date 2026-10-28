package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 영업 노트 · 차기 사업 기회 · 운영 업무/이슈/영업 DTO. 다른 행과의 참조(프로젝트·담당자·연결 항목)는 서버 PK 로 주고받는다. */
public final class OpsDto {

    private OpsDto() {
    }

    // ---------- 영업 노트 ----------

    @Schema(name = "PmsSalesNoteRequest")
    public record NoteRequest(
            @NotNull LocalDate noteDate,
            @NotBlank String noteTypeCode,
            String projectId,
            String customerId,
            String contactId,
            @NotBlank @Size(max = 300) String title,
            @NotBlank String content,
            @Size(max = 500) String nextAction,
            LocalDate nextDate,
            Boolean done
    ) {
    }

    @Schema(name = "PmsSalesNoteResponse")
    public record NoteResponse(
            String id, String noteCode, LocalDate noteDate, String noteTypeCode, String authorUserId, String authorName,
            String projectId, String projectCode, String customerId, String contactId, String title, String content,
            String nextAction, LocalDate nextDate, boolean done
    ) {
    }

    // ---------- 차기 사업 기회 ----------

    @Schema(name = "PmsNextOppRequest")
    public record NextRequest(
            @NotBlank @Size(max = 300) String title,
            @NotBlank String kindCode,
            @NotBlank String sourceCode,
            String content,
            String requesterContactId,
            String decisionMakerContactId,
            String budgetCode,
            BigDecimal estMm,
            Long amount,
            @Size(max = 10) String quarter,
            @NotBlank String stageCode,
            @NotBlank String priorityCode,
            @NotBlank String ownerUserId,
            String issueId,
            @Size(max = 500) String nextAction,
            LocalDate nextDate,
            @Size(max = 500) String scopeReason,
            String memo
    ) {
    }

    @Schema(name = "PmsNextOppResponse")
    public record NextResponse(
            String id, String oppCode, String projectId, String projectCode, String title, String kindCode, String sourceCode,
            String content, String requesterContactId, String decisionMakerContactId, String budgetCode, BigDecimal estMm,
            Long amount, String quarter, String stageCode, String priorityCode, String ownerUserId, String ownerName,
            String issueId, String nextAction, LocalDate nextDate, String scopeReason, String memo, String opsOpportunityId
    ) {
    }

    // ---------- 운영 업무 ----------

    @Schema(name = "PmsOpsTaskRequest")
    public record TaskRequest(
            @NotBlank @Size(max = 300) String title,
            @NotBlank String categoryCode,
            @NotBlank String assigneeUserId,
            @NotNull LocalDate dueDate,
            @NotBlank String statusCode,
            @NotBlank String priorityCode,
            String repeatCode,
            LocalDate repeatEndDate,
            String memo,
            String incidentId
    ) {
    }

    @Schema(name = "PmsOpsTaskStatusRequest")
    public record TaskStatusRequest(@NotBlank String statusCode) {
    }

    @Schema(name = "PmsOpsTaskResponse")
    public record TaskResponse(
            String id, String taskCode, String projectId, String projectCode, String title, String categoryCode,
            String assigneeUserId, String assigneeName, LocalDate dueDate, String statusCode, String priorityCode,
            String repeatCode, LocalDate repeatEndDate, String prevTaskId, boolean autoCreated, String memo, String incidentId,
            @Schema(description = "이 요청으로 자동 생성한 다음 반복 업무 코드") String spawnedTaskCode,
            @Schema(description = "반복 업무를 완료했지만 자동 생성하지 않은 사유") String skippedReason
    ) {
    }

    // ---------- 운영 이슈 ----------

    @Schema(name = "PmsOpsIncidentRequest")
    public record IncidentRequest(
            @NotBlank String typeCode,
            @NotBlank @Size(max = 300) String title,
            String content,
            @NotBlank String priorityCode,
            @NotBlank String statusCode,
            @NotBlank @Size(max = 100) String reporterName,
            String assigneeUserId,
            @NotNull LocalDateTime receivedAt,
            LocalDateTime respondedAt,
            LocalDateTime resolvedAt,
            String actionText
    ) {
    }

    @Schema(name = "PmsOpsIncidentLinkRequest")
    public record IncidentLinkRequest(String opportunityId) {
    }

    @Schema(name = "PmsOpsIncidentResponse")
    public record IncidentResponse(
            String id, String incidentCode, String projectId, String projectCode, String typeCode, String title, String content,
            String priorityCode, String statusCode, String reporterName, String assigneeUserId, String assigneeName,
            LocalDateTime receivedAt, LocalDateTime respondedAt, LocalDateTime resolvedAt, String actionText, String opportunityId
    ) {
    }

    // ---------- 운영 영업 ----------

    @Schema(name = "PmsOpsOppRequest")
    public record OppRequest(
            @NotBlank String kindCode,
            @NotBlank @Size(max = 300) String title,
            @NotBlank String stageCode,
            Long amount,
            BigDecimal probabilityPct,
            @NotBlank String ownerUserId,
            LocalDate expectedDate,
            @Size(max = 500) String nextAction,
            LocalDate nextDate,
            String memo,
            String sourceIncidentId,
            String contactId,
            String sourceNextOppId
    ) {
    }

    @Schema(name = "PmsOpsOppResponse")
    public record OppResponse(
            String id, String oppCode, String projectId, String projectCode, String kindCode, String title, String stageCode,
            Long amount, BigDecimal probabilityPct, String ownerUserId, String ownerName, LocalDate expectedDate, String nextAction,
            LocalDate nextDate, String memo, String sourceIncidentId, String contactId, String sourceNextOppId
    ) {
    }
}
