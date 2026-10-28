package com.platform.module.pms.dto;

import com.platform.module.pms.domain.Project;
import com.platform.module.pms.domain.StatusHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public final class ProjectDto {

    private ProjectDto() {
    }

    /**
     * 등록/수정 공통 요청 (프로젝트 코드는 서버가 채번한다).
     * 조건부 필수(상태에 따른 담당자·제안 마감일·실패 사유)는 서비스에서 검증한다.
     */
    @Schema(name = "PmsProjectRequest")
    public record Request(
            @NotBlank @Size(max = 300) String projectName,
            @NotBlank @Size(max = 30) @Schema(example = "공고 전") String statusCode,
            @NotNull BigDecimal probabilityPct,
            @NotBlank String customerId,
            String contactId,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull Long amount,
            String vatIncludedYn,
            @NotBlank String salesUserId,
            String pmUserId,
            @Size(max = 30) String businessTypeCode,
            @Size(max = 30) String bidTypeCode,
            @Size(max = 50) String noticeNo,
            LocalDate dueDate,
            LocalDate presentDate,
            @Size(max = 500) String competitors,
            Long costEstimate,
            @Size(max = 20) String costSourceType,
            @Size(max = 300) String costSourceNote,
            @Size(max = 50) String failReasonCode,
            @Size(max = 1000) String failReasonDetail,
            String viewRestrictedYn,
            @Size(max = 1000) String memo
    ) {
    }

    /** 상태만 바꾸는 요청 (단계전환·수행 종료 등 다른 화면의 상태 변경에서 사용). */
    @Schema(name = "PmsProjectStatusRequest")
    public record StatusRequest(
            @NotBlank @Size(max = 30) String statusCode,
            @Size(max = 1000) String memo
    ) {
    }

    @Schema(name = "PmsProjectResponse")
    public record Response(
            String id,
            String projectCode,
            String projectName,
            String statusCode,
            BigDecimal probabilityPct,
            String customerId,
            String clientName,
            String contactId,
            String contactName,
            String contactTitle,
            String contactPhone,
            String contactEmail,
            LocalDate startDate,
            LocalDate endDate,
            Long amount,
            String vatIncludedYn,
            String salesUserId,
            String salesUserName,
            String pmUserId,
            String pmUserName,
            String businessTypeCode,
            String bidTypeCode,
            String noticeNo,
            LocalDate dueDate,
            LocalDate presentDate,
            String competitors,
            Long costEstimate,
            String costSourceType,
            String costSourceNote,
            String failReasonCode,
            String failReasonDetail,
            LocalDate execStartDate,
            LocalDate execEndDate,
            String execEndMemo,
            String viewRestrictedYn,
            String memo
    ) {
        /** showCost 가 false 면 예상 원가 관련 값을 가린다(PMS_COST_VIEW 권한이 없는 사용자). */
        public static Response from(Project p, Map<String, String> userNames, boolean showCost) {
            return new Response(p.getId(), p.getProjectCode(), p.getProjectName(), p.getStatusCode(), p.getProbabilityPct(),
                    p.getCustomerId(), p.getClientName(), p.getContactId(), p.getContactName(), p.getContactTitle(),
                    p.getContactPhone(), p.getContactEmail(), p.getStartDate(), p.getEndDate(), p.getAmount(),
                    p.getVatIncludedYn(), p.getSalesUserId(), userNames.get(p.getSalesUserId()), p.getPmUserId(),
                    p.getPmUserId() == null ? null : userNames.get(p.getPmUserId()), p.getBusinessTypeCode(),
                    p.getBidTypeCode(), p.getNoticeNo(), p.getDueDate(), p.getPresentDate(), p.getCompetitors(),
                    showCost ? p.getCostEstimate() : null, showCost ? p.getCostSourceType() : null, showCost ? p.getCostSourceNote() : null,
                    p.getFailReasonCode(),
                    p.getFailReasonDetail(), p.getExecStartDate(), p.getExecEndDate(), p.getExecEndMemo(),
                    p.getViewRestrictedYn(), p.getMemo());
        }
    }

    @Schema(name = "PmsStatusHistoryResponse")
    public record HistoryResponse(
            String id,
            String fromStatus,
            String toStatus,
            String actorUserId,
            String actorUserName,
            LocalDateTime actedAt,
            String memo
    ) {
        public static HistoryResponse from(StatusHistory h, Map<String, String> userNames) {
            return new HistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getActorUserId(),
                    userNames.get(h.getActorUserId()), h.getActedAt(), h.getMemo());
        }
    }
}
