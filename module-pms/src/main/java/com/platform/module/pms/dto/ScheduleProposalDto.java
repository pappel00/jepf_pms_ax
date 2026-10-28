package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ScheduleProposalDto {

    private ScheduleProposalDto() {
    }

    @Schema(name = "PmsScheduleProposalDecisionRequest")
    public record DecisionRequest(@Size(max = 500) String comment) {
    }

    @Schema(name = "PmsScheduleProposalItemResponse")
    public record ItemResponse(
            String taskId,
            String taskName,
            @Schema(description = "지연 업무(이슈 연결 업무) / 후행 업무") String role,
            LocalDate oldStart,
            LocalDate oldEnd,
            LocalDate newStart,
            LocalDate newEnd,
            boolean applied
    ) {
    }

    @Schema(name = "PmsScheduleProposalResponse")
    public record Response(
            String id,
            String proposalCode,
            String projectId,
            String projectCode,
            String issueId,
            String issueCode,
            String issueTitle,
            String statusCode,
            Integer shiftDays,
            String basisText,
            LocalDateTime createdAt,
            String decidedBy,
            String decidedByName,
            LocalDateTime decidedAt,
            String decisionComment,
            List<ItemResponse> items
    ) {
    }
}
