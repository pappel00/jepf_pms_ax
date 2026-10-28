package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Transition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransitionRepository extends JpaRepository<Transition, String> {
    Optional<Transition> findByIdAndDeletedYn(String id, String deletedYn);

    boolean existsByProjectIdAndKindCodeAndStatusCodeAndDeletedYn(String projectId, String kindCode, String statusCode, String deletedYn);

    Optional<Transition> findFirstByProjectIdAndKindCodeAndStatusCodeAndDeletedYnOrderByRequestedAtDesc(
            String projectId, String kindCode, String statusCode, String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<Transition> findAll();

    /** 조건이 없으면 '' 를 넘긴다(PostgreSQL null 파라미터 타입 추론 문제 회피). 최신 요청이 먼저. */
    @Query("select t from Transition t where t.deletedYn = 'N' "
            + "and (:projectId = '' or t.projectId = :projectId) "
            + "and (:status = '' or t.statusCode = :status) "
            + "and (:kind = '' or t.kindCode = :kind) order by t.requestedAt desc")
    List<Transition> search(@Param("projectId") String projectId, @Param("status") String status, @Param("kind") String kind);
}