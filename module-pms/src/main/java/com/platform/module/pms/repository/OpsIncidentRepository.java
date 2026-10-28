package com.platform.module.pms.repository;

import com.platform.module.pms.domain.OpsIncident;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsIncidentRepository extends JpaRepository<OpsIncident, String> {
    Optional<OpsIncident> findByIdAndDeletedYn(String id, String deletedYn);

    List<OpsIncident> findAllByDeletedYnOrderByReceivedAtDesc(String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<OpsIncident> findAll();
}
