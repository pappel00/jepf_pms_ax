package com.platform.module.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.user.domain.User;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.dto.UserDto;
import com.platform.module.user.repository.UserGroupRepository;
import com.platform.module.user.repository.UserGroupUserRepository;
import com.platform.module.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGroupRepository userGroupRepository;

    @Mock
    private UserGroupUserRepository userGroupUserRepository;

    @Mock
    private EffectiveRoleService effectiveRoleService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private UserService service() {
        return new UserService(userRepository, userGroupRepository, userGroupUserRepository,
                effectiveRoleService, passwordEncoder);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(userRepository.findById("USR_unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getById("USR_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWithEffectiveRoles() {
        User user = User.create("admin", "hashed", "a@a.com", "관리자");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(effectiveRoleService.resolveRoles(user.getId())).thenReturn(List.of("ROLE_USER", "ROLE_ADMIN"));

        UserDto.Response response = service().getById(user.getId());

        assertThat(response.loginId()).isEqualTo("admin");
        assertThat(response.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void createFailsWhenLoginIdDuplicated() {
        when(userRepository.existsByLoginId("admin")).thenReturn(true);

        var request = new UserDto.CreateRequest("admin", "password1234", "a@a.com", "관리자", null);

        assertThatThrownBy(() -> service().create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createAutoAssignsDefaultGroupWhenOneExists() {
        when(userRepository.existsByLoginId("bob")).thenReturn(false);
        when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserGroup defaultGroup = UserGroup.create("DEFAULT_USER", "기본권한 사용자", null, null, "Y");
        when(userGroupRepository.findFirstByDefaultYnAndDeletedYn("Y", "N")).thenReturn(Optional.of(defaultGroup));
        when(effectiveRoleService.resolveRoles(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());

        var request = new UserDto.CreateRequest("bob", "password1234", "bob@a.com", "Bob", null);
        UserDto.Response response = service().create(request);

        assertThat(response.loginId()).isEqualTo("bob");
        org.mockito.Mockito.verify(userGroupUserRepository)
                .saveAndFlush(org.mockito.ArgumentMatchers.any(com.platform.module.user.domain.UserGroupUser.class));
    }

    @Test
    void createStoresDepartmentIdWhenProvided() {
        when(userRepository.existsByLoginId("carol")).thenReturn(false);
        when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userGroupRepository.findFirstByDefaultYnAndDeletedYn("Y", "N")).thenReturn(Optional.empty());
        when(effectiveRoleService.resolveRoles(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());

        var request = new UserDto.CreateRequest("carol", "password1234", "carol@a.com", "Carol", "DEPT_1");
        UserDto.Response response = service().create(request);

        assertThat(response.departmentId()).isEqualTo("DEPT_1");
    }

    @Test
    void updateAssignsDepartment() {
        User user = User.create("dave", "hashed", "dave@a.com", "Dave");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(effectiveRoleService.resolveRoles(user.getId())).thenReturn(List.of());

        var request = new UserDto.UpdateRequest("dave@a.com", "Dave", "DEPT_2", "Y");
        UserDto.Response response = service().update(user.getId(), request);

        assertThat(response.departmentId()).isEqualTo("DEPT_2");
    }
}
