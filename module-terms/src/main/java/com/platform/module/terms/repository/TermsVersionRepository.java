package com.platform.module.terms.repository;

import com.platform.module.terms.domain.TermsVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsVersionRepository extends JpaRepository<TermsVersion, String> {
    List<TermsVersion> findAllByGroupIdOrderByEffectiveDateDesc(String groupId);
    Optional<TermsVersion> findByIdAndGroupId(String id, String groupId);
}
