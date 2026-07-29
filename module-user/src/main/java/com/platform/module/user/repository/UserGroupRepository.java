package com.platform.module.user.repository;

import com.platform.module.user.domain.UserGroup;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupRepository extends JpaRepository<UserGroup, String> {
    boolean existsByGroupCode(String groupCode);
    Optional<UserGroup> findByIdAndDeletedYn(String id, String deletedYn);
    Optional<UserGroup> findByGroupCodeAndDeletedYn(String groupCode, String deletedYn);
    Page<UserGroup> findAllByDeletedYn(String deletedYn, Pageable pageable);

    /** 신규 가입 시 자동 배정할 기본권한 그룹을 찾는 용도. 테넌트당 하나만 있는 것을 전제로 한다. */
    Optional<UserGroup> findFirstByDefaultYnAndDeletedYn(String defaultYn, String deletedYn);
}
