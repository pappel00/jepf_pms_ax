package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ActualMm;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActualMmRepository extends JpaRepository<ActualMm, String> {
    Optional<ActualMm> findByIdAndDeletedYn(String id, String deletedYn);

    List<ActualMm> findAllByDeletedYnOrderByActualMonthAsc(String deletedYn);

    Optional<ActualMm> findByProjectIdAndKindCodeAndUserIdAndActualMonthAndDeletedYn(String projectId, String kindCode, String userId, String actualMonth, String deletedYn);
}
