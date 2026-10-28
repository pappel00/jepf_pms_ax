package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProjectOperation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectOperationRepository extends JpaRepository<ProjectOperation, String> {
    Optional<ProjectOperation> findByIdAndDeletedYn(String id, String deletedYn);

    Optional<ProjectOperation> findByProjectIdAndDeletedYn(String projectId, String deletedYn);

    List<ProjectOperation> findAllByDeletedYn(String deletedYn);
}
