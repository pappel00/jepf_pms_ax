package com.platform.module.tenant.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.tenant.domain.Department;
import com.platform.module.tenant.dto.DepartmentDto;
import com.platform.module.tenant.exception.TenantModuleErrorCode;
import com.platform.module.tenant.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이 서비스의 모든 조회/저장은 Hibernate @TenantId 에 의해 자동으로
 * 현재 로그인한 사용자의 테넌트(TenantContext)로 범위가 제한된다.
 */
@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public DepartmentDto.Response create(DepartmentDto.CreateRequest request) {
        if (departmentRepository.existsByDepartmentCode(request.departmentCode())) {
            throw new BusinessException(TenantModuleErrorCode.DEPARTMENT_CODE_DUPLICATED,
                    "이미 존재하는 부서 코드입니다: " + request.departmentCode());
        }
        Department department = Department.create(
                request.departmentCode(), request.departmentName(), request.parentDepartmentId());
        // save()만 호출하면 INSERT가 트랜잭션 커밋 시점까지 지연될 수 있어, Hibernate @TenantId 가
        // 채워주는 tenantId 값이 이 메서드가 반환하는 응답 DTO에는 아직 null로 보일 수 있다.
        // saveAndFlush() 로 즉시 INSERT를 실행시켜 생성된 값이 바로 응답에 반영되도록 한다.
        return DepartmentDto.Response.from(departmentRepository.saveAndFlush(department));
    }

    public DepartmentDto.Response getById(String id) {
        return DepartmentDto.Response.from(findActiveOrThrow(id));
    }

    public PagedResult<DepartmentDto.Response> getList(Pageable pageable) {
        Page<Department> page = departmentRepository.findAll(pageable);
        return new PagedResult<>(page.map(DepartmentDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public DepartmentDto.Response update(String id, DepartmentDto.UpdateRequest request) {
        Department department = findActiveOrThrow(id);
        department.update(request.departmentName(), request.parentDepartmentId());
        if ("Y".equals(request.useYn())) {
            department.activate();
        } else {
            department.deactivate();
        }
        return DepartmentDto.Response.from(department);
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private Department findActiveOrThrow(String id) {
        return departmentRepository.findById(id)
                .filter(department -> "N".equals(department.getDeletedYn()))
                .orElseThrow(() -> new TenantException(TenantModuleErrorCode.DEPARTMENT_NOT_FOUND,
                        "존재하지 않는 부서입니다: " + id));
    }
}
