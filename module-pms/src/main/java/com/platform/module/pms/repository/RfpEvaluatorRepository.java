package com.platform.module.pms.repository;

import com.platform.module.pms.domain.RfpEvaluator;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfpEvaluatorRepository extends JpaRepository<RfpEvaluator, String> {
    Optional<RfpEvaluator> findByIdAndDeletedYn(String id, String deletedYn);
}
