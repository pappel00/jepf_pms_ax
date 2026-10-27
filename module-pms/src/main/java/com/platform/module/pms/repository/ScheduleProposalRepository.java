package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ScheduleProposal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleProposalRepository extends JpaRepository<ScheduleProposal, String> {
    Optional<ScheduleProposal> findByIdAndDeletedYn(String id, String deletedYn);
}
