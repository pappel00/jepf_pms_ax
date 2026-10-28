package com.platform.module.pms.dto;

import com.platform.module.pms.domain.Issue;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class IssueDto {

    private IssueDto() {
    }

    /** 등록/수정 공통. taskIds 가 null 이면 연결 업무를 건드리지 않고, 값이 있으면 그 목록으로 맞춘다(이슈 1 : 업무 N). */
    @Schema(name = "PmsIssueRequest")
    public record Request(
            @NotBlank @Size(max = 300) String title,
            @NotBlank String typeCode,
            @NotBlank String priorityCode,
            String statusCode,
            String reporterUserId,
            @NotBlank String assigneeUserId,
            LocalDate registeredDate,
            LocalDate dueDate,
            @NotNull @Min(0) Integer impactDays,
            @NotBlank String content,
            String resolution,
            List<String> taskIds
    ) {
    }

    @Schema(name = "PmsIssueNoteRequest")
    public record NoteRequest(@NotBlank @Size(max = 1000) String text) {
    }

    @Schema(name = "PmsIssueHistoryResponse")
    public record HistoryEntry(String actionCode, String actorUserId, String actorName, LocalDateTime actedAt, String text) {
    }

    @Schema(name = "PmsIssueResponse")
    public record Response(
            String id,
            String issueCode,
            String projectId,
            String projectCode,
            String title,
            String typeCode,
            String priorityCode,
            String statusCode,
            String reporterUserId,
            String reporterName,
            String assigneeUserId,
            String assigneeName,
            LocalDate registeredDate,
            LocalDate dueDate,
            LocalDate resolvedDate,
            Integer impactDays,
            String content,
            String resolution,
            List<String> taskIds,
            List<HistoryEntry> history,
            @Schema(description = "이 요청으로 새로 만든 일정 조정 제안 코드(없으면 null)") String newProposalCode
    ) {
        public static Response of(Issue i, String projectCode, Map<String, String> names, List<String> taskIds,
                                  List<HistoryEntry> history, String newProposalCode) {
            return new Response(i.getId(), i.getIssueCode(), i.getProjectId(), projectCode, i.getTitle(), i.getTypeCode(),
                    i.getPriorityCode(), i.getStatusCode(), i.getReporterUserId(), names.get(i.getReporterUserId()),
                    i.getAssigneeUserId(), names.get(i.getAssigneeUserId()), i.getRegisteredDate(), i.getDueDate(),
                    i.getResolvedDate(), i.getImpactDays(), i.getContent(), i.getResolution(), taskIds, history, newProposalCode);
        }
    }
}
