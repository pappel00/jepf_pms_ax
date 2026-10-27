package com.platform.module.pms.repository;

import com.platform.module.pms.domain.UserProfile;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {
    Optional<UserProfile> findByIdAndDeletedYn(String id, String deletedYn);

    Optional<UserProfile> findByUserIdAndDeletedYn(String userId, String deletedYn);

    List<UserProfile> findAllByDeletedYn(String deletedYn);
}