package com.platform.module.pms.repository;

import com.platform.module.pms.domain.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    Optional<Customer> findByIdAndDeletedYn(String id, String deletedYn);

    Page<Customer> findAllByDeletedYn(String deletedYn, Pageable pageable);

    /** 코드 채번용 — 삭제된 행 포함 전체(삭제된 코드를 재사용하지 않기 위해). */
    List<Customer> findAll();
}