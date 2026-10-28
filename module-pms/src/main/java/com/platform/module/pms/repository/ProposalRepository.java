package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Proposal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProposalRepository extends JpaRepository<Proposal, String> {
    Optional<Proposal> findByIdAndDeletedYn(String id, String deletedYn);

    List<Proposal> findAllByDeletedYnOrderByProposalCodeAsc(String deletedYn);

    boolean existsByRfpIdAndDeletedYn(String rfpId, String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<Proposal> findAll();
}
