package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RfpDto {

    private RfpDto() {
    }

    @Schema(name = "PmsRfpRequest")
    public record Request(
            @NotBlank @Size(max = 300) String title,
            @NotBlank String roundCode,
            LocalDate receivedDate,
            LocalDate dueDate
    ) {
    }

    @Schema(name = "PmsRfpItemRequest")
    public record ItemRequest(
            @NotBlank String categoryCode,
            @Size(max = 100) String area,
            @NotBlank @Size(max = 300) String itemName,
            String content,
            Integer sourcePage,
            @NotNull Boolean mandatory,
            @Size(max = 30) @Schema(description = "원문 고유번호(문서 자동 분해 시). 비우면 분류-일련번호로 자동 채번") String itemCode
    ) {
    }

    @Schema(name = "PmsRfpItemBulkRequest")
    public record ItemBulkRequest(@NotEmpty @Valid List<ItemRequest> items) {
    }

    @Schema(name = "PmsRfpEvaluatorsRequest")
    public record EvaluatorsRequest(@NotNull List<String> userIds) {
    }

    @Schema(name = "PmsRfpEvalRequest")
    public record EvalRequest(
            String kindCode,
            String implCode,
            String difficultyCode,
            @DecimalMin("0") BigDecimal md,
            @Size(max = 1000) String comment
    ) {
    }

    /** 확정 M/D — evaluatorUserId 만 주면 그 평가자의 값으로, md 를 주면 직접 수정으로, 둘 다 없으면 확정 해제. */
    @Schema(name = "PmsRfpConfirmRequest")
    public record ConfirmRequest(String evaluatorUserId, @DecimalMin("0") BigDecimal md) {
    }

    @Schema(name = "PmsRfpConfirmAllRequest")
    public record ConfirmAllRequest(@NotBlank String evaluatorUserId) {
    }

    @Schema(name = "PmsRfpEvaluatorResponse")
    public record EvaluatorResponse(String userId, String userName, boolean submitted) {
    }

    @Schema(name = "PmsRfpItemResponse")
    public record ItemResponse(
            String id,
            String itemCode,
            String categoryCode,
            String area,
            String itemName,
            String content,
            Integer sourcePage,
            boolean mandatory,
            @Schema(description = "평가를 완료한 평가자 수(다른 평가자의 초안은 보이지 않으므로 서버가 센다)") int evalDone,
            String confirmedEvaluatorUserId,
            String confirmedEvaluatorName,
            BigDecimal confirmedMd,
            boolean confirmedEdited
    ) {
    }

    @Schema(name = "PmsRfpResponse")
    public record Response(
            String id,
            String rfpCode,
            String projectId,
            String projectCode,
            String title,
            String roundCode,
            LocalDate receivedDate,
            LocalDate dueDate,
            List<EvaluatorResponse> evaluators,
            List<ItemResponse> items
    ) {
    }

    /** 평가 한 건 — 본인 것은 항상, 다른 평가자 것은 제출된 경우에만 내려준다(블라인드). */
    @Schema(name = "PmsRfpEvalResponse")
    public record EvalResponse(
            String itemId,
            String evaluatorUserId,
            String evaluatorName,
            String kindCode,
            String implCode,
            String difficultyCode,
            BigDecimal md,
            String comment
    ) {
    }
}
