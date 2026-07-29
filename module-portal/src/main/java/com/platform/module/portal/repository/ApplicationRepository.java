package com.platform.module.portal.repository;

import com.platform.module.portal.domain.Application;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, String> {
    boolean existsByApplicationCode(String applicationCode);
    Optional<Application> findByIdAndDeletedYn(String id, String deletedYn);
    Page<Application> findAllByDeletedYn(String deletedYn, Pageable pageable);
    Page<Application> findAllByPortalIdAndDeletedYn(String portalId, String deletedYn, Pageable pageable);

    /** 관리자 목록 화면은 삭제된 항목도 상태(deletedYn)와 함께 보여줘야 하므로 필터링하지 않는다. */
    Page<Application> findAllByPortalId(String portalId, Pageable pageable);
}
