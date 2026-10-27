package com.platform.module.pms.repository;

import com.platform.module.pms.domain.OpsTask;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsTaskRepository extends JpaRepository<OpsTask, String> {
    Optional<OpsTask> findByIdAndDeletedYn(String id, String deletedYn);
}
