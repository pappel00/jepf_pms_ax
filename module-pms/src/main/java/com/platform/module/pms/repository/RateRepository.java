package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Rate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateRepository extends JpaRepository<Rate, String> {
    Optional<Rate> findByIdAndDeletedYn(String id, String deletedYn);
}
