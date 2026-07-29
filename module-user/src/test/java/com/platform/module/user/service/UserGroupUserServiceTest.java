package com.platform.module.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.user.domain.User;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.domain.UserGroupUser;
import com.platform.module.user.dto.UserGroupUserDto;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import com.platform.module.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserGroupUserServiceTest {

    @Mock
    private UserGroupUserRepository userGroupUserRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EffectiveRoleService effectiveRoleService;

    private UserGroupUserService service() {
        return new UserGroupUserService(userGroupUserRepository, userGroupRepository, userRepository, effectiveRoleService);
    }

    @Test
    void addUserFailsWhenUserGroupNotFound() {
        when(userGroupRepository.findByIdAndDeletedYn("UGR_unknown", "N")).thenReturn(Optional.empty());

        var request = new UserGroupUserDto.AssignRequest("USR_1");

        assertThatThrownBy(() -> service().addUser("UGR_unknown", request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void addUserFailsWhenUserNotFound() {
        UserGroup group = UserGroup.create("ADMIN_GROUP", "관리자", null, null, "N");
        when(userGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(userRepository.findById("USR_unknown")).thenReturn(Optional.empty());

        var request = new UserGroupUserDto.AssignRequest("USR_unknown");

        assertThatThrownBy(() -> service().addUser(group.getId(), request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void addUserFailsWhenAlreadyInGroup() {
        UserGroup group = UserGroup.create("ADMIN_GROUP", "관리자", null, null, "N");
        User user = User.create("bob", "hashed", "bob@a.com", "Bob");
        when(userGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userGroupUserRepository.existsByUserGroupIdAndUserId(group.getId(), user.getId())).thenReturn(true);

        var request = new UserGroupUserDto.AssignRequest(user.getId());

        assertThatThrownBy(() -> service().addUser(group.getId(), request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void addUserSucceedsAndReturnsEffectiveRoles() {
        UserGroup group = UserGroup.create("ADMIN_GROUP", "관리자", null, null, "N");
        User user = User.create("bob", "hashed", "bob@a.com", "Bob");
        when(userGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userGroupUserRepository.existsByUserGroupIdAndUserId(group.getId(), user.getId())).thenReturn(false);
        when(effectiveRoleService.resolveRoles(user.getId())).thenReturn(List.of("ROLE_ADMIN"));

        var request = new UserGroupUserDto.AssignRequest(user.getId());
        var response = service().addUser(group.getId(), request);

        assertThat(response.loginId()).isEqualTo("bob");
        assertThat(response.roles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void removeUserThrowsWhenMappingNotFound() {
        when(userGroupUserRepository.findByUserGroupIdAndUserId("UGR_1", "USR_unknown"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().removeUser("UGR_1", "USR_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void removeUserDeletesMappingFoundByUserId() {
        UserGroupUser mapping = UserGroupUser.create("UGR_1", "USR_1");
        when(userGroupUserRepository.findByUserGroupIdAndUserId("UGR_1", "USR_1"))
                .thenReturn(Optional.of(mapping));

        service().removeUser("UGR_1", "USR_1");

        org.mockito.Mockito.verify(userGroupUserRepository).delete(mapping);
    }
}
