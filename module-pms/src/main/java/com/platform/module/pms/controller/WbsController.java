package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.WbsDto;
import com.platform.module.pms.service.WbsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** WBS 업무 API — 조회, 단계전환 시 일괄 생성, 진척률 입력, 업무 복사. */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsWbs", description = "PMS WBS 업무·진척 (테넌트 자동 격리)")
public class WbsController {

    private final WbsService wbsService;

    public WbsController(WbsService wbsService) {
        this.wbsService = wbsService;
    }

    @GetMapping("/wbs-tasks")
    public ApiResponse<List<WbsDto.Response>> list(@RequestParam(required = false) String projectId) {
        return ApiResponse.success(wbsService.getList(projectId));
    }

    @PostMapping("/projects/{projectId}/wbs-tasks/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<WbsDto.Response>> createBulk(@PathVariable String projectId, @Valid @RequestBody WbsDto.BulkRequest request) {
        return ApiResponse.success("201", "WBS를 생성했습니다.", wbsService.createBulk(projectId, request));
    }

    @PutMapping("/wbs-tasks/{id}/progress")
    public ApiResponse<WbsDto.Response> updateProgress(@PathVariable String id, @Valid @RequestBody WbsDto.ProgressRequest request) {
        return ApiResponse.success("200", "진척률을 저장했습니다.", wbsService.updateProgress(id, request));
    }

    @PostMapping("/wbs-tasks/{id}/copy")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<WbsDto.Response>> copy(@PathVariable String id, @RequestBody WbsDto.CopyRequest request) {
        return ApiResponse.success("201", "업무를 복사했습니다.", wbsService.copy(id, request));
    }
}
