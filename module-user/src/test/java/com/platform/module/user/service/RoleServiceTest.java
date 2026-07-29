package com.platform.module.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.user.domain.Role;
import com.platform.module.user.dto.RoleDto;
import com.platform.module.user.repository.RoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Test
    void createFailsWhenRoleCodeDuplicated() {
        RoleService service = new RoleService(roleRepository);
        when(roleRepository.existsByRoleCode("CNTN_OWNR")).thenReturn(true);

        var request = new RoleDto.CreateRequest("CNTN_OWNR", "콘텐츠 담당", "CNTN_OWNR", "콘텐츠 관리", "설명");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        RoleService service = new RoleService(roleRepository);
        when(roleRepository.findByIdAndDeletedYn("ROL_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("ROL_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        RoleService service = new RoleService(roleRepository);
        Role role = Role.create("CNTN_OWNR", "콘텐츠 담당", "CNTN_OWNR", "콘텐츠 관리", "설명");
        when(roleRepository.findByIdAndDeletedYn(role.getId(), "N")).thenReturn(Optional.of(role));

        RoleDto.Response response = service.getById(role.getId());

        assertThat(response.roleCode()).isEqualTo("CNTN_OWNR");
        assertThat(response.roleName()).isEqualTo("콘텐츠 담당");
    }
}
