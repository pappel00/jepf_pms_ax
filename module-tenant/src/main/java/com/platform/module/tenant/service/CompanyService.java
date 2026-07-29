package com.platform.module.tenant.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.framework.core.common.exception.TenantException;
import com.platform.framework.core.common.response.PageInfo;
import com.platform.framework.core.common.response.PagedResult;
import com.platform.module.tenant.domain.Company;
import com.platform.module.tenant.dto.CompanyDto;
import com.platform.module.tenant.event.TenantCreatedEvent;
import com.platform.module.tenant.exception.TenantModuleErrorCode;
import com.platform.module.tenant.repository.CompanyRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CompanyService(CompanyRepository companyRepository, ApplicationEventPublisher eventPublisher) {
        this.companyRepository = companyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CompanyDto.Response create(CompanyDto.CreateRequest request) {
        if (companyRepository.existsByCompanyCode(request.companyCode())) {
            throw new BusinessException(TenantModuleErrorCode.COMPANY_CODE_DUPLICATED,
                    "이미 존재하는 회사 코드입니다: " + request.companyCode());
        }
        Company company = Company.create(request.companyCode(), request.companyName(), request.description());
        Company saved = companyRepository.save(company);
        // 이 회사에 소속된 기초 데이터(Role 마스터, 기본 UserGroup 등)를 module-user 쪽에서
        // 부트스트랩하도록 알린다. TenantBootstrapListener 참고.
        eventPublisher.publishEvent(new TenantCreatedEvent(saved.getId(), saved.getCompanyCode()));
        return CompanyDto.Response.from(saved);
    }

    public CompanyDto.Response getById(String id) {
        return CompanyDto.Response.from(findActiveOrThrow(id));
    }

    /**
     * 삭제(delete())는 실제 행 삭제가 아니라 deletedYn="Y" 로 바꾸는 소프트 삭제다.
     * 관리자 화면에서는 삭제된 데이터도 목록에 보이고 그 상태(deletedYn)를 확인할 수 있어야
     * 하므로, 여기서는 deletedYn으로 거르지 않고 전체를 조회한다 (findAllByDeletedYn("N",...)
     * 대신 JpaRepository 기본 findAll 사용). Response.deletedYn 필드로 프론트에 상태를 넘긴다.
     */
    public PagedResult<CompanyDto.Response> getList(Pageable pageable) {
        Page<Company> page = companyRepository.findAll(pageable);
        return new PagedResult<>(page.map(CompanyDto.Response::from).getContent(), PageInfo.of(page));
    }

    @Transactional
    public CompanyDto.Response update(String id, CompanyDto.UpdateRequest request) {
        Company company = findActiveOrThrow(id);
        company.update(request.companyName(), request.description());
        applyUseYn(company, request.useYn());
        return CompanyDto.Response.from(company);
    }

    private void applyUseYn(Company company, String useYn) {
        if ("Y".equals(useYn)) {
            company.activate();
        } else {
            company.deactivate();
        }
    }

    @Transactional
    public void delete(String id) {
        findActiveOrThrow(id).markDeleted();
    }

    private Company findActiveOrThrow(String id) {
        return companyRepository.findByIdAndDeletedYn(id, "N")
                .orElseThrow(() -> new TenantException(TenantModuleErrorCode.COMPANY_NOT_FOUND,
                        "존재하지 않는 회사입니다: " + id));
    }
}
