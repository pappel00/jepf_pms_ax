package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.PageMessageResourceDto;
import com.platform.module.portal.service.PageMessageResourceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사진 속 페이지 관리 화면의 "메시지 자원" 탭 대응. 조회는 로그인만 되어 있으면 가능,
 * 매핑 추가/삭제는 ADMIN 권한이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/pages/{pageId}/message-resources")
@Tag(name = "PageMessageResourceMapping", description = "페이지-메시지자원 매핑")
public class PageMessageResourceMappingController {

    private final PageMessageResourceService pageMessageResourceService;

    public PageMessageResourceMappingController(PageMessageResourceService pageMessageResourceService) {
        this.pageMessageResourceService = pageMessageResourceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageMessageResourceDto.Response> create(
            @PathVariable String pageId, @Valid @RequestBody PageMessageResourceDto.CreateRequest request) {
        return ApiResponse.success("201", "메시지자원이 페이지에 매핑되었습니다.",
                pageMessageResourceService.create(pageId, request));
    }

    @GetMapping
    public ApiResponse<List<PageMessageResourceDto.Response>> list(@PathVariable String pageId) {
        return ApiResponse.success(pageMessageResourceService.getList(pageId));
    }

    @DeleteMapping("/{mappingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String pageId, @PathVariable String mappingId) {
        pageMessageResourceService.delete(pageId, mappingId);
        return ApiResponse.success("200", "매핑이 삭제되었습니다.", null);
    }
}
