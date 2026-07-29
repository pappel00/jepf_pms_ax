package com.platform.module.tenant.repository;

import com.platform.module.tenant.domain.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * findAll 계열 메서드는 Hibernate @TenantId 에 의해 자동으로 현재 테넌트 데이터만 조회된다.
 * (별도로 tenantId 조건을 추가할 필요 없음)
 */
public interface DepartmentRepository extends JpaRepository<Department, String> {

    boolean existsByDepartmentCode(String departmentCode);

    Page<Department> findAllByDeletedYn(String deletedYn, Pageable pageable);
}
