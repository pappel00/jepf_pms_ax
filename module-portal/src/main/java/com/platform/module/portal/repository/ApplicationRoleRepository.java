package com.platform.module.portal.repository;

import com.platform.module.portal.domain.ApplicationRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRoleRepository extends JpaRepository<ApplicationRole, String> {
    boolean existsByApplicationIdAndRoleCode(String applicationId, String roleCode);
    List<ApplicationRole> findAllByApplicationId(String applicationId);
    Optional<ApplicationRole> findByIdAndApplicationId(String id, String applicationId);
}
