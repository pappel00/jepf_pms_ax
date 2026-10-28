package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public final class CostDto {

    private CostDto() {
    }

    @Schema(name = "PmsRateRequest")
    public record RateRequest(
            @NotBlank String jobCode,
            @NotBlank String gradeCode,
            @NotNull @Min(0) Long unitPrice,
            @NotNull LocalDate fromDate,
            LocalDate toDate
    ) {
    }

    @Schema(name = "PmsRateResponse")
    public record RateResponse(String id, String jobCode, String gradeCode, Long unitPrice, LocalDate fromDate, LocalDate toDate) {
    }

    /** 월별 실적 M/M 입력 — actualMm 이 null 이면 그 달 실적을 지운다. */
    @Schema(name = "PmsActualMmRequest")
    public record ActualRequest(
            @NotBlank String userId,
            @NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String month,
            @DecimalMin("0") @DecimalMax("99") BigDecimal actualMm
    ) {
    }

    @Schema(name = "PmsActualMmResponse")
    public record ActualResponse(String projectId, String projectCode, String kindCode, String userId, String userName,
                                 String month, BigDecimal actualMm) {
    }

    @Schema(name = "PmsSettingsRequest")
    public record SettingsRequest(@NotNull Map<String, String> values) {
    }
}
