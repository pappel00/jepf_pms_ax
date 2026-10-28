package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProposalChecklist;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalChecklistRepository extends JpaRepository<ProposalChecklist, String> {
    Optional<ProposalChecklist> findByIdAndDeletedYn(String id, String deletedYn);

    List<ProposalChecklist> findAllByProposalIdInAndDeletedYnOrderBySortOrderAscCreatedDateAsc(Collection<String> proposalIds, String deletedYn);

    List<ProposalChecklist> findAllByProposalIdAndDeletedYn(String proposalId, String deletedYn);
}
