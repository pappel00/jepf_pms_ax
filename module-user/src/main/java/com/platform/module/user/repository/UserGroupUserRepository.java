package com.platform.module.user.repository;

import com.platform.module.user.domain.UserGroupUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGroupUserRepository extends JpaRepository<UserGroupUser, String> {
    boolean existsByUserGroupIdAndUserId(String userGroupId, String userId);
    List<UserGroupUser> findAllByUserGroupId(String userGroupId);
    List<UserGroupUser> findAllByUserId(String userId);
    Optional<UserGroupUser> findByIdAndUserGroupId(String id, String userGroupId);

    /** 프론트엔드는 목록 조회에서 User 정보를 받으므로(매핑 자체의 PK가 아니라), userId로 직접 삭제한다. */
    Optional<UserGroupUser> findByUserGroupIdAndUserId(String userGroupId, String userId);
}
