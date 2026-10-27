package com.platform.module.pms.repository;

import com.platform.module.pms.domain.CustomerContact;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerContactRepository extends JpaRepository<CustomerContact, String> {
    Optional<CustomerContact> findByIdAndDeletedYn(String id, String deletedYn);

    Page<CustomerContact> findAllByDeletedYn(String deletedYn, Pageable pageable);

    Page<CustomerContact> findAllByCustomerIdAndDeletedYn(String customerId, String deletedYn, Pageable pageable);

    List<CustomerContact> findAllByCustomerIdAndDeletedYn(String customerId, String deletedYn);

    List<CustomerContact> findAll();
}