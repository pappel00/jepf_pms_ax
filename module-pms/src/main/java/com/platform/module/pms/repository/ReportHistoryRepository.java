package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ReportHistory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportHistoryRepository extends JpaRepository<ReportHistory, String> {
    Optional<ReportHistory> findByIdAndDeletedYn(String id, String deletedYn);

    List<ReportHistory> findAllByReportIdInAndDeletedYnOrderByActedAtAsc(Collection<String> reportIds, String deletedYn);

    boolean existsByReportId(String reportId);
}
