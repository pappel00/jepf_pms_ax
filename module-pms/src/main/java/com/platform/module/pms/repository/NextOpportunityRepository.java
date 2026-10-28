package com.platform.module.pms.repository;

import com.platform.module.pms.domain.NextOpportunity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NextOpportunityRepository extends JpaRepository<NextOpportunity, String> {
    Optional<NextOpportunity> findByIdAndDeletedYn(String id, String deletedYn);

    List<NextOpportunity> findAllByDeletedYnOrderByOppCodeAsc(String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<NextOpportunity> findAll();

    List<NextOpportunity> findAllByProjectIdAndDeletedYn(String projectId, String deletedYn);
}
