package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.StaffDto;
import com.platform.module.pms.service.StaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 인력 관리 API — 계정·PMS 프로필·PMS 역할. PM·센터장·관리자만 쓴다. */
@RestController
@RequestMapping("/api/v1/pms/staff")
@Tag(name = "PmsStaff", description = "PMS 인력 관리(계정·소속·등급·역할) (테넌트 자동 격리)")
@PreAuthorize("hasAnyRole('PMS_PM','PMS_MANAGER','ADMIN','SYSTEM_ADMIN')")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping
    public ApiResponse<List<StaffDto.Response>> list() {
        return ApiResponse.success(staffService.getList());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StaffDto.Response> create(@Valid @RequestBody StaffDto.CreateRequest request) {
        return ApiResponse.success("201", "인력이 등록되었습니다.", staffService.create(request));
    }

    @PutMapping("/{userId}")
    public ApiResponse<StaffDto.Response> update(@PathVariable String userId, @Valid @RequestBody StaffDto.UpdateRequest request) {
        return ApiResponse.success(staffService.update(userId, request));
    }
}
