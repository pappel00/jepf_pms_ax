package com.platform.module.code.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.code.domain.CodeGroup;
import com.platform.module.code.dto.CodeGroupDto;
import com.platform.module.code.repository.CodeGroupRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodeGroupServiceTest {

    @Mock
    private CodeGroupRepository codeGroupRepository;

    @Test
    void createFailsWhenGroupCodeDuplicated() {
        CodeGroupService service = new CodeGroupService(codeGroupRepository);
        when(codeGroupRepository.existsByGroupCode("DEVICE_TYPE")).thenReturn(true);

        var request = new CodeGroupDto.CreateRequest("DEVICE_TYPE", "디바이스 유형", null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        CodeGroupService service = new CodeGroupService(codeGroupRepository);
        when(codeGroupRepository.findByIdAndDeletedYn("CGR_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("CGR_unknown"))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void getByIdReturnsResponseWhenFound() {
        CodeGroupService service = new CodeGroupService(codeGroupRepository);
        CodeGroup group = CodeGroup.create("DEVICE_TYPE", "디바이스 유형", "설명");
        when(codeGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));

        CodeGroupDto.Response response = service.getById(group.getId());

        assertThat(response.groupCode()).isEqualTo("DEVICE_TYPE");
    }
}
