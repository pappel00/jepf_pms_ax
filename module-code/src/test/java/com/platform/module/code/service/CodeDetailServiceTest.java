package com.platform.module.code.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.code.domain.CodeDetail;
import com.platform.module.code.domain.CodeGroup;
import com.platform.module.code.dto.CodeDetailDto;
import com.platform.module.code.repository.CodeDetailRepository;
import com.platform.module.code.repository.CodeGroupRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CodeDetailServiceTest {

    @Mock
    private CodeDetailRepository codeDetailRepository;

    @Mock
    private CodeGroupRepository codeGroupRepository;

    private CodeDetailService service() {
        return new CodeDetailService(codeDetailRepository, codeGroupRepository);
    }

    @Test
    void createFailsWhenGroupNotFound() {
        when(codeGroupRepository.findByIdAndDeletedYn("CGR_unknown", "N")).thenReturn(Optional.empty());

        var request = new CodeDetailDto.CreateRequest("PC", "PC", 1, null);

        assertThatThrownBy(() -> service().create("CGR_unknown", request))
                .isInstanceOf(TenantException.class);
    }

    @Test
    void createFailsWhenCodeDuplicated() {
        CodeGroup group = CodeGroup.create("DEVICE_TYPE", "디바이스 유형", null);
        when(codeGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(codeDetailRepository.existsByGroupIdAndCode(group.getId(), "PC")).thenReturn(true);

        var request = new CodeDetailDto.CreateRequest("PC", "PC", 1, null);

        assertThatThrownBy(() -> service().create(group.getId(), request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createSucceedsWhenSortOrderIsNull() {
        CodeGroup group = CodeGroup.create("DEVICE_TYPE", "디바이스 유형", null);
        when(codeGroupRepository.findByIdAndDeletedYn(group.getId(), "N")).thenReturn(Optional.of(group));
        when(codeDetailRepository.existsByGroupIdAndCode(group.getId(), "PC")).thenReturn(false);

        var request = new CodeDetailDto.CreateRequest("PC", "PC", null, null);
        CodeDetailDto.Response response = service().create(group.getId(), request);

        assertThat(response.sortOrder()).isEqualTo(0);
        assertThat(response.code()).isEqualTo("PC");
    }
}
