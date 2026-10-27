package com.platform.module.pms.repository;

import com.platform.module.pms.domain.RfpItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfpItemRepository extends JpaRepository<RfpItem, String> {
    Optional<RfpItem> findByIdAndDeletedYn(String id, String deletedYn);
}
