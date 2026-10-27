package com.platform.module.pms.repository;

import com.platform.module.pms.domain.DocTailoring;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocTailoringRepository extends JpaRepository<DocTailoring, String> {
    Optional<DocTailoring> findByIdAndDeletedYn(String id, String deletedYn);
}
