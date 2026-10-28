package com.platform.module.pms.repository;

import com.platform.module.pms.domain.RfpEvaluator;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfpEvaluatorRepository extends JpaRepository<RfpEvaluator, String> {
    Optional<RfpEvaluator> findByIdAndDeletedYn(String id, String deletedYn);

    List<RfpEvaluator> findAllByRfpIdAndDeletedYnOrderByCreatedDateAsc(String rfpId, String deletedYn);

    List<RfpEvaluator> findAllByRfpIdInAndDeletedYnOrderByCreatedDateAsc(Collection<String> rfpIds, String deletedYn);

    Optional<RfpEvaluator> findByRfpIdAndEvaluatorUserIdAndDeletedYn(String rfpId, String evaluatorUserId, String deletedYn);
}
