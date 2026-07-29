package com.platform.module.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.InvalidRequestException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.dto.UserGroupRoleDto;
import com.platform.module.user.repository.RoleRepository;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupRoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserGroupRoleServiceTest {

    @Mock
    private UserGroupRoleRepository userGroupRoleRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private RoleRepository roleRepository;

    @Test
    void createFailsWhenUserGroupNotFound() {
        UserGroupRoleService service = new UserGroupRoleService(userGroupRoleRepository, userGroupRepository, roleRepository);
        when(userGroupRepository.findByIdAndDeletedYn("UGR_unknown", "N")).thenReturn(Optional.empty());

        var request = new UserGroupRoleDto.CreateRequest("ROLE_ADMIN");

        assertThatThrownBy(() -> service.create("UGR_unknown", request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void createSucceedsWhenRoleCodeIsSystemRole() {
        UserGroupRoleService service = new UserGroupRoleService(userGroupRoleRepository, userGroupRepository, roleRepository);
        UserGroup group = UserGroup.create("ADMIN_GROUP", "관리자", null, null, "N");
        when(userGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(userGroupRoleRepository.existsByUserGroupIdAndRoleCode(group.getId(), "ROLE_ADMIN")).thenReturn(false);

        var request = new UserGroupRoleDto.CreateRequest("ROLE_ADMIN");
        UserGroupRoleDto.Response response = service.create(group.getId(), request);

        assertThat(response.roleCode()).isEqualTo("ROLE_ADMIN");
        // SystemRoles라 roleRepository는 아예 호출되지 않아야 한다.
        org.mockito.Mockito.verifyNoInteractions(roleRepository);
    }

    @Test
    void createFailsWhenCustomRoleNotInRoleMaster() {
        UserGroupRoleService service = new UserGroupRoleService(userGroupRoleRepository, userGroupRepository, roleRepository);
        UserGroup group = UserGroup.create("CNTN_MGT", "콘텐츠 관리자", null, null, "N");
        when(userGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(roleRepository.existsByRoleCodeAndDeletedYn("NOT_A_REAL_ROLE", "N")).thenReturn(false);

        var request = new UserGroupRoleDto.CreateRequest("NOT_A_REAL_ROLE");

        assertThatThrownBy(() -> service.create(group.getId(), request))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void createFailsWhenMappingDuplicated() {
        UserGroupRoleService service = new UserGroupRoleService(userGroupRoleRepository, userGroupRepository, roleRepository);
        UserGroup group = UserGroup.create("ADMIN_GROUP", "관리자", null, null, "N");
        when(userGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(userGroupRoleRepository.existsByUserGroupIdAndRoleCode(group.getId(), "ROLE_ADMIN")).thenReturn(true);

        var request = new UserGroupRoleDto.CreateRequest("ROLE_ADMIN");

        assertThatThrownBy(() -> service.create(group.getId(), request))
                .isInstanceOf(BusinessException.class);
    }
}
