package com.platform.module.portal.repository;

import com.platform.module.portal.domain.Application;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, String> {
    boolean existsByApplicationCode(String applicationCode);
    Optional<Application> findByIdAndDeletedYn(String id, String deletedYn);
    Page<Application> findAllByDeletedYn(String deletedYn, Pageable pageable);
    Page<Application> findAllByPortalIdAndDeletedYn(String portalId, String deletedYn, Pageable pageable);

    /** 다른 모듈이 부트스트랩 시 특정 어플리케이션이 이미 있는지 찾아볼 때 사용 (find-or-create). */
    Optional<Application> findByApplicationCodeAndDeletedYn(String applicationCode, String deletedYn);

    /** 관리자 목록 화면은 삭제된 항목도 상태(deletedYn)와 함께 보여줘야 하므로 필터링하지 않는다. */
    Page<Application> findAllByPortalId(String portalId, Pageable pageable);

    /** 운영 화면 셸(어플리케이션 선택 UI)의 accessibleOnly 목록용 — 삭제 제외, sortOrder 순. */
    List<Application> findAllByPortalIdAndDeletedYnOrderBySortOrderAsc(String portalId, String deletedYn);
    List<Application> findAllByDeletedYnOrderBySortOrderAsc(String deletedYn);
}
