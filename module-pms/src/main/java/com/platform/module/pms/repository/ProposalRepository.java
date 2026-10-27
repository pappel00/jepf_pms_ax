package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Proposal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, String> {
    Optional<Proposal> findByIdAndDeletedYn(String id, String deletedYn);
}
