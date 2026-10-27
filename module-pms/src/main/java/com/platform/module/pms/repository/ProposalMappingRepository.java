package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProposalMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalMappingRepository extends JpaRepository<ProposalMapping, String> {
    Optional<ProposalMapping> findByIdAndDeletedYn(String id, String deletedYn);
}
