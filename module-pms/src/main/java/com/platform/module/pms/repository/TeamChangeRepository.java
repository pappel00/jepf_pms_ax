package com.platform.module.pms.repository;

import com.platform.module.pms.domain.TeamChange;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamChangeRepository extends JpaRepository<TeamChange, String> {
    Optional<TeamChange> findByIdAndDeletedYn(String id, String deletedYn);
}
