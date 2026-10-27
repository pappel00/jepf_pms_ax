package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.CustomerDto;
import com.platform.module.pms.service.CustomerService;
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

/** 고객사(발주사) 마스터 API. 조회·등록·수정은 로그인 사용자, 삭제는 ADMIN. */
@RestController
@RequestMapping("/api/v1/pms/customers")
@Tag(name = "PmsCustomer", description = "PMS 고객사 관리 (테넌트 자동 격리)")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CustomerDto.Response> create(@Valid @RequestBody CustomerDto.Request request) {
        return ApiResponse.success("201", "고객사가 등록되었습니다.", customerService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<CustomerDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(customerService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<CustomerDto.Response>> list(Pageable pageable) {
        var result = customerService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    public ApiResponse<CustomerDto.Response> update(@PathVariable String id, @Valid @RequestBody CustomerDto.Request request) {
        return ApiResponse.success(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        customerService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}