package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/** 수행 종료 · 운영단계전환 · 운영 정보. */
public final class OperationDto {

    private OperationDto() {
    }

    @Schema(name = "PmsEndExecutionRequest")
    public record EndRequest(
            @NotNull LocalDate date,
            @Size(max = 1000) String memo,
            @Schema(description = "운영 인수 대상 후보가 되는 기능 목록") List<String> features
    ) {
    }

    @Schema(name = "PmsDocHandover")
    public record DocRef(String refId, @NotBlank String phase, @NotBlank String name, String version) {
    }

    @Schema(name = "PmsIssueHandover")
    public record IssueRef(String refId, @NotBlank String title, String priority) {
    }

    /** 운영 정보(형태·기간·계약 금액·SLA)와 운영 팀. 전환 시에는 인수 대상(기능·산출물·이슈·차기 기회)도 함께 보낸다. */
    @Schema(name = "PmsOperationRequest")
    public record Request(
            @NotBlank String opTypeCode,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            Long contractAmount,
            @NotBlank String slaLevelCode,
            Integer slaRespondHours,
            Integer slaResolveHours,
            @Size(max = 100) String slaServiceHours,
            @NotNull @Valid TeamDto.Spec team,
            List<String> features,
            List<DocRef> docs,
            List<IssueRef> issues,
            List<String> nextOppIds
    ) {
    }

    @Schema(name = "PmsHandoverResponse")
    public record HandoverResponse(String typeCode, String refId, String itemName) {
    }

    @Schema(name = "PmsOperationResponse")
    public record Response(
            String projectId,
            String projectCode,
            LocalDate transitionDate,
            String opTypeCode,
            LocalDate startDate,
            LocalDate endDate,
            Long contractAmount,
            String slaLevelCode,
            Integer slaRespondHours,
            Integer slaResolveHours,
            String slaServiceHours,
            List<HandoverResponse> handover
    ) {
    }

    @Schema(name = "PmsProjectFeatureResponse")
    public record FeatureResponse(String projectId, String projectCode, String featureName, boolean handover) {
    }
}
