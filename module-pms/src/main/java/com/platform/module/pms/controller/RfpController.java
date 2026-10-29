package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.RfpDto;
import com.platform.module.pms.dto.RfpParseDto;
import com.platform.module.pms.service.RfpParseService;
import com.platform.module.pms.service.RfpService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** RFP·항목·평가자·평가·확정 M/D API. 재평가 요청·확정은 평가 관리자(센터장·admin). */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsRfp", description = "PMS RFP·평가 (테넌트 자동 격리)")
public class RfpController {

    private static final String EVAL_ADMIN = "hasAnyRole('PMS_APPROVE','ADMIN','SYSTEM_ADMIN')";

    private final RfpService rfpService;
    private final RfpParseService parseService;

    public RfpController(RfpService rfpService, RfpParseService parseService) {
        this.rfpService = rfpService;
        this.parseService = parseService;
    }

    /** RFP 문서(hwpx) 자동 분해 미리보기 — 저장하지 않는다. 확인한 항목은 항목 일괄 등록 API 로 저장한다. */
    @PostMapping(value = "/rfp-parse", consumes = "multipart/form-data")
    public ApiResponse<RfpParseDto.Response> parse(@RequestParam("file") MultipartFile file,
                                                    @RequestParam(defaultValue = "false") boolean splitLines) {
        return ApiResponse.success("200", "문서를 분석했습니다.", parseService.parse(file, splitLines));
    }

    @GetMapping("/rfps")
    public ApiResponse<List<RfpDto.Response>> list(@RequestParam(required = false) String projectId) {
        return ApiResponse.success(rfpService.getList(projectId));
    }

    @PostMapping("/projects/{projectId}/rfps")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RfpDto.Response> create(@PathVariable String projectId, @Valid @RequestBody RfpDto.Request request) {
        return ApiResponse.success("201", "RFP를 등록했습니다.", rfpService.create(projectId, request));
    }

    @PutMapping("/rfps/{id}")
    public ApiResponse<RfpDto.Response> update(@PathVariable String id, @Valid @RequestBody RfpDto.Request request) {
        return ApiResponse.success("200", "RFP를 저장했습니다.", rfpService.update(id, request));
    }

    @DeleteMapping("/rfps/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        rfpService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PutMapping("/rfps/{id}/evaluators")
    public ApiResponse<RfpDto.Response> setEvaluators(@PathVariable String id, @Valid @RequestBody RfpDto.EvaluatorsRequest request) {
        return ApiResponse.success("200", "평가자를 지정했습니다.", rfpService.setEvaluators(id, request.userIds()));
    }

    @PostMapping("/rfps/{id}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RfpDto.Response> addItem(@PathVariable String id, @Valid @RequestBody RfpDto.ItemRequest request) {
        return ApiResponse.success("201", "항목을 추가했습니다.", rfpService.addItem(id, request));
    }

    @PostMapping("/rfps/{id}/items/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RfpDto.Response> addItems(@PathVariable String id, @Valid @RequestBody RfpDto.ItemBulkRequest request) {
        return ApiResponse.success("201", "항목을 추가했습니다.", rfpService.addItems(id, request.items()));
    }

    @PutMapping("/rfp-items/{itemId}")
    public ApiResponse<RfpDto.Response> updateItem(@PathVariable String itemId, @Valid @RequestBody RfpDto.ItemRequest request) {
        return ApiResponse.success("200", "항목을 저장했습니다.", rfpService.updateItem(itemId, request));
    }

    @DeleteMapping("/rfp-items/{itemId}")
    public ApiResponse<RfpDto.Response> deleteItem(@PathVariable String itemId) {
        return ApiResponse.success("200", "항목을 삭제했습니다.", rfpService.deleteItem(itemId));
    }

    @GetMapping("/rfp-evals")
    public ApiResponse<List<RfpDto.EvalResponse>> evals() {
        return ApiResponse.success(rfpService.getVisibleEvals());
    }

    @PutMapping("/rfp-items/{itemId}/evals/me")
    public ApiResponse<RfpDto.EvalResponse> saveMyEval(@PathVariable String itemId, @Valid @RequestBody RfpDto.EvalRequest request) {
        return ApiResponse.success("200", "평가를 저장했습니다.", rfpService.saveMyEval(itemId, request));
    }

    @PostMapping("/rfps/{id}/evals/submit")
    public ApiResponse<RfpDto.Response> submit(@PathVariable String id) {
        return ApiResponse.success("200", "평가를 제출했습니다.", rfpService.submit(id));
    }

    @PostMapping("/rfps/{id}/evaluators/{userId}/reopen")
    @PreAuthorize(EVAL_ADMIN)
    public ApiResponse<RfpDto.Response> reopen(@PathVariable String id, @PathVariable String userId) {
        return ApiResponse.success("200", "재평가를 요청했습니다.", rfpService.reopen(id, userId));
    }

    @PutMapping("/rfp-items/{itemId}/confirm")
    @PreAuthorize(EVAL_ADMIN)
    public ApiResponse<RfpDto.Response> confirm(@PathVariable String itemId, @Valid @RequestBody RfpDto.ConfirmRequest request) {
        return ApiResponse.success("200", "확정 M/D를 저장했습니다.", rfpService.confirm(itemId, request));
    }

    @PostMapping("/rfps/{id}/confirm-all")
    @PreAuthorize(EVAL_ADMIN)
    public ApiResponse<RfpDto.Response> confirmAll(@PathVariable String id, @Valid @RequestBody RfpDto.ConfirmAllRequest request) {
        return ApiResponse.success("200", "확정 평가자를 일괄 지정했습니다.", rfpService.confirmAll(id, request.evaluatorUserId()));
    }
}
