package com.platform.module.pms.repository;

import com.platform.module.pms.domain.ProjectFeature;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectFeatureRepository extends JpaRepository<ProjectFeature, String> {
    Optional<ProjectFeature> findByIdAndDeletedYn(String id, String deletedYn);
}
