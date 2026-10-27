package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.CustomerContactDto;
import com.platform.module.pms.service.CustomerContactService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 고객사 담당자 마스터 API. 조회·등록·수정은 로그인 사용자, 삭제는 ADMIN. */
@RestController
@RequestMapping("/api/v1/pms/customer-contacts")
@Tag(name = "PmsCustomerContact", description = "PMS 고객사 담당자 관리 (테넌트 자동 격리)")
public class CustomerContactController {

    private final CustomerContactService contactService;

    public CustomerContactController(CustomerContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerContactDto.Response> create(@Valid @RequestBody CustomerContactDto.Request request) {
        return ApiResponse.success("201", "담당자가 등록되었습니다.", contactService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerContactDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(contactService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<CustomerContactDto.Response>> list(@RequestParam(required = false) String customerId, Pageable pageable) {
        var result = contactService.getList(customerId, pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerContactDto.Response> update(@PathVariable String id,
                                                           @Valid @RequestBody CustomerContactDto.Request request) {
        return ApiResponse.success(contactService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        contactService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}