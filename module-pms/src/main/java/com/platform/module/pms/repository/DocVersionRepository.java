package com.platform.module.pms.repository;

import com.platform.module.pms.domain.DocVersion;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocVersionRepository extends JpaRepository<DocVersion, String> {
    Optional<DocVersion> findByIdAndDeletedYn(String id, String deletedYn);
}
