package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ScheduleProposalItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleProposalItemRepository extends JpaRepository<ScheduleProposalItem, String> {
    Optional<ScheduleProposalItem> findByIdAndDeletedYn(String id, String deletedYn);

    List<ScheduleProposalItem> findAllByProposalIdInAndDeletedYnOrderByCreatedDateAsc(Collection<String> proposalIds, String deletedYn);
}
