package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProposalVersion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalVersionRepository extends JpaRepository<ProposalVersion, String> {
    Optional<ProposalVersion> findByIdAndDeletedYn(String id, String deletedYn);

    List<ProposalVersion> findAllByProposalIdInAndDeletedYnOrderByCreatedDateAsc(Collection<String> proposalIds, String deletedYn);

    List<ProposalVersion> findAllByProposalIdAndDeletedYnOrderByCreatedDateAsc(String proposalId, String deletedYn);
}
