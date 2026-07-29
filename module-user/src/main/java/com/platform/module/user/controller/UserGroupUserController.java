package com.platform.module.user.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.user.dto.UserDto;
import com.platform.module.user.dto.UserGroupUserDto;
import com.platform.module.user.service.UserGroupUserService;
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
 * 사진 속 "사용자그룹 관리" 화면의 "사용자" 탭 대응. 이 그룹에 사용자를 소속시킨다.
 *
 * ⚠️ 다른 매핑 컨트롤러들(PageRoleMappingController 등)과 달리 DELETE가 매핑 자체의 내부 PK가
 * 아니라 userId를 받는다 — GET(list)이 매핑 레코드가 아니라 User 정보를 돌려주기 때문에,
 * 프론트엔드가 애초에 매핑의 내부 PK를 알 방법이 없다(User.id만 안다). userId로 바로 지우는 게
 * 더 자연스럽고 실제로 프론트에서 쓸 수 있는 유일한 방법이다.
 */
@RestController
@RequestMapping("/api/v1/user-groups/{userGroupId}/users")
@Tag(name = "UserGroupUserMapping", description = "사용자그룹-사용자 매핑 (ADMIN 권한 필요)")
@PreAuthorize("hasRole('ADMIN')")
public class UserGroupUserController {

    private final UserGroupUserService userGroupUserService;

    public UserGroupUserController(UserGroupUserService userGroupUserService) {
        this.userGroupUserService = userGroupUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserDto.Response> add(
            @PathVariable String userGroupId, @Valid @RequestBody UserGroupUserDto.AssignRequest request) {
        return ApiResponse.success("201", "사용자가 그룹에 추가되었습니다.", userGroupUserService.addUser(userGroupId, request));
    }

    @GetMapping
    public ApiResponse<List<UserDto.Response>> list(@PathVariable String userGroupId) {
        return ApiResponse.success(userGroupUserService.getUsers(userGroupId));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> remove(@PathVariable String userGroupId, @PathVariable String userId) {
        userGroupUserService.removeUser(userGroupId, userId);
        return ApiResponse.success("200", "사용자가 그룹에서 제거되었습니다.", null);
    }
}
