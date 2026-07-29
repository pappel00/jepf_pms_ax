package com.platform.module.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.platform.module.user.domain.UserGroupRole;
import com.platform.module.user.domain.UserGroupUser;
import com.platform.module.user.repository.UserGroupRoleRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EffectiveRoleServiceTest {

    @Mock
    private UserGroupUserRepository userGroupUserRepository;

    @Mock
    private UserGroupRoleRepository userGroupRoleRepository;

    @Test
    void resolveRolesReturnsEmptyWhenUserBelongsToNoGroup() {
        EffectiveRoleService service = new EffectiveRoleService(userGroupUserRepository, userGroupRoleRepository);
        when(userGroupUserRepository.findAllByUserId("USR_1")).thenReturn(List.of());

        assertThat(service.resolveRoles("USR_1")).isEmpty();
    }

    @Test
    void resolveRolesMergesRolesFromAllGroupsWithoutDuplicates() {
        EffectiveRoleService service = new EffectiveRoleService(userGroupUserRepository, userGroupRoleRepository);

        UserGroupUser membership1 = UserGroupUser.create("UGR_1", "USR_1");
        UserGroupUser membership2 = UserGroupUser.create("UGR_2", "USR_1");
        when(userGroupUserRepository.findAllByUserId("USR_1")).thenReturn(List.of(membership1, membership2));

        when(userGroupRoleRepository.findAllByUserGroupIdIn(List.of("UGR_1", "UGR_2"))).thenReturn(List.of(
                UserGroupRole.create("UGR_1", "ROLE_USER"),
                UserGroupRole.create("UGR_2", "ROLE_USER"),
                UserGroupRole.create("UGR_2", "ROLE_ADMIN")
        ));

        List<String> roles = service.resolveRoles("USR_1");

        assertThat(roles).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(roles).hasSize(2);
    }
}
