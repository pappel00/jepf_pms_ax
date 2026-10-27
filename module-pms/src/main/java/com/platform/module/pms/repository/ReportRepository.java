package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Report;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, String> {
    Optional<Report> findByIdAndDeletedYn(String id, String deletedYn);
}
