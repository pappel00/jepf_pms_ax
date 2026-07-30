package com.platform.module.user.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.dto.UserGroupDto;
import com.platform.module.user.exception.UserModuleErrorCode;
import com.platform.module.user.repository.UserGroupRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;

    public UserGroupService(UserGroupRepository userGroupRepository) {
        this.userGroupRepository = userGroupRepository;
    }

    @Transactional
    public UserGroupDto.Response create(UserGroupDto.CreateRequest request) {
        if (userGroupRepository.existsByGroupCode(request.groupCode())) {
            throw new BusinessException(UserModuleErrorCode.USER_GROUP_CODE_DUPLICATED,
                    "이미 존재하는 사용자그룹 코드입니다: " + request.groupCode());
        }
        UserGroup userGroup = UserGroup.create(request.groupCode(), request.groupName(),
                request.category(), request.portalId(), request.defaultYn());
        return UserGroupDto.Response.from(userGroupRepository.saveAndFlush(userGroup));
    }

    public UserGroupDto.Response getById(String id) {
        return UserGroupDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<UserGroupDto.Response> getList(Pageable pageable) {
        Page<UserGroup> page = userGroupRepository.findAll(pageable);
        return new PagedResult<>(page.map(UserGroupDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public UserGroupDto.Response update(String id, UserGroupDto.UpdateRequest request) {
        UserGroup userGroup = findActiveOrThrow(id);
        userGroup.update(request.groupName(), request.category(), request.portalId());
        if ("Y".equals(request.useYn())) {
            userGroup.activate();
        } else {
            userGroup.deactivate();
        }
        return UserGroupDto.Response.from(userGroup);
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

    private UserGroup findActiveOrThrow(String id) {
        return userGroupRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_GROUP_NOT_FOUND,
                        "존재하지 않는 사용자그룹입니다: " + id));
    }

    private UserGroup findDeletedOrThrow(String id) {
        return userGroupRepository.findByIdAndDeletedYn(id, "Y")
                .orElseThrow(() -> new TenantException(UserModuleErrorCode.USER_GROUP_NOT_FOUND,
                        "삭제된 상태가 아니거나 존재하지 않는 사용자그룹입니다: " + id));
    }
}
