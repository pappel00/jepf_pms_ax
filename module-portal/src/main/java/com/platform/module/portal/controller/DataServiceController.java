package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.DataServiceDto;
import com.platform.module.portal.service.DataServiceService;
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
 * 데이터서비스(내부 API 엔드포인트 메타데이터) 관리 API.
 * URL 등 내부 구조를 노출하는 민감한 정보라 조회를 포함해 전체 ADMIN 권한으로 제한한다
 * (User/Role 컨트롤러와 동일한 클래스 레벨 @PreAuthorize 패턴).
 */
@RestController
@RequestMapping("/api/v1/data-services")
@Tag(name = "DataService", description = "데이터서비스 관리 (테넌트 자동 격리, ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class DataServiceController {

    private final DataServiceService dataServiceService;

    public DataServiceController(DataServiceService dataServiceService) {
        this.dataServiceService = dataServiceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DataServiceDto.Response> create(@Valid @RequestBody DataServiceDto.CreateRequest request) {
        return ApiResponse.success("201", "데이터서비스가 등록되었습니다.", dataServiceService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DataServiceDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(dataServiceService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<DataServiceDto.Response>> list(Pageable pageable) {
        var result = dataServiceService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    public ApiResponse<DataServiceDto.Response> update(@PathVariable String id,
                                                          @Valid @RequestBody DataServiceDto.UpdateRequest request) {
        return ApiResponse.success(dataServiceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        dataServiceService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
