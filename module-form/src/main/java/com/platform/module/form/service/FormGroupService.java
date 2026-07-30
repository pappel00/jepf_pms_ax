package com.platform.module.form.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.form.domain.FormGroup;
import com.platform.module.form.dto.FormGroupDto;
import com.platform.module.form.exception.FormModuleErrorCode;
import com.platform.module.form.repository.FormGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FormGroupService {

    private final FormGroupRepository formGroupRepository;

    public FormGroupService(FormGroupRepository formGroupRepository) {
        this.formGroupRepository = formGroupRepository;
    }

    @Transactional
    public FormGroupDto.Response create(FormGroupDto.CreateRequest request) {
        if (formGroupRepository.existsByPortalIdAndFormCode(request.portalId(), request.formCode())) {
            throw new BusinessException(FormModuleErrorCode.FORM_GROUP_CODE_DUPLICATED,
                    "이 포탈에 이미 존재하는 양식입니다: " + request.formCode());
        }
        FormGroup group = FormGroup.create(
                request.portalId(), request.formCode(), request.formName(), request.category(), request.description());
        return FormGroupDto.Response.from(formGroupRepository.saveAndFlush(group));
    }

    public FormGroupDto.Response getById(String id) {
        return FormGroupDto.Response.from(findActiveOrThrow(id));
    }

    /** portalId가 주어지면 그 포탈의 양식만, 없으면 테넌트 전체를 조회한다. */
    public PagedResult<FormGroupDto.Response> getList(String portalId, Pageable pageable) {
        Page<FormGroup> page = (portalId != null && !portalId.isBlank())
                ? formGroupRepository.findAllByPortalId(portalId, pageable)
                : formGroupRepository.findAll(pageable);
        return new PagedResult<>(page.map(FormGroupDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public FormGroupDto.Response update(String id, FormGroupDto.UpdateRequest request) {
        FormGroup group = findActiveOrThrow(id);
        group.update(request.formName(), request.category(), request.description());
        if ("Y".equals(request.useYn())) {
            group.activate();
        } else {
            group.deactivate();
        }
        return FormGroupDto.Response.from(group);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    /** 삭제된 행에서 "삭제"를 다시 누르면 404가 나던 문제의 대응 — 소프트 삭제를 되돌린다. */
    @Transactional
    public void restore(String id) {
        findDeletedOrThrow(id).restore();
    }

    private FormGroup findActiveOrThrow(String id) {
        return formGroupRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(FormModuleErrorCode.FORM_GROUP_NOT_FOUND,
                        "존재하지 않는 양식입니다: " + id));
    }

    private FormGroup findDeletedOrThrow(String id) {
        return formGroupRepository.findByIdAndDeletedYn(id, "Y")
                .orElseThrow(() -> new TenantException(FormModuleErrorCode.FORM_GROUP_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 양식입니다: " + id));
    }
}
