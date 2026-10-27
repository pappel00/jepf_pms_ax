package com.platform.module.pms.repository;

import com.platform.module.pms.domain.OpsOpportunity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsOpportunityRepository extends JpaRepository<OpsOpportunity, String> {
    Optional<OpsOpportunity> findByIdAndDeletedYn(String id, String deletedYn);
}
