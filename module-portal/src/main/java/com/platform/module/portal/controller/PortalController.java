package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.PortalDto;
import com.platform.module.portal.service.PortalService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
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

/**
 * 포탈 관리 API. 조회는 로그인만 되어 있으면 가능(자신이 속한 포탈 구조를 렌더링하기 위함),
 * 생성/수정/삭제는 ADMIN 권한이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/portals")
@Tag(name = "Portal", description = "포탈 관리 (테넌트 자동 격리)")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PortalDto.Response> create(@Valid @RequestBody PortalDto.CreateRequest request) {
        return ApiResponse.success("201", "포탈이 등록되었습니다.", portalService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<PortalDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(portalService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<PortalDto.Response>> list(Pageable pageable) {
        var result = portalService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PortalDto.Response> update(@PathVariable String id,
                                                    @Valid @RequestBody PortalDto.UpdateRequest request) {
        return ApiResponse.success(portalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        portalService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
