package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.ProposalDto;
import com.platform.module.pms.service.ProposalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 제안서·버전·RFP 항목 대응·제출 체크리스트 API. */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsProposal", description = "PMS 제안서 (테넌트 자동 격리)")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @GetMapping("/proposals")
    public ApiResponse<List<ProposalDto.Response>> list() {
        return ApiResponse.success(proposalService.getList());
    }

    @PostMapping("/proposals")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProposalDto.Response> create(@Valid @RequestBody ProposalDto.Request request) {
        return ApiResponse.success("201", "제안서를 등록했습니다.", proposalService.create(request));
    }

    @PutMapping("/proposals/{id}")
    public ApiResponse<ProposalDto.Response> update(@PathVariable String id, @Valid @RequestBody ProposalDto.Request request) {
        return ApiResponse.success("200", "제안서를 저장했습니다.", proposalService.update(id, request));
    }

    @DeleteMapping("/proposals/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        proposalService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/proposals/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProposalDto.Response> addVersion(@PathVariable String id, @Valid @RequestBody ProposalDto.VersionRequest request) {
        return ApiResponse.success("201", "버전을 등록했습니다.", proposalService.addVersion(id, request));
    }

    @PutMapping("/proposals/{id}/mappings/{rfpItemId}")
    public ApiResponse<ProposalDto.Response> saveMapping(@PathVariable String id, @PathVariable String rfpItemId,
                                                         @RequestBody ProposalDto.MappingRequest request) {
        return ApiResponse.success("200", "항목 대응을 저장했습니다.", proposalService.saveMapping(id, rfpItemId, request));
    }

    @DeleteMapping("/proposals/{id}/mappings/{rfpItemId}")
    public ApiResponse<ProposalDto.Response> deleteMapping(@PathVariable String id, @PathVariable String rfpItemId) {
        return ApiResponse.success("200", "항목 대응을 지웠습니다.", proposalService.deleteMapping(id, rfpItemId));
    }

    @PostMapping("/proposals/{id}/checklist")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProposalDto.Response> addCheck(@PathVariable String id, @Valid @RequestBody ProposalDto.CheckRequest request) {
        return ApiResponse.success("201", "체크리스트 항목을 추가했습니다.", proposalService.addCheck(id, request.itemName()));
    }

    @PostMapping("/proposals/{id}/checklist/{checkId}/toggle")
    public ApiResponse<ProposalDto.Response> toggleCheck(@PathVariable String id, @PathVariable String checkId) {
        return ApiResponse.success("200", "체크리스트를 변경했습니다.", proposalService.toggleCheck(id, checkId));
    }

    @DeleteMapping("/proposals/{id}/checklist/{checkId}")
    public ApiResponse<ProposalDto.Response> removeCheck(@PathVariable String id, @PathVariable String checkId) {
        return ApiResponse.success("200", "체크리스트 항목을 삭제했습니다.", proposalService.removeCheck(id, checkId));
    }
}
