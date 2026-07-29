package com.platform.module.portal.repository;

import com.platform.module.portal.domain.DataService;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataServiceRepository extends JpaRepository<DataService, String> {
    boolean existsByDataServiceCode(String dataServiceCode);
    Optional<DataService> findByIdAndDeletedYn(String id, String deletedYn);
    Page<DataService> findAllByDeletedYn(String deletedYn, Pageable pageable);
}
