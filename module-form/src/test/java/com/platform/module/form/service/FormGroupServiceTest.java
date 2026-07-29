package com.platform.module.form.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.form.domain.FormGroup;
import com.platform.module.form.dto.FormGroupDto;
import com.platform.module.form.repository.FormGroupRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FormGroupServiceTest {

    @Mock
    private FormGroupRepository formGroupRepository;

    @Test
    void createFailsWhenFormCodeDuplicatedWithinSamePortal() {
        FormGroupService service = new FormGroupService(formGroupRepository);
        when(formGroupRepository.existsByPortalIdAndFormCode("PRT_1", "LEAVE_REQUEST")).thenReturn(true);

        var request = new FormGroupDto.CreateRequest("PRT_1", "LEAVE_REQUEST", "휴가신청서", "인사", null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createSucceedsWhenSameFormCodeUsedByDifferentPortal() {
        FormGroupService service = new FormGroupService(formGroupRepository);
        when(formGroupRepository.existsByPortalIdAndFormCode("PRT_2", "LEAVE_REQUEST")).thenReturn(false);
        when(formGroupRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(FormGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new FormGroupDto.CreateRequest("PRT_2", "LEAVE_REQUEST", "휴가신청서", "인사", null);
        FormGroupDto.Response response = service.create(request);

        assertThat(response.portalId()).isEqualTo("PRT_2");
        assertThat(response.formCode()).isEqualTo("LEAVE_REQUEST");
        assertThat(response.category()).isEqualTo("인사");
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        FormGroupService service = new FormGroupService(formGroupRepository);
        when(formGroupRepository.findByIdAndDeletedYn("FMG_unknown", "N")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("FMG_unknown"))
                .isInstanceOf(TenantException.class);
    }
}
