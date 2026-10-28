package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProposalMapping;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalMappingRepository extends JpaRepository<ProposalMapping, String> {
    Optional<ProposalMapping> findByIdAndDeletedYn(String id, String deletedYn);

    List<ProposalMapping> findAllByProposalIdInAndDeletedYn(Collection<String> proposalIds, String deletedYn);

    List<ProposalMapping> findAllByProposalIdAndDeletedYn(String proposalId, String deletedYn);

    List<ProposalMapping> findAllByRfpItemIdInAndDeletedYn(Collection<String> rfpItemIds, String deletedYn);

    Optional<ProposalMapping> findByProposalIdAndRfpItemIdAndDeletedYn(String proposalId, String rfpItemId, String deletedYn);
}
