package com.platform.module.pms.repository;

import com.platform.module.pms.domain.TaskProgressLog;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskProgressLogRepository extends JpaRepository<TaskProgressLog, String> {
    Optional<TaskProgressLog> findByIdAndDeletedYn(String id, String deletedYn);
}
