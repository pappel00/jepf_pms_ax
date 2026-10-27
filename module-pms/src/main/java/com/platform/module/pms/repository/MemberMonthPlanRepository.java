package com.platform.module.pms.repository;

import com.platform.module.pms.domain.MemberMonthPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberMonthPlanRepository extends JpaRepository<MemberMonthPlan, String> {
    Optional<MemberMonthPlan> findByIdAndDeletedYn(String id, String deletedYn);
}
