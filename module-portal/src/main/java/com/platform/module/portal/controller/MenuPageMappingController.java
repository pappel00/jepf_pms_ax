package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.MenuPageDto;
import com.platform.module.portal.service.MenuPageService;
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
 * 사진 속 "메뉴 관리" 화면에서 왼쪽 페이지를 체크해서 오른쪽 선택된 폴더 하위로 저장하는
 * 동작 대응. 조회는 로그인만 되어 있으면 가능, 매핑 추가/삭제는 ADMIN 권한이 필요하다
 * (PageDataServiceMappingController와 동일한 패턴).
 */
@RestController
@RequestMapping("/api/v1/menus/{menuId}/pages")
@Tag(name = "MenuPageMapping", description = "메뉴(폴더)-페이지 매핑")
public class MenuPageMappingController {

    private final MenuPageService menuPageService;

    public MenuPageMappingController(MenuPageService menuPageService) {
        this.menuPageService = menuPageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MenuPageDto.Response> create(
            @PathVariable String menuId, @Valid @RequestBody MenuPageDto.CreateRequest request) {
        return ApiResponse.success("201", "페이지가 폴더에 추가되었습니다.", menuPageService.create(menuId, request));
    }

    @GetMapping
    public ApiResponse<List<MenuPageDto.Response>> list(@PathVariable String menuId) {
        return ApiResponse.success(menuPageService.getList(menuId));
    }

    @DeleteMapping("/{mappingId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String menuId, @PathVariable String mappingId) {
        menuPageService.delete(menuId, mappingId);
        return ApiResponse.success("200", "매핑이 삭제되었습니다.", null);
    }
}
