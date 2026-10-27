package com.platform.module.pms.repository;

import com.platform.module.pms.domain.StatusHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, String> {
    Optional<StatusHistory> findByIdAndDeletedYn(String id, String deletedYn);

    List<StatusHistory> findAllByTargetTypeCodeAndTargetIdOrderByActedAtAsc(String targetTypeCode, String targetId);
}