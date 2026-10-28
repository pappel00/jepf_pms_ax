package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ReportVersion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportVersionRepository extends JpaRepository<ReportVersion, String> {
    Optional<ReportVersion> findByIdAndDeletedYn(String id, String deletedYn);

    List<ReportVersion> findAllByReportIdInAndDeletedYnOrderByVersionNoAsc(Collection<String> reportIds, String deletedYn);
}
