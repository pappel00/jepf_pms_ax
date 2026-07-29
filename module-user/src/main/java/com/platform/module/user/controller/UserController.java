package com.platform.module.user.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.user.dto.UserDto;
import com.platform.module.user.service.UserService;
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
 * 인증이 필요한 API. 모든 조회/수정은 요청자의 테넌트로 자동 범위 제한된다.
 * 다른 사용자의 정보를 다루는 API이므로 클래스 전체를 ADMIN 권한으로 제한한다.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 관리 (테넌트 자동 격리, ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 회원가입(/api/v1/auth/register)과 별개로, 관리자가 사용자 관리 화면에서 직접 사용자를 만든다. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto.Response> create(@Valid @RequestBody UserDto.CreateRequest request) {
        return ApiResponse.success("201", "사용자가 생성되었습니다.", userService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(userService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<UserDto.Response>> list(Pageable pageable) {
        var result = userService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    @PutMapping("/{id}")
    public ApiResponse<UserDto.Response> update(@PathVariable String id,
                                                  @Valid @RequestBody UserDto.UpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        userService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
