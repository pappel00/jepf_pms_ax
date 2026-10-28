package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Report;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {
    Optional<Report> findByIdAndDeletedYn(String id, String deletedYn);

    List<Report> findAllByDeletedYnOrderByPeriodStartAsc(String deletedYn);

    Optional<Report> findByProjectIdAndReportTypeCodeAndPeriodStartAndDeletedYn(String projectId, String typeCode, LocalDate periodStart, String deletedYn);
}
