package com.platform.module.portal.repository;

import com.platform.module.portal.domain.PageDataService;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageDataServiceRepository extends JpaRepository<PageDataService, String> {
    boolean existsByPageIdAndDataServiceId(String pageId, String dataServiceId);
    List<PageDataService> findAllByPageId(String pageId);
    Optional<PageDataService> findByIdAndPageId(String id, String pageId);
}
