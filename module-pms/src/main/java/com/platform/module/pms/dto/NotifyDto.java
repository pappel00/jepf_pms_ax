package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

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

    /** 업무 알림 설정 — 켜기/끄기, 발송 시각(시), 마감 며칠 전에 알릴지, 시작일 알림 여부. */
    @Schema(name = "PmsNotifySettings")
    public record Settings(boolean enabled, int hour, List<Integer> dueDays, boolean startAlert, java.util.Map<String, Boolean> types) {
    }

    /** 알림 종류 안내(설정 표의 한 행). scope: DAILY 매일 정한 시각 / SLA 매시. */
    @Schema(name = "PmsNotifyTypeInfo")
    public record TypeInfo(String key, String folder, String page, String item, String description, boolean defaultOn, String scope) {
    }

    /** 알림 작업 실행 결과(dryRun 이면 보내지 않고 대상만 센다). */
    @Schema(name = "PmsNotifyRunResult")
    public record RunResult(boolean dryRun, int targets, int sent, int skipped, int failed, int duplicates, java.util.Map<String, Integer> byType, List<String> samples) {
    }

    @Schema(name = "PmsNotifyTestRequest")
    public record TestRequest(@NotBlank String userId) {
    }

    /** 텔레그램 봇 연결 상태. polling 은 새 메시지·버튼 클릭을 가져오는 중인지. */
    @Schema(name = "PmsNotifyTelegramStatus")
    public record TelegramStatus(boolean tokenConfigured, boolean botReady, String botUsername, boolean polling, String message) {
    }

    /** 사용자 연결용 코드 — 사용자가 봇에게 /start 코드 를 보내거나 link 를 열면 chat ID 가 저장된다. */
    @Schema(name = "PmsNotifyTelegramLinkCode")
    public record LinkCode(String code, String link, int expiresInMinutes) {
    }

    /** 이메일(SMTP) 연결 상태. */
    @Schema(name = "PmsNotifyEmailStatus")
    public record EmailStatus(boolean configured, boolean connected, String host, String from, String message) {
    }

    @Schema(name = "PmsNotifyProgressPromptRequest")
    public record ProgressPromptRequest(@NotBlank String taskId) {
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
