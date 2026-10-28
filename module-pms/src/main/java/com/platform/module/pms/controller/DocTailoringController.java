package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.DocDto;
import com.platform.module.pms.service.DocTailoringService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 문서 테일러링·단계말 산출물 API. 산출물 관리는 PM(프로젝트 관리자)만 — 서버 판정은 프로젝트 멤버 권한 적용 때 추가한다. */
@RestController
@RequestMapping("/api/v1/pms")
@Tag(name = "PmsDocTailoring", description = "PMS 문서 테일러링·산출물 (테넌트 자동 격리)")
public class DocTailoringController {

    private final DocTailoringService docService;

    public DocTailoringController(DocTailoringService docService) {
        this.docService = docService;
    }

    @GetMapping("/doc-tailoring")
    public ApiResponse<List<DocDto.Response>> list() {
        return ApiResponse.success(docService.getList());
    }

    @PostMapping("/projects/{projectId}/doc-tailoring/init")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<DocDto.Response>> init(@PathVariable String projectId, @Valid @RequestBody DocDto.InitRequest request) {
        return ApiResponse.success("201", "문서 테일러링을 생성했습니다.", docService.init(projectId, request));
    }

    @PostMapping("/projects/{projectId}/doc-tailoring/reset")
    public ApiResponse<List<DocDto.Response>> reset(@PathVariable String projectId) {
        return ApiResponse.success("200", "표준 목록으로 초기화했습니다.", docService.reset(projectId));
    }

    @PostMapping("/projects/{projectId}/doc-tailoring")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocDto.Response> addCustom(@PathVariable String projectId, @Valid @RequestBody DocDto.CustomRequest request) {
        return ApiResponse.success("201", "문서 종류를 추가했습니다.", docService.addCustom(projectId, request));
    }

    @PatchMapping("/doc-tailoring/{id}")
    public ApiResponse<DocDto.Response> patch(@PathVariable String id, @Valid @RequestBody DocDto.PatchRequest request) {
        return ApiResponse.success("200", "저장했습니다.", docService.patch(id, request));
    }

    @DeleteMapping("/doc-tailoring/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        docService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }

    @PostMapping("/doc-tailoring/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocDto.Response> addVersion(@PathVariable String id, @Valid @RequestBody DocDto.VersionRequest request) {
        return ApiResponse.success("201", "산출물을 등록했습니다.", docService.addVersion(id, request));
    }

    @PutMapping("/doc-tailoring/{id}/versions/latest")
    public ApiResponse<DocDto.Response> updateLatest(@PathVariable String id, @Valid @RequestBody DocDto.VersionRequest request) {
        return ApiResponse.success("200", "최신 버전을 수정했습니다.", docService.updateLatest(id, request));
    }

    @DeleteMapping("/doc-tailoring/{id}/versions/latest")
    public ApiResponse<DocDto.Response> deleteLatest(@PathVariable String id) {
        return ApiResponse.success("200", "최신 버전을 삭제했습니다.", docService.deleteLatest(id));
    }

    @PutMapping("/doc-tailoring/{id}/reviewed")
    public ApiResponse<DocDto.Response> setReviewed(@PathVariable String id, @Valid @RequestBody DocDto.ReviewedRequest request) {
        return ApiResponse.success("200", "검토 상태를 저장했습니다.", docService.setReviewed(id, request.reviewed()));
    }
}
