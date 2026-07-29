package com.platform.module.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.user.domain.Role;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.domain.UserGroupRole;
import com.platform.module.user.dto.RoleUserGroupDto;
import com.platform.module.user.repository.RoleRepository;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupRoleRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleUserGroupServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private UserGroupRoleRepository userGroupRoleRepository;

    private RoleUserGroupService service() {
        return new RoleUserGroupService(roleRepository, userGroupRepository, userGroupRoleRepository);
    }

    @Test
    void getUserGroupsThrowsWhenRoleNotFound() {
        when(roleRepository.findByIdAndDeletedYn("ROL_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getUserGroups("ROL_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getUserGroupsReturnsGroupsMappedToThisRole() {
        Role role = Role.create("CNTN_OWNR", "콘텐츠 담당", null, null, null);
        UserGroup group = UserGroup.create("CNTN_MGT", "콘텐츠 관리자", null, null, "N");
        when(roleRepository.findByIdAndDeletedYn(role.getId(), "N")).thenReturn(Optional.of(role));
        when(userGroupRoleRepository.findAllByRoleCode("CNTN_OWNR"))
                .thenReturn(List.of(UserGroupRole.create(group.getId(), "CNTN_OWNR")));
        when(userGroupRepository.findAllById(List.of(group.getId()))).thenReturn(List.of(group));

        var result = service().getUserGroups(role.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).groupCode()).isEqualTo("CNTN_MGT");
    }

    @Test
    void addUserGroupFailsWhenUserGroupNotFound() {
        Role role = Role.create("CNTN_OWNR", "콘텐츠 담당", null, null, null);
        when(roleRepository.findByIdAndDeletedYn(role.getId(), "N")).thenReturn(Optional.of(role));
        when(userGroupRepository.findByIdAndDeletedYn("UGR_unknown", "N")).thenReturn(Optional.empty());

        var request = new RoleUserGroupDto.CreateRequest("UGR_unknown");

        assertThatThrownBy(() -> service().addUserGroup(role.getId(), request))
                .isInstanceOf(TenantException.class);
    }
}
