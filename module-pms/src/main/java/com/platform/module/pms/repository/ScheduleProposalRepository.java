package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ScheduleProposal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleProposalRepository extends JpaRepository<ScheduleProposal, String> {
    Optional<ScheduleProposal> findByIdAndDeletedYn(String id, String deletedYn);

    boolean existsByIssueIdAndStatusCodeAndDeletedYn(String issueId, String statusCode, String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<ScheduleProposal> findAll();

    @Query("select p from ScheduleProposal p where p.deletedYn = 'N' and (:projectId = '' or p.projectId = :projectId) "
            + "order by p.createdDate desc")
    List<ScheduleProposal> search(@Param("projectId") String projectId);
}
