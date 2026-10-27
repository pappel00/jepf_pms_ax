package com.platform.module.pms.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.pms.dto.UserProfileDto;
import com.platform.module.pms.service.UserProfileService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** PMS 사용자 확장 프로필 API. 조회는 로그인 사용자, 수정은 ADMIN. */
@RestController
@RequestMapping("/api/v1/pms/user-profiles")
@Tag(name = "PmsUserProfile", description = "PMS 사용자 프로필(소속 구분·협력사·인력 등급·메신저) (테넌트 자동 격리)")
public class UserProfileController {

    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ApiResponse<List<UserProfileDto.Response>> list() {
        return ApiResponse.success(profileService.getList());
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserProfileDto.Response> get(@PathVariable String userId) {
        return ApiResponse.success(profileService.get(userId));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserProfileDto.Response> upsert(@PathVariable String userId,
                                                       @Valid @RequestBody UserProfileDto.Request request) {
        return ApiResponse.success(profileService.upsert(userId, request));
    }
}