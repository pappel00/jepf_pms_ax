package com.platform.module.pms.repository;

import com.platform.module.pms.domain.DocTailoring;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocTailoringRepository extends JpaRepository<DocTailoring, String> {
    Optional<DocTailoring> findByIdAndDeletedYn(String id, String deletedYn);

    List<DocTailoring> findAllByDeletedYnOrderByProjectIdAscCreatedDateAsc(String deletedYn);

    List<DocTailoring> findAllByProjectIdAndDeletedYnOrderByCreatedDateAsc(String projectId, String deletedYn);

    boolean existsByProjectIdAndDeletedYn(String projectId, String deletedYn);
}
