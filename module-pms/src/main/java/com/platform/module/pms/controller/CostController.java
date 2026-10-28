package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.CostDto;
import com.platform.module.pms.service.CostService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 단가·실적 M/M·PMS 설정 API. 단가 조회는 원가 열람(PMS_COST_VIEW), 단가·설정 변경은 승인 권한(PMS_APPROVE). */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsCost", description = "PMS 단가·실적 M/M·설정 (테넌트 자동 격리)")
public class CostController {

    private static final String COST_VIEW = "hasAnyRole('PMS_COST_VIEW','ADMIN','SYSTEM_ADMIN')";
    private static final String APPROVER = "hasAnyRole('PMS_APPROVE','ADMIN','SYSTEM_ADMIN')";

    private final CostService costService;

    public CostController(CostService costService) {
        this.costService = costService;
    }

    @GetMapping("/rates")
    @PreAuthorize(COST_VIEW)
    public ApiResponse<List<CostDto.RateResponse>> rates() {
        return ApiResponse.success(costService.getRates());
    }

    @PostMapping("/rates")
    @PreAuthorize(APPROVER)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CostDto.RateResponse> createRate(@Valid @RequestBody CostDto.RateRequest request) {
        return ApiResponse.success("201", "단가를 등록했습니다.", costService.createRate(request));
    }

    @PutMapping("/rates/{id}")
    @PreAuthorize(APPROVER)
    public ApiResponse<CostDto.RateResponse> updateRate(@PathVariable String id, @Valid @RequestBody CostDto.RateRequest request) {
        return ApiResponse.success("200", "단가를 저장했습니다.", costService.updateRate(id, request));
    }

    @DeleteMapping("/rates/{id}")
    @PreAuthorize(APPROVER)
    public ApiResponse<Void> deleteRate(@PathVariable String id) {
        costService.deleteRate(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @GetMapping("/actual-mm")
    public ApiResponse<List<CostDto.ActualResponse>> actuals() {
        return ApiResponse.success(costService.getActuals());
    }

    @PutMapping("/projects/{projectId}/actual-mm")
    public ApiResponse<Void> saveActual(@PathVariable String projectId, @Valid @RequestBody CostDto.ActualRequest request) {
        costService.saveActual(projectId, request);
        return ApiResponse.success("200", "실적을 저장했습니다.", null);
    }

    @GetMapping("/settings")
    public ApiResponse<Map<String, String>> settings() {
        return ApiResponse.success(costService.getSettings());
    }

    @PutMapping("/settings")
    @PreAuthorize(APPROVER)
    public ApiResponse<Map<String, String>> saveSettings(@Valid @RequestBody CostDto.SettingsRequest request) {
        return ApiResponse.success("200", "설정을 저장했습니다.", costService.saveSettings(request.values()));
    }
}
