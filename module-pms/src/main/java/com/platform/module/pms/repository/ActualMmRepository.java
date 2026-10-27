package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ActualMm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActualMmRepository extends JpaRepository<ActualMm, String> {
    Optional<ActualMm> findByIdAndDeletedYn(String id, String deletedYn);
}
