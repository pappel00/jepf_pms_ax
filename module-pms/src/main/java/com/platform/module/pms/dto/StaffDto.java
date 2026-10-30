package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 인력 관리 — 계정 + PMS 프로필 + PMS 사용자그룹(역할)을 한 번에 다룬다. */
public final class StaffDto {

    private StaffDto() {
    }

    @Schema(name = "PmsStaffCreateRequest")
    public record CreateRequest(
            @NotBlank @Size(max = 100) String loginId,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 200) String email,
            @NotBlank @Size(max = 100) String userName,
            @NotBlank @Size(max = 20) @Schema(example = "자사") String orgTypeCode,
            @Size(max = 200) String partnerCompany,
            @Size(max = 20) String notifyChannelCode,
            @Size(max = 100) String telegramChatId,
            @Size(max = 100) String mattermostUserId,
            @Size(max = 20) String jobGradeCode,
            @Schema(example = "[\"PMS_PM\"]") List<String> groupCodes
    ) {
    }

    @Schema(name = "PmsStaffUpdateRequest")
    public record UpdateRequest(
            @Size(max = 200) String email,
            @NotBlank @Size(max = 100) String userName,
            @NotBlank @Schema(example = "Y") String useYn,
            @NotBlank @Size(max = 20) String orgTypeCode,
            @Size(max = 200) String partnerCompany,
            @Size(max = 20) String notifyChannelCode,
            @Size(max = 100) String telegramChatId,
            @Size(max = 100) String mattermostUserId,
            @Size(max = 20) String jobGradeCode,
            List<String> groupCodes
    ) {
    }

    @Schema(name = "PmsStaffResponse")
    public record Response(
            String userId,
            String loginId,
            String userName,
            String email,
            String useYn,
            String orgTypeCode,
            String partnerCompany,
            String notifyChannelCode,
            String telegramChatId,
            String mattermostUserId,
            String jobGradeCode,
            List<String> groupCodes
    ) {
    }
}
