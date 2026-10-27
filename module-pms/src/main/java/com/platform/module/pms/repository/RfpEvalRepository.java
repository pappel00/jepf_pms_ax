package com.platform.module.pms.repository;

import com.platform.module.pms.domain.RfpEval;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfpEvalRepository extends JpaRepository<RfpEval, String> {
    Optional<RfpEval> findByIdAndDeletedYn(String id, String deletedYn);
}
