package com.platform.module.pms.repository;

import com.platform.module.pms.domain.TeamChange;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamChangeRepository extends JpaRepository<TeamChange, String> {
    Optional<TeamChange> findByIdAndDeletedYn(String id, String deletedYn);

    boolean existsByProjectIdAndStatusCodeAndDeletedYn(String projectId, String statusCode, String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<TeamChange> findAll();

    @Query("select c from TeamChange c where c.deletedYn = 'N' "
            + "and (:projectId = '' or c.projectId = :projectId) "
            + "and (:status = '' or c.statusCode = :status) order by c.requestedAt desc")
    List<TeamChange> search(@Param("projectId") String projectId, @Param("status") String status);
}