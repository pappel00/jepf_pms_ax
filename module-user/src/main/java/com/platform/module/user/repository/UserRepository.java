package com.platform.module.user.repository;

import com.platform.module.user.domain.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 여기 정의된 조회 메서드들은 Hibernate @TenantId 에 의해 자동으로 현재 테넌트로 범위가 제한된다.
 */
public interface UserRepository extends JpaRepository<User, String> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginIdAndDeletedYn(String loginId, String deletedYn);

    Page<User> findAllByDeletedYn(String deletedYn, Pageable pageable);
}
