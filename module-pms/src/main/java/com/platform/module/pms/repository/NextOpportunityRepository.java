package com.platform.module.pms.repository;

import com.platform.module.pms.domain.NextOpportunity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NextOpportunityRepository extends JpaRepository<NextOpportunity, String> {
    Optional<NextOpportunity> findByIdAndDeletedYn(String id, String deletedYn);
}
