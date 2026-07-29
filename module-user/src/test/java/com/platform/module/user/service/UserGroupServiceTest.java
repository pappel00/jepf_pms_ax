package com.platform.module.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.user.domain.UserGroup;
import com.platform.module.user.dto.UserGroupDto;
import com.platform.module.user.repository.UserGroupRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserGroupServiceTest {

    @Mock
    private UserGroupRepository userGroupRepository;

    @Test
    void createFailsWhenGroupCodeDuplicated() {
        UserGroupService service = new UserGroupService(userGroupRepository);
        when(userGroupRepository.existsByGroupCode("CNTN_MGT")).thenReturn(true);

        var request = new UserGroupDto.CreateRequest("CNTN_MGT", "콘텐츠 관리자", "콘텐츠 관리", null, "N");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        UserGroupService service = new UserGroupService(userGroupRepository);
        when(userGroupRepository.findByIdAndDeletedYn("UGR_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("UGR_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        UserGroupService service = new UserGroupService(userGroupRepository);
        UserGroup group = UserGroup.create("CNTN_MGT", "콘텐츠 관리자", "콘텐츠 관리", null, "N");
        when(userGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));

        UserGroupDto.Response response = service.getById(group.getId());

        assertThat(response.groupCode()).isEqualTo("CNTN_MGT");
        assertThat(response.defaultYn()).isEqualTo("N");
    }
}
