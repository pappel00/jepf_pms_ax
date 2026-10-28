package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProjectFeature;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectFeatureRepository extends JpaRepository<ProjectFeature, String> {
    Optional<ProjectFeature> findByIdAndDeletedYn(String id, String deletedYn);

    List<ProjectFeature> findAllByDeletedYnOrderByCreatedDateAsc(String deletedYn);

    List<ProjectFeature> findAllByProjectIdAndDeletedYn(String projectId, String deletedYn);
}
