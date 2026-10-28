package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Issue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IssueRepository extends JpaRepository<Issue, String> {
    Optional<Issue> findByIdAndDeletedYn(String id, String deletedYn);

    boolean existsByProjectIdAndDeletedYn(String projectId, String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<Issue> findAll();

    @Query("select i from Issue i where i.deletedYn = 'N' and (:projectId = '' or i.projectId = :projectId) order by i.issueCode")
    List<Issue> search(@Param("projectId") String projectId);
}
