package com.platform.module.privacy.repository;

import com.platform.module.privacy.domain.PrivacyPolicyVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyPolicyVersionRepository extends JpaRepository<PrivacyPolicyVersion, String> {
    List<PrivacyPolicyVersion> findAllByGroupIdOrderByEffectiveDateDesc(String groupId);
    Optional<PrivacyPolicyVersion> findByIdAndGroupId(String id, String groupId);
}
