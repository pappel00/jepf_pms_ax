package com.platform.module.pms.dto;

import com.platform.module.pms.domain.Transition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class TransitionDto {

    private TransitionDto() {
    }

    /**
     * 수행단계전환 요청. 이 요청 전체가 selection_json 에 저장되어 승인 시점에 그대로 적용된다
     * (WBS·문서 테일러링 기초데이터는 이 선택값으로 승인 시점에 생성한다).
     */
    @Schema(name = "PmsTransitionRequest")
    public record Request(
            @NotEmpty List<String> phases,
            @Size(max = 64) String rfpId,
            List<String> itemIds,
            Integer itemCount,
            @Schema(example = "auto") String assignMode,
            @Size(max = 500) String summary,
            @NotNull @Valid TeamDto.Spec team
    ) {
    }

    @Schema(name = "PmsTransitionDecisionRequest")
    public record DecisionRequest(@Size(max = 1000) String comment) {
    }

    @Schema(name = "PmsTransitionRollbackRequest")
    public record RollbackRequest(@NotBlank @Size(max = 1000) String reason) {
    }

    @Schema(name = "PmsTransitionResponse")
    public record Response(
            String id,
            String transitionCode,
            String projectId,
            String projectCode,
            String projectName,
            String kindCode,
            String statusCode,
            String requesterUserId,
            String requesterName,
            LocalDateTime requestedAt,
            String approverUserId,
            String approverName,
            LocalDateTime decidedAt,
            String decisionComment,
            String summary,
            String rfpId,
            Integer itemCount,
            Map<String, Object> selection,
            String rolledBackBy,
            String rolledBackByName,
            LocalDateTime rolledBackAt,
            String rollbackReason
    ) {
        public static Response of(Transition t, String projectCode, String projectName, Map<String, String> names,
                                  Map<String, Object> selection) {
            return new Response(t.getId(), t.getTransitionCode(), t.getProjectId(), projectCode, projectName, t.getKindCode(),
                    t.getStatusCode(), t.getRequesterUserId(), names.get(t.getRequesterUserId()), t.getRequestedAt(),
                    t.getApproverUserId(), t.getApproverUserId() == null ? null : names.get(t.getApproverUserId()), t.getDecidedAt(),
                    t.getDecisionComment(), t.getSummary(), t.getRfpId(), t.getItemCount(), selection, t.getRolledBackBy(),
                    t.getRolledBackBy() == null ? null : names.get(t.getRolledBackBy()), t.getRolledBackAt(), t.getRollbackReason());
        }
    }
}
