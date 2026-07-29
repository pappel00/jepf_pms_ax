package com.platform.module.portal.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.portal.domain.MessageResource;
import com.platform.module.portal.dto.MessageResourceDto;
import com.platform.module.portal.exception.PortalModuleErrorCode;
import com.platform.module.portal.repository.MessageResourceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MessageResourceService {

    private final MessageResourceRepository messageResourceRepository;

    public MessageResourceService(MessageResourceRepository messageResourceRepository) {
        this.messageResourceRepository = messageResourceRepository;
    }

    @Transactional
    public MessageResourceDto.Response create(MessageResourceDto.CreateRequest request) {
        if (messageResourceRepository.existsByMessageResourceCodeAndLanguageId(
                request.messageResourceCode(), request.languageId())) {
            throw new BusinessException(PortalModuleErrorCode.MESSAGE_RESOURCE_DUPLICATED,
                    "이미 존재하는 메시지자원입니다: " + request.messageResourceCode() + " (" + request.languageId() + ")");
        }
        MessageResource messageResource = MessageResource.create(request.messageResourceCode(),
                request.resourceType(), request.languageId(), request.value(), request.defaultValue());
        return MessageResourceDto.Response.from(messageResourceRepository.saveAndFlush(messageResource));
    }

    public MessageResourceDto.Response getById(String id) {
        return MessageResourceDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<MessageResourceDto.Response> getList(Pageable pageable) {
        Page<MessageResource> page = messageResourceRepository.findAll(pageable);
        return new PagedResult<>(page.map(MessageResourceDto.Response::from).getContent(), PageInfo.of(page));
    }

    public PagedResult<MessageResourceDto.Response> getListByCode(String messageResourceCode, Pageable pageable) {
        Page<MessageResource> page = messageResourceRepository
                .findAllByMessageResourceCode(messageResourceCode, pageable);
        return new PagedResult<>(page.map(MessageResourceDto.Response::from).getContent(), PageInfo.of(page));
    }

    /** 프론트가 "이 코드를 이 언어로 어떻게 표시할지" 물어볼 때 사용. 없으면 defaultValue 로 폴백하지 않고 그대로 404. */
    public MessageResourceDto.Response resolve(String messageResourceCode, String languageId) {
        return messageResourceRepository
                .findByMessageResourceCodeAndLanguageIdAndDeletedYn(messageResourceCode, languageId, "N")
                .map(MessageResourceDto.Response::from)
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.MESSAGE_RESOURCE_NOT_FOUND,
                        "존재하지 않는 메시지자원입니다: " + messageResourceCode + " (" + languageId + ")"));
    }

    @Transactional
    public MessageResourceDto.Response update(String id, MessageResourceDto.UpdateRequest request) {
        MessageResource messageResource = findActiveOrThrow(id);
        messageResource.update(request.value(), request.defaultValue());
        if ("Y".equals(request.useYn())) {
            messageResource.activate();
        } else {
            messageResource.deactivate();
        }
        return MessageResourceDto.Response.from(messageResource);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private MessageResource findActiveOrThrow(String id) {
        return messageResourceRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(PortalModuleErrorCode.MESSAGE_RESOURCE_NOT_FOUND,
                        "존재하지 않는 메시지자원입니다: " + id));
    }
}
