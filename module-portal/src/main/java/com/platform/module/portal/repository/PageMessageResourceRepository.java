package com.platform.module.portal.repository;

import com.platform.module.portal.domain.PageMessageResource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageMessageResourceRepository extends JpaRepository<PageMessageResource, String> {
    boolean existsByPageIdAndMessageResourceCode(String pageId, String messageResourceCode);
    List<PageMessageResource> findAllByPageId(String pageId);
    Optional<PageMessageResource> findByIdAndPageId(String id, String pageId);
}
