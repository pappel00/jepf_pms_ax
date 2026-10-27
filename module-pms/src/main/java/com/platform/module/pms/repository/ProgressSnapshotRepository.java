package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProgressSnapshot;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressSnapshotRepository extends JpaRepository<ProgressSnapshot, String> {
    Optional<ProgressSnapshot> findByIdAndDeletedYn(String id, String deletedYn);
}
