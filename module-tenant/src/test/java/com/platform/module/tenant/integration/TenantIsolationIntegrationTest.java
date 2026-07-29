package com.platform.module.tenant.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.platform.framework.security.tenant.TenantContext;
import com.platform.module.tenant.TenantModuleTestApplication;
import com.platform.module.tenant.domain.Department;
import com.platform.module.tenant.repository.DepartmentRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 공유 스키마 + Hibernate @TenantId 방식 멀티테넌시가 실제로 격리되는지 검증하는 통합 테스트 (H2).
 *
 * 주의: 이 테스트는 의도적으로 @Transactional 을 사용하지 않는다.
 * Hibernate의 CurrentTenantIdentifierResolver는 "Session이 열릴 때" 한 번만 호출되어 그 Session의
 * 생명주기 동안 테넌트가 고정된다. 테스트 메서드 전체를 하나의 트랜잭션(=하나의 Session)으로 묶으면
 * 중간에 TenantContext를 바꿔도 반영되지 않는다. 그래서 각 repository 호출이 Spring Data JPA의
 * 기본 동작대로 자신만의 트랜잭션(=새 Session)을 여는 것을 이용해, 호출 사이사이에 테넌트를 바꾼다.
 */
@SpringBootTest(classes = TenantModuleTestApplication.class)
class TenantIsolationIntegrationTest {

    @Autowired
    private DepartmentRepository departmentRepository;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void departmentsAreIsolatedPerTenant() {
        TenantContext.setTenantId("TNT_A");
        departmentRepository.save(Department.create("DEV", "개발팀 A", null));

        TenantContext.setTenantId("TNT_B");
        departmentRepository.save(Department.create("DEV", "개발팀 B", null));

        TenantContext.setTenantId("TNT_A");
        List<Department> tenantADepartments = departmentRepository.findAll();

        TenantContext.setTenantId("TNT_B");
        List<Department> tenantBDepartments = departmentRepository.findAll();

        assertThat(tenantADepartments).hasSize(1);
        assertThat(tenantADepartments.get(0).getDepartmentName()).isEqualTo("개발팀 A");
        assertThat(tenantADepartments.get(0).getTenantId()).isEqualTo("TNT_A");

        assertThat(tenantBDepartments).hasSize(1);
        assertThat(tenantBDepartments.get(0).getDepartmentName()).isEqualTo("개발팀 B");
        assertThat(tenantBDepartments.get(0).getTenantId()).isEqualTo("TNT_B");
    }
}
