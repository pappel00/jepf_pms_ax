package com.platform.module.tenant.repository;

import com.platform.module.tenant.domain.Company;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, String> {

    boolean existsByCompanyCode(String companyCode);

    Optional<Company> findByIdAndDeletedYn(String id, String deletedYn);

    /** module-user 의 로그인/회원가입에서 companyCode -> tenantId 해석에 사용된다. */
    Optional<Company> findByCompanyCodeAndDeletedYn(String companyCode, String deletedYn);

    Page<Company> findAllByDeletedYn(String deletedYn, Pageable pageable);
}
