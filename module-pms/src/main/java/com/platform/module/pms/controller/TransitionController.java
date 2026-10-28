package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.TransitionDto;
import com.platform.module.pms.service.TransitionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 수행단계전환 요청·승인·롤백 API. 요청·조회·취소는 로그인 사용자, 승인·반려·롤백은 센터장·admin. */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsTransition", description = "PMS 단계전환 요청·승인·롤백 (테넌트 자동 격리)")
public class TransitionController {

    private static final String APPROVER = "hasAnyRole('PMS_APPROVE','ADMIN','SYSTEM_ADMIN')";

    private final TransitionService transitionService;

    public TransitionController(TransitionService transitionService) {
        this.transitionService = transitionService;
    }

    @PostMapping("/projects/{projectId}/transitions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TransitionDto.Response> request(@PathVariable String projectId, @Valid @RequestBody TransitionDto.Request request) {
        return ApiResponse.success("201", "단계전환을 요청했습니다.", transitionService.request(projectId, request));
    }

    @GetMapping("/transitions")
    public ApiResponse<List<TransitionDto.Response>> list(@RequestParam(required = false) String projectId,
                                                          @RequestParam(required = false) String status,
                                                          @RequestParam(required = false) String kind) {
        return ApiResponse.success(transitionService.getList(projectId, status, kind));
    }

    @GetMapping("/transitions/{id}")
    public ApiResponse<TransitionDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(transitionService.getById(id));
    }

    @PostMapping("/transitions/{id}/approve")
    @PreAuthorize(APPROVER)
    public ApiResponse<TransitionDto.Response> approve(@PathVariable String id) {
        return ApiResponse.success("200", "단계전환을 승인했습니다.", transitionService.approve(id));
    }

    @PostMapping("/transitions/{id}/reject")
    @PreAuthorize(APPROVER)
    public ApiResponse<TransitionDto.Response> reject(@PathVariable String id, @Valid @RequestBody TransitionDto.DecisionRequest request) {
        return ApiResponse.success("200", "반려했습니다.", transitionService.reject(id, request.comment()));
    }

    @PostMapping("/transitions/{id}/cancel")
    public ApiResponse<TransitionDto.Response> cancel(@PathVariable String id) {
        return ApiResponse.success("200", "요청을 취소했습니다.", transitionService.cancel(id));
    }

    @PostMapping("/transitions/{id}/rollback")
    @PreAuthorize(APPROVER)
    public ApiResponse<TransitionDto.Response> rollback(@PathVariable String id, @Valid @RequestBody TransitionDto.RollbackRequest request) {
        return ApiResponse.success("200", "단계전환을 롤백했습니다.", transitionService.rollback(id, request.reason()));
    }
}