package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.NotifyDto;
import com.platform.module.pms.service.NotifyJobService;
import com.platform.module.pms.service.NotifyService;
import com.platform.module.pms.service.NotifyType;
import com.platform.module.pms.service.TelegramLinkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 메신저 알림 API — 연결 상태, 사용자 매핑 확인, 테스트 발송, 발송 이력. PM·센터장·관리자만 쓴다. */
@RestController
@RequestMapping("/api/v1/pms/notify")
@Tag(name = "PmsNotify", description = "PMS 메신저 알림(Mattermost) (테넌트 자동 격리)")
@PreAuthorize("hasAnyRole('PMS_PM','PMS_MANAGER','ADMIN','SYSTEM_ADMIN')")
public class NotifyController {

    private final NotifyService notifyService;
    private final NotifyJobService jobService;
    private final TelegramLinkService telegramLinkService;

    public NotifyController(NotifyService notifyService, NotifyJobService jobService, TelegramLinkService telegramLinkService) {
        this.notifyService = notifyService;
        this.jobService = jobService;
        this.telegramLinkService = telegramLinkService;
    }

    @GetMapping("/status")
    public ApiResponse<NotifyDto.Status> status() {
        return ApiResponse.success(notifyService.status());
    }

    /** 이메일(SMTP) 연결 상태. */
    @GetMapping("/email/status")
    public ApiResponse<NotifyDto.EmailStatus> emailStatus() {
        return ApiResponse.success(notifyService.emailStatus());
    }

    /** 텔레그램 봇 연결 상태. */
    @GetMapping("/telegram/status")
    public ApiResponse<NotifyDto.TelegramStatus> telegramStatus() {
        return ApiResponse.success(notifyService.telegramStatus());
    }

    /** 사용자 연결 코드 발급 — 사용자가 봇에게 「/start 코드」를 보내면(또는 링크를 열면) chat ID 가 저장된다. */
    @PostMapping("/telegram/link-code/{userId}")
    public ApiResponse<NotifyDto.LinkCode> issueLinkCode(@PathVariable String userId) {
        return ApiResponse.success(telegramLinkService.issue(userId));
    }

    @DeleteMapping("/telegram/link/{userId}")
    public ApiResponse<Void> unlink(@PathVariable String userId) {
        telegramLinkService.unlink(userId);
        return ApiResponse.success("200", "텔레그램 연결을 해제했습니다.", null);
    }

    @GetMapping("/resolve/{userId}")
    public ApiResponse<NotifyDto.Resolve> resolve(@PathVariable String userId) {
        return ApiResponse.success(notifyService.resolve(userId));
    }

    @PostMapping("/test")
    public ApiResponse<NotifyDto.Result> test(@Valid @RequestBody NotifyDto.TestRequest request) {
        return ApiResponse.success(notifyService.send(request.userId(), "TEST", "[PMS] 알림 연결 테스트입니다. 이 메시지가 보이면 PMS 알림이 정상 연결된 것입니다."));
    }

    /** 담당자에게 진척 입력 버튼이 달린 알림을 보낸다(매일 알림 발송 연동 전의 수동 발송·검증용). */
    @PostMapping("/progress-prompt")
    public ApiResponse<NotifyDto.Result> progressPrompt(@Valid @RequestBody NotifyDto.ProgressPromptRequest request) {
        return ApiResponse.success(notifyService.sendProgressPrompt(request.taskId()));
    }

    @GetMapping("/settings")
    public ApiResponse<NotifyDto.Settings> settings() {
        return ApiResponse.success(jobService.getSettings());
    }

    @PutMapping("/settings")
    public ApiResponse<NotifyDto.Settings> saveSettings(@RequestBody NotifyDto.Settings request) {
        return ApiResponse.success("200", "알림 설정을 저장했습니다.", jobService.saveSettings(request));
    }

    /** 오늘 알릴 업무를 지금 처리한다. dryRun=true 면 보내지 않고 대상만 보여 준다. */
    @PostMapping("/run")
    public ApiResponse<NotifyDto.RunResult> run(@RequestParam(defaultValue = "true") boolean dryRun) {
        return ApiResponse.success(jobService.run(dryRun, NotifyType.Scope.ALL));
    }

    /** 알림 종류 목록(이름·설명·기본값). */
    @GetMapping("/types")
    public ApiResponse<List<NotifyDto.TypeInfo>> types() {
        return ApiResponse.success(jobService.types());
    }

    @GetMapping("/logs")
    public ApiResponse<List<NotifyDto.Log>> logs() {
        return ApiResponse.success(notifyService.recentLogs());
    }
}
