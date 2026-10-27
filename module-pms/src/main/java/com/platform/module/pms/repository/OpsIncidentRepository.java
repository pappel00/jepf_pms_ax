package com.platform.module.pms.repository;

import com.platform.module.pms.domain.OpsIncident;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpsIncidentRepository extends JpaRepository<OpsIncident, String> {
    Optional<OpsIncident> findByIdAndDeletedYn(String id, String deletedYn);
}
