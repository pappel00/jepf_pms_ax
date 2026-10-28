package com.platform.module.pms.repository;

import com.platform.module.pms.domain.RfpItem;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RfpItemRepository extends JpaRepository<RfpItem, String> {
    Optional<RfpItem> findByIdAndDeletedYn(String id, String deletedYn);

    List<RfpItem> findAllByRfpIdAndDeletedYnOrderBySortOrderAscCreatedDateAsc(String rfpId, String deletedYn);

    List<RfpItem> findAllByRfpIdInAndDeletedYnOrderBySortOrderAscCreatedDateAsc(Collection<String> rfpIds, String deletedYn);

    /** 항목 코드 채번용 — 삭제된 행 포함. */
    List<RfpItem> findAllByRfpId(String rfpId);
}
