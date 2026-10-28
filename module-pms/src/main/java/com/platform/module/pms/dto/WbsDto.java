package com.platform.module.pms.dto;

import com.platform.module.pms.domain.WbsTask;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class WbsDto {

    private WbsDto() {
    }

    /** 일괄 생성용 노드 — key/parentKey/predKey 는 요청 안에서만 쓰는 임시 키(서버가 ID 로 바꿔 저장). */
    @Schema(name = "PmsWbsNode")
    public record Node(
            @NotBlank String key,
            String parentKey,
            String predKey,
            @NotBlank String nodeTypeCode,
            @NotBlank String phaseCode,
            String categoryName,
            @NotBlank @Size(max = 300) String taskName,
            String rfpItemId,
            String reqCode,
            String featureName,
            String assigneeUserId,
            LocalDate startDate,
            LocalDate endDate,
            @NotNull BigDecimal weight,
            BigDecimal progressPct
    ) {
    }

    @Schema(name = "PmsWbsBulkRequest")
    public record BulkRequest(@NotEmpty @Valid List<Node> tasks) {
    }

    @Schema(name = "PmsWbsProgressRequest")
    public record ProgressRequest(
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal progressPct,
            @Size(max = 500) String note
    ) {
    }

    @Schema(name = "PmsWbsCopyRequest")
    public record CopyRequest(String assigneeUserId) {
    }

    @Schema(name = "PmsWbsTaskResponse")
    public record Response(
            String id,
            String projectId,
            String projectCode,
            String parentId,
            String nodeTypeCode,
            String phaseCode,
            String categoryName,
            String taskName,
            String rfpItemId,
            String reqCode,
            String featureName,
            String assigneeUserId,
            String assigneeName,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal weight,
            BigDecimal progressPct,
            String predTaskId,
            String copiedFromId,
            Integer sortOrder
    ) {
        public static Response of(WbsTask t, String projectCode, Map<String, String> names) {
            return new Response(t.getId(), t.getProjectId(), projectCode, t.getParentId(), t.getNodeTypeCode(), t.getPhaseCode(),
                    t.getCategoryName(), t.getTaskName(), t.getRfpItemId(), t.getReqCode(), t.getFeatureName(), t.getAssigneeUserId(),
                    t.getAssigneeUserId() == null ? null : names.get(t.getAssigneeUserId()), t.getStartDate(), t.getEndDate(),
                    t.getWeight(), t.getProgressPct(), t.getPredTaskId(), t.getCopiedFromId(), t.getSortOrder());
        }
    }
}
