package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.NotifyDto;
import com.platform.module.pms.service.NotifyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public NotifyController(NotifyService notifyService) {
        this.notifyService = notifyService;
    }

    @GetMapping("/status")
    public ApiResponse<NotifyDto.Status> status() {
        return ApiResponse.success(notifyService.status());
    }

    @GetMapping("/resolve/{userId}")
    public ApiResponse<NotifyDto.Resolve> resolve(@PathVariable String userId) {
        return ApiResponse.success(notifyService.resolve(userId));
    }

    @PostMapping("/test")
    public ApiResponse<NotifyDto.Result> test(@Valid @RequestBody NotifyDto.TestRequest request) {
        return ApiResponse.success(notifyService.send(request.userId(), "TEST", "[PMS] 알림 연결 테스트입니다. 이 메시지가 보이면 PMS 알림이 정상 연결된 것입니다."));
    }

    @GetMapping("/logs")
    public ApiResponse<List<NotifyDto.Log>> logs() {
        return ApiResponse.success(notifyService.recentLogs());
    }
}
