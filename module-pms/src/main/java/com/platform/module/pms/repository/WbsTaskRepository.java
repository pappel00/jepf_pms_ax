package com.platform.module.pms.repository;

import com.platform.module.pms.domain.WbsTask;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WbsTaskRepository extends JpaRepository<WbsTask, String> {
    Optional<WbsTask> findByIdAndDeletedYn(String id, String deletedYn);
}
