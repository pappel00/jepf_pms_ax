package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, String> {
    Optional<Project> findByIdAndDeletedYn(String id, String deletedYn);

    Optional<Project> findByProjectCodeAndDeletedYn(String projectCode, String deletedYn);

    boolean existsByCustomerIdAndDeletedYn(String customerId, String deletedYn);

    boolean existsByContactIdAndDeletedYn(String contactId, String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<Project> findAll();

    /**
     * 목록 검색. 조건이 없으면 빈 문자열('')을 넘긴다(PostgreSQL 은 null 파라미터의 타입을 추론하지 못하는 경우가 있어
     * null 대신 '' 비교로 처리). keyword 는 서비스에서 소문자로 바꿔서 넘긴다.
     */
    @Query("select p from Project p where p.deletedYn = 'N' "
            + "and (:status = '' or p.statusCode = :status) "
            + "and (:customerId = '' or p.customerId = :customerId) "
            + "and (:salesUserId = '' or p.salesUserId = :salesUserId) "
            + "and (:keyword = '' or lower(p.projectName) like concat('%', :keyword, '%') "
            + "     or lower(p.clientName) like concat('%', :keyword, '%') "
            + "     or lower(p.projectCode) like concat('%', :keyword, '%'))")
    Page<Project> search(@Param("status") String status,
                         @Param("customerId") String customerId,
                         @Param("salesUserId") String salesUserId,
                         @Param("keyword") String keyword,
                         Pageable pageable);
}