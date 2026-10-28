package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 프로젝트 관리자·투입 인력(팀) 입력/응답 형태 — 단계전환 요청, 투입 계획 변경 요청, 팀 조회가 같이 쓴다. */
public final class TeamDto {

    private TeamDto() {
    }

    /** 투입이 중간에 비는 공백 구간. */
    public record Gap(LocalDate fromDate, LocalDate toDate) {
    }

    @Schema(name = "PmsTeamMemberSpec")
    public record Member(
            @NotBlank String userId,
            @NotBlank String roleCode,
            List<String> phases,
            BigDecimal mm,
            BigDecimal ratePct,
            LocalDate fromDate,
            LocalDate toDate,
            Boolean autoEnd,
            @Schema(example = "편집") String accessLevel,
            String notifyChannel,
            List<Gap> gaps,
            @Schema(description = "월별 조정 { 'YYYY-MM': 투입률(%) } — 직접 수정한 달만") Map<String, BigDecimal> monthPlan
    ) {
    }

    @Schema(name = "PmsTeamManagerSpec")
    public record Manager(@NotBlank String userId, @NotBlank @Schema(example = "PM") String roleCode) {
    }

    /** 팀 전체(관리자 + 투입 인력 + 멤버 외 열람 제한). */
    @Schema(name = "PmsTeamSpec")
    public record Spec(@NotNull @Valid Manager manager, @NotNull @Valid List<Member> team, Boolean restricted) {
    }

    // ---------- 응답 ----------

    public record MemberResponse(
            String userId,
            String userName,
            String orgTypeCode,
            String partnerCompany,
            String roleCode,
            List<String> phases,
            BigDecimal mm,
            BigDecimal ratePct,
            LocalDate fromDate,
            LocalDate toDate,
            boolean autoEnd,
            String accessLevel,
            String notifyChannel,
            List<Gap> gaps,
            Map<String, BigDecimal> monthPlan
    ) {
    }

    public record ManagerResponse(String userId, String userName, String roleCode) {
    }

    @Schema(name = "PmsTeamResponse")
    public record TeamResponse(
            String projectId,
            String projectCode,
            ManagerResponse manager,
            List<MemberResponse> team,
            boolean restricted
    ) {
    }
}
