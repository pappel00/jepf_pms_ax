package com.platform.module.portal.repository;

import com.platform.module.portal.domain.Page;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PageRepository extends JpaRepository<Page, String> {
    boolean existsByPageCode(String pageCode);
    Optional<Page> findByIdAndDeletedYn(String id, String deletedYn);
    org.springframework.data.domain.Page<Page> findAllByDeletedYn(String deletedYn, Pageable pageable);

    /** 다른 모듈이 부트스트랩 시 특정 페이지가 이미 있는지 찾아볼 때 사용 (find-or-create). */
    Optional<Page> findByPageCodeAndDeletedYn(String pageCode, String deletedYn);
}
