package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Transition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransitionRepository extends JpaRepository<Transition, String> {
    Optional<Transition> findByIdAndDeletedYn(String id, String deletedYn);
}
