package com.platform.module.pms.repository;

import com.platform.module.pms.domain.StdDoc;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StdDocRepository extends JpaRepository<StdDoc, String> {
    Optional<StdDoc> findByIdAndDeletedYn(String id, String deletedYn);
}
