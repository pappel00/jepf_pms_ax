package com.platform.module.form.repository;

import com.platform.module.form.domain.FormVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormVersionRepository extends JpaRepository<FormVersion, String> {
    List<FormVersion> findAllByGroupIdOrderByEffectiveDateDesc(String groupId);
    Optional<FormVersion> findByIdAndGroupId(String id, String groupId);
}
