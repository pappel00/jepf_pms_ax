package com.platform.module.pms.repository;

import com.platform.module.pms.domain.DocVersion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocVersionRepository extends JpaRepository<DocVersion, String> {
    Optional<DocVersion> findByIdAndDeletedYn(String id, String deletedYn);

    List<DocVersion> findAllByTailoringIdInAndDeletedYnOrderByCreatedDateAsc(Collection<String> tailoringIds, String deletedYn);

    List<DocVersion> findAllByTailoringIdAndDeletedYnOrderByCreatedDateAsc(String tailoringId, String deletedYn);
}
