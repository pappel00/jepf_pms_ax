package com.platform.module.pms.repository;

import com.platform.module.pms.domain.OpsOpportunity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsOpportunityRepository extends JpaRepository<OpsOpportunity, String> {
    Optional<OpsOpportunity> findByIdAndDeletedYn(String id, String deletedYn);

    List<OpsOpportunity> findAllByDeletedYnOrderByOppCodeAsc(String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<OpsOpportunity> findAll();
}
