package com.platform.module.portal.controller;

import com.platform.framework.core.common.response.ApiResponse;
import com.platform.module.portal.dto.MessageResourceDto;
import com.platform.module.portal.service.MessageResourceService;
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

/**
 * 다국어 메시지자원 관리 API. Page 등 다른 화면이 표시 문구를 다국어로 해석할 때 이 API를 쓴다.
 * 조회는 로그인만 되어 있으면 가능(모든 사용자가 자기 언어로 UI 문구를 조회해야 함),
 * 생성/수정/삭제는 ADMIN 권한이 필요하다.
 */
@RestController
@RequestMapping("/api/v1/message-resources")
@Tag(name = "MessageResource", description = "다국어 메시지자원 관리 (테넌트 자동 격리)")
public class MessageResourceController {

    private final MessageResourceService messageResourceService;

    public MessageResourceController(MessageResourceService messageResourceService) {
        this.messageResourceService = messageResourceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MessageResourceDto.Response> create(
            @Valid @RequestBody MessageResourceDto.CreateRequest request) {
        return ApiResponse.success("201", "메시지자원이 등록되었습니다.", messageResourceService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<MessageResourceDto.Response> get(@PathVariable String id) {
        return ApiResponse.success(messageResourceService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<MessageResourceDto.Response>> list(
            @RequestParam(required = false) String messageResourceCode, Pageable pageable) {
        var result = messageResourceCode != null
                ? messageResourceService.getListByCode(messageResourceCode, pageable)
                : messageResourceService.getList(pageable);
        return ApiResponse.successList(result.content(), result.pageInfo());
    }

    /**
     * 프론트가 "이 코드를 이 언어로 어떻게 표시할지" 바로 물어보는 조회용 엔드포인트.
     * 예) GET /api/v1/message-resources/resolve?code=MSG_ALT_CHK_UNIT_DUP&languageId=ko
     */
    @GetMapping("/resolve")
    public ApiResponse<MessageResourceDto.Response> resolve(
            @RequestParam String code, @RequestParam String languageId) {
        return ApiResponse.success(messageResourceService.resolve(code, languageId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<MessageResourceDto.Response> update(
            @PathVariable String id, @Valid @RequestBody MessageResourceDto.UpdateRequest request) {
        return ApiResponse.success(messageResourceService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> delete(@PathVariable String id) {
        messageResourceService.delete(id);
        return ApiResponse.success("200", "삭제되었습니다.", null);
    }
}
