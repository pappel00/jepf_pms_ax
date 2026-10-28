package com.platform.module.pms.dto;

import com.platform.module.pms.domain.TeamChange;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;

public final class TeamChangeDto {

    private TeamChangeDto() {
    }

    /**
     * 투입 계획 변경 요청. before 는 서버가 현재 팀에서 만든다. 변경 요약 문장과 과투입 예상은 화면이 계산해서 보낸다
     * (과투입 계산은 다른 프로젝트 투입까지 보는 화면 로직이라 이후 단계에서 서버로 옮긴다).
     */
    @Schema(name = "PmsTeamChangeRequest")
    public record Request(
            @NotBlank @Size(max = 1000) String reason,
            @NotNull @Valid TeamDto.Spec after,
            String summaryText,
            @Schema(description = "승인 시 과투입 예상(JSON 문자열)") String conflictJson
    ) {
    }

    @Schema(name = "PmsTeamChangeDecisionRequest")
    public record DecisionRequest(@Size(max = 1000) String comment) {
    }

    @Schema(name = "PmsTeamChangeResponse")
    public record Response(
            String id,
            String changeCode,
            String projectId,
            String projectCode,
            String projectName,
            String statusCode,
            String requesterUserId,
            String requesterName,
            LocalDateTime requestedAt,
            String reason,
            Map<String, Object> before,
            Map<String, Object> after,
            String summaryText,
            String conflictJson,
            String decidedBy,
            String decidedByName,
            LocalDateTime decidedAt,
            String decisionComment
    ) {
        public static Response of(TeamChange c, String projectCode, String projectName, Map<String, String> names,
                                  Map<String, Object> before, Map<String, Object> after) {
            return new Response(c.getId(), c.getChangeCode(), c.getProjectId(), projectCode, projectName, c.getStatusCode(),
                    c.getRequesterUserId(), names.get(c.getRequesterUserId()), c.getRequestedAt(), c.getReason(), before, after,
                    c.getSummaryText(), c.getConflictJson(), c.getDecidedBy(),
                    c.getDecidedBy() == null ? null : names.get(c.getDecidedBy()), c.getDecidedAt(), c.getDecisionComment());
        }
    }
}
