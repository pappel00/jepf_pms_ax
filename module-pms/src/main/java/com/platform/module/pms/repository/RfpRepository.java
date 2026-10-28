package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Rfp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfpRepository extends JpaRepository<Rfp, String> {
    Optional<Rfp> findByIdAndDeletedYn(String id, String deletedYn);

    List<Rfp> findAllByDeletedYnOrderByRfpCodeAsc(String deletedYn);

    List<Rfp> findAllByProjectIdAndDeletedYnOrderByRfpCodeAsc(String projectId, String deletedYn);

    /** 코드 채번용 — 삭제된 행 포함 전체. */
    List<Rfp> findAll();
}
