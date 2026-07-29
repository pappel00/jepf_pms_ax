package com.platform.module.user.repository;

import com.platform.module.user.domain.Role;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** 여기 정의된 조회 메서드들은 Hibernate @TenantId 에 의해 자동으로 현재 테넌트로 범위가 제한된다. */
public interface RoleRepository extends JpaRepository<Role, String> {

    boolean existsByRoleCode(String roleCode);

    /** 사용자에게 역할을 부여할 때, 삭제되지 않은 유효한 Role인지 검증하는 용도. */
    boolean existsByRoleCodeAndDeletedYn(String roleCode, String deletedYn);

    Optional<Role> findByIdAndDeletedYn(String id, String deletedYn);

    Page<Role> findAllByDeletedYn(String deletedYn, Pageable pageable);
}
