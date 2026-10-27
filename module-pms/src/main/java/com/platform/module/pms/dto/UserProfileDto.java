package com.platform.module.pms.dto;

import com.platform.module.pms.domain.UserProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class UserProfileDto {

    private UserProfileDto() {
    }

    @Schema(name = "PmsUserProfileRequest")
    public record Request(
            @NotBlank @Size(max = 20) @Schema(example = "자사") String orgTypeCode,
            @Size(max = 200) String partnerCompany,
            @Size(max = 20) String notifyChannelCode,
            @Size(max = 100) String telegramChatId,
            @Size(max = 100) String mattermostUserId,
            @Size(max = 20) @Schema(example = "고급") String jobGradeCode
    ) {
    }

    @Schema(name = "PmsUserProfileResponse")
    public record Response(
            String userId,
            String loginId,
            String userName,
            String orgTypeCode,
            String partnerCompany,
            String notifyChannelCode,
            String telegramChatId,
            String mattermostUserId,
            String jobGradeCode
    ) {
        public static Response of(String userId, String loginId, String userName, UserProfile p) {
            if (p == null) {
                return new Response(userId, loginId, userName, "자사", null, null, null, null, null);
            }
            return new Response(userId, loginId, userName, p.getOrgTypeCode(), p.getPartnerCompany(),
                    p.getNotifyChannelCode(), p.getTelegramChatId(), p.getMattermostUserId(), p.getJobGradeCode());
        }
    }
}
