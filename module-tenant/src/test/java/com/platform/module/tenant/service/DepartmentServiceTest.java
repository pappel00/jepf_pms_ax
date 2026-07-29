package com.platform.module.tenant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.module.tenant.dto.DepartmentDto;
import com.platform.module.tenant.repository.DepartmentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Test
    void createFailsWhenDepartmentCodeDuplicated() {
        DepartmentService service = new DepartmentService(departmentRepository);
        when(departmentRepository.existsByDepartmentCode("DEV")).thenReturn(true);

        var request = new DepartmentDto.CreateRequest("DEV", "개발팀", null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        DepartmentService service = new DepartmentService(departmentRepository);
        when(departmentRepository.findById("DEPT_unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById("DEPT_unknown"))
                .isInstanceOf(TenantException.class);
    }
}
