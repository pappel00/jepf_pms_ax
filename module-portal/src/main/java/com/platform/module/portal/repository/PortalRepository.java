package com.platform.module.portal.repository;

import com.platform.module.portal.domain.Portal;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortalRepository extends JpaRepository<Portal, String> {
    boolean existsByPortalCode(String portalCode);
    Optional<Portal> findByIdAndDeletedYn(String id, String deletedYn);
    Page<Portal> findAllByDeletedYn(String deletedYn, Pageable pageable);
}
