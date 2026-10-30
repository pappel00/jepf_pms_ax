package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public final class NotifyDto {

    private NotifyDto() {
    }

    /** Mattermost 연결 상태. */
    @Schema(name = "PmsNotifyStatus")
    public record Status(
            String baseUrl,
            boolean serverReachable,
            String serverVersion,
            boolean tokenConfigured,
            boolean botReady,
            String botName,
            String message
    ) {
    }

    /** 사용자 ↔ Mattermost 매핑 확인 결과. */
    @Schema(name = "PmsNotifyResolve")
    public record Resolve(
            String userId,
            String channelCode,
            boolean found,
            String mattermostUserId,
            String mattermostUsername,
            /** 찾은 방법: 이메일 / 메신저 ID */
            String via,
            String message
    ) {
    }

    @Schema(name = "PmsNotifyTestRequest")
    public record TestRequest(@NotBlank String userId) {
    }

    @Schema(name = "PmsNotifyResult")
    public record Result(String statusCode, String message) {
    }

    @Schema(name = "PmsNotifyLog")
    public record Log(
            String id,
            String userId,
            String userName,
            String channelCode,
            String kindCode,
            String message,
            String statusCode,
            String errorMessage,
            LocalDateTime createdDate
    ) {
    }
}
