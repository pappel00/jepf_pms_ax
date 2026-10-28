package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.TeamChangeDto;
import com.platform.module.pms.service.TeamChangeService;
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

/** 투입 계획 변경 요청·승인 API. 요청·조회·취소는 로그인 사용자, 승인·반려는 센터장·admin(요청자 본인 제외). */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsTeamChange", description = "PMS 투입 계획 변경 요청·승인 (테넌트 자동 격리)")
public class TeamChangeController {

    private static final String APPROVER = "hasAnyRole('PMS_APPROVE','ADMIN','SYSTEM_ADMIN')";

    private final TeamChangeService changeService;

    public TeamChangeController(TeamChangeService changeService) {
        this.changeService = changeService;
    }

    @PostMapping("/projects/{projectId}/team-changes")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TeamChangeDto.Response> request(@PathVariable String projectId, @Valid @RequestBody TeamChangeDto.Request request) {
        return ApiResponse.success("201", "투입 계획 변경을 요청했습니다.", changeService.request(projectId, request));
    }

    @GetMapping("/team-changes")
    public ApiResponse<List<TeamChangeDto.Response>> list(@RequestParam(required = false) String projectId,
                                                          @RequestParam(required = false) String status) {
        return ApiResponse.success(changeService.getList(projectId, status));
    }

    @PostMapping("/team-changes/{id}/approve")
    @PreAuthorize(APPROVER)
    public ApiResponse<TeamChangeDto.Response> approve(@PathVariable String id, @RequestBody(required = false) TeamChangeDto.DecisionRequest request) {
        return ApiResponse.success("200", "승인했습니다.", changeService.approve(id, request == null ? null : request.comment()));
    }

    @PostMapping("/team-changes/{id}/reject")
    @PreAuthorize(APPROVER)
    public ApiResponse<TeamChangeDto.Response> reject(@PathVariable String id, @Valid @RequestBody TeamChangeDto.DecisionRequest request) {
        return ApiResponse.success("200", "반려했습니다.", changeService.reject(id, request.comment()));
    }

    @PostMapping("/team-changes/{id}/cancel")
    public ApiResponse<TeamChangeDto.Response> cancel(@PathVariable String id) {
        return ApiResponse.success("200", "요청을 취소했습니다.", changeService.cancel(id));
    }
}