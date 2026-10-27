package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Rfp;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfpRepository extends JpaRepository<Rfp, String> {
    Optional<Rfp> findByIdAndDeletedYn(String id, String deletedYn);
}
