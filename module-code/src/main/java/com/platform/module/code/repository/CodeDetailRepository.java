package com.platform.module.code.repository;

import com.platform.module.code.domain.CodeDetail;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeDetailRepository extends JpaRepository<CodeDetail, String> {
    boolean existsByGroupIdAndCode(String groupId, String code);
    List<CodeDetail> findAllByGroupIdOrderBySortOrderAsc(String groupId);
    Optional<CodeDetail> findByIdAndGroupId(String id, String groupId);
}
