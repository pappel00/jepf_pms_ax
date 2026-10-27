package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProjectOperation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectOperationRepository extends JpaRepository<ProjectOperation, String> {
    Optional<ProjectOperation> findByIdAndDeletedYn(String id, String deletedYn);
}
