package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.ReportDto;
import com.platform.module.pms.service.ReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 주간·월간 보고서와 진척 스냅샷 API. 보고서는 (프로젝트, 유형 WEEK/MONTH, 기간 시작일)로 식별한다. */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsReport", description = "PMS 보고서·진척 스냅샷 (테넌트 자동 격리)")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public ApiResponse<List<ReportDto.Response>> list() {
        return ApiResponse.success(reportService.getList());
    }

    @PutMapping("/projects/{projectId}/reports/{type}/{periodStart}/draft")
    public ApiResponse<ReportDto.Response> saveDraft(@PathVariable String projectId, @PathVariable String type,
                                                     @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
                                                     @RequestBody ReportDto.DraftRequest request) {
        return ApiResponse.success("200", "보고서를 저장했습니다.", reportService.saveDraft(projectId, type, periodStart, request.memo()));
    }

    @PostMapping("/projects/{projectId}/reports/{type}/{periodStart}/submit")
    public ApiResponse<ReportDto.Response> submit(@PathVariable String projectId, @PathVariable String type,
                                                  @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
                                                  @Valid @RequestBody ReportDto.SubmitRequest request) {
        return ApiResponse.success("200", "보고서를 제출했습니다.", reportService.submit(projectId, type, periodStart, request));
    }

    @PostMapping("/projects/{projectId}/reports/{type}/{periodStart}/reopen")
    public ApiResponse<ReportDto.Response> reopen(@PathVariable String projectId, @PathVariable String type,
                                                  @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart,
                                                  @Valid @RequestBody ReportDto.ReopenRequest request) {
        return ApiResponse.success("200", "수정을 시작합니다.", reportService.reopen(projectId, type, periodStart, request.reason()));
    }

    @PostMapping("/projects/{projectId}/reports/{type}/{periodStart}/cancel-edit")
    public ApiResponse<ReportDto.Response> cancelEdit(@PathVariable String projectId, @PathVariable String type,
                                                      @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {
        return ApiResponse.success("200", "수정을 취소했습니다.", reportService.cancelEdit(projectId, type, periodStart));
    }

    @GetMapping("/progress-snapshots")
    public ApiResponse<List<ReportDto.SnapshotResponse>> snapshots() {
        return ApiResponse.success(reportService.getSnapshots());
    }

    @PutMapping("/projects/{projectId}/progress-snapshots")
    public ApiResponse<ReportDto.SnapshotResponse> saveSnapshot(@PathVariable String projectId, @Valid @RequestBody ReportDto.SnapshotRequest request) {
        return ApiResponse.success("200", "스냅샷을 저장했습니다.", reportService.saveSnapshot(projectId, request));
    }
}
