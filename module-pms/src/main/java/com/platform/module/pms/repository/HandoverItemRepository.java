package com.platform.module.pms.repository;

import com.platform.module.pms.domain.HandoverItem;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HandoverItemRepository extends JpaRepository<HandoverItem, String> {
    Optional<HandoverItem> findByIdAndDeletedYn(String id, String deletedYn);
}
