package com.platform.module.pms.controller;

import com.platform.module.pms.service.NotifyCallbackService;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mattermost 메시지 버튼 콜백. Mattermost 서버가 호출하므로 JWT 인증을 건너뛰고(SecurityConfig 허용 경로),
 * 대신 버튼에 심은 서명 토큰으로 검증한다. 응답은 ApiResponse 가 아니라 Mattermost 규격 JSON 이다.
 */
@Hidden
@RestController
@RequestMapping("/api/v1/pms/notify/callback")
public class NotifyCallbackController {

    private final NotifyCallbackService callbackService;

    public NotifyCallbackController(NotifyCallbackService callbackService) {
        this.callbackService = callbackService;
    }

    @PostMapping
    public Map<String, Object> callback(@RequestBody Map<String, Object> body) {
        return callbackService.handle(body);
    }
}
